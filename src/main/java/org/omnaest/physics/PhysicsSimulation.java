/*

	Copyright 2017 Danny Kunz

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.


*/
package org.omnaest.physics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.omnaest.physics.domain.ForceProvider;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.Vector;
import org.omnaest.physics.force.utils.DurationCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see PhysicsUtils#newSimulationInstance()
 * @author omnaest
 */
public class PhysicsSimulation
{
	private static final Logger LOG = LoggerFactory.getLogger(PhysicsSimulation.class);

	private Set<Particle>		particles		= new LinkedHashSet<>();
	private List<ForceProvider>	forceProviders	= new ArrayList<>();

	public PhysicsSimulation addParticle(Particle particle)
	{
		this.particles.add(particle);
		return this;
	}

	public PhysicsSimulation addParticles(Collection<Particle> particles)
	{
		this.particles.addAll(particles);
		return this;
	}

	public PhysicsSimulation addForceProvider(ForceProvider forceProvider)
	{
		this.forceProviders.add(forceProvider);
		return this;
	}

	public PhysicsSimulation addForceProviders(Collection<? extends ForceProvider> forceProviders)
	{
		this.forceProviders.addAll(forceProviders);
		return this;
	}

	public List<Particle> getParticles()
	{
		return new ArrayList<>(this.particles);
	}

	public List<ForceProvider> getForceProviders()
	{
		return this.forceProviders;
	}

	public void tick()
	{
		this.tick(1.0);
	}

	public void tick(double deltaT)
	{
		this.particles	.stream()
						.parallel()
						.forEach(particle ->
						{
							Set<ForceProvider> matchedForceProviders = this.forceProviders	.stream()
																							.filter(forceProvider -> forceProvider.match(particle))
																							.collect(Collectors.toSet());

							this.applyForce(particle, matchedForceProviders, deltaT);
						});
	}

	private void applyForce(Particle particle, Set<ForceProvider> forceProviders, double deltaT)
	{

		//
		double passedTime = 0.0;
		int depth = 0;
		int maxDepth = 4;
		while (passedTime < deltaT * 0.9999 && depth < maxDepth)
		{
			//
			Vector force = forceProviders	.stream()
											.map(forceProvider -> forceProvider.getForce(particle))
											.reduce((f1, f2) -> f1.add(f2))
											.get();

			//identify timeScale
			double timeScale = deltaT - passedTime;
			boolean correctTimeFrame = false;
			do
			{
				double absoluteDistance = this	.calculateDistance(force, timeScale)
												.absolute();
				correctTimeFrame = absoluteDistance <= Math.max(1.0, 1.0 * deltaT);
				if (!correctTimeFrame)
				{
					timeScale /= 2.0;
				}
			} while (!correctTimeFrame && depth < maxDepth);

			//
			Vector distance = this.calculateDistance(force, timeScale);
			particle.move(distance);

			//
			passedTime += timeScale;
			depth++;
		}

	}

	private Vector calculateDistance(Vector force, double timeScale)
	{
		Vector speed = force.multiply(timeScale);
		Vector distance = speed.multiply(timeScale);
		return distance;
	}

	public static interface Runner
	{

		Runner stop();

		Runner run();

		Runner setTimeTickHandler(TimeTickHandler timeTickHandler);

		Runner awaitStop();

		/**
		 * Sets the detail precision of the simulation. Defaults to 1.0
		 *
		 * @param precision
		 * @return
		 */
		Runner setPrecision(double precision);

		double getFPS();

	}

	public static interface TimeTickHandler
	{
		public void handle(long timeTick, PhysicsSimulation simulation);
	}

	public Runner getRunner()
	{
		return new Runner()
		{
			private ExecutorService			executorService	= Executors.newCachedThreadPool();
			private TimeTickHandler			timeTickHandler	= null;
			private Lock					lock			= new ReentrantLock(true);
			private AtomicReference<Double>	fps				= new AtomicReference<>(0.0);
			private double					precision;

			@Override
			public Runner setTimeTickHandler(TimeTickHandler timeTickHandler)
			{
				this.timeTickHandler = timeTickHandler;
				return this;
			}

			@Override
			public Runner run()
			{
				this.executorService.submit(new Runnable()
				{
					private final double	PIXEL_PER_SECOND		= 10;
					private long			durationInMilliseconds	= 100;

					@Override
					public void run()
					{
						DurationCapture tickDurationCapture = new DurationCapture();
						lock.lock();
						try
						{
							tickDurationCapture.start();
							{
								double timeDuration = Math.max(0.1, this.durationInMilliseconds) * precision * this.PIXEL_PER_SECOND / 1000.0;
								PhysicsSimulation.this.tick(timeDuration);
							}
							this.durationInMilliseconds = tickDurationCapture.stop();
							fps.set((1000.0 / this.durationInMilliseconds));
						}
						finally
						{
							lock.unlock();
							executorService.submit(this);
						}
					}
				});

				if (this.timeTickHandler != null)
				{
					this.executorService.submit(new Runnable()
					{
						private AtomicLong timeTicker = new AtomicLong();

						@Override
						public void run()
						{
							//
							lock.lock();
							try
							{
								timeTickHandler.handle(this.timeTicker.getAndIncrement(), PhysicsSimulation.this);
							}
							catch (Exception e)
							{
								LOG.error("Error during time tick handler execution", e);
							}
							finally
							{
								lock.unlock();
							}

							//
							try
							{
								Thread.sleep(1);
								executorService.submit(this);
							}
							catch (InterruptedException e)
							{
							}
						}
					});
				}
				return this;
			}

			@Override
			public Runner stop()
			{
				try
				{
					this.executorService.shutdown();
					this.executorService.awaitTermination(2, TimeUnit.SECONDS);
				}
				catch (InterruptedException e)
				{
				}
				return this;
			}

			@Override
			public Runner awaitStop()
			{
				boolean notTerminated = true;
				while (notTerminated)
				{
					try
					{
						notTerminated = !this.executorService.awaitTermination(1, TimeUnit.SECONDS);
					}
					catch (InterruptedException e)
					{
					}
				}
				return this;
			}

			@Override
			public double getFPS()
			{
				return this.fps.get();
			}

			@Override
			public Runner setPrecision(double precision)
			{
				this.precision = precision;
				return this;
			}
		};
	}

	public Vector calculateForceFor(Particle particle)
	{
		return this.forceProviders	.stream()
									.filter(forceProvider -> forceProvider.match(particle))
									.map(forceProvider -> forceProvider.getForce(particle))
									.reduce((f1, f2) -> f1.add(f2))
									.orElse(Vector.NULL);
	}
}
