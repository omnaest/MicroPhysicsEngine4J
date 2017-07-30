package org.omnaest.physics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.omnaest.physics.domain.ForceProvider;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicsSimulation
{
	private static final Logger LOG = LoggerFactory.getLogger(PhysicsSimulation.class);

	private List<Particle>		particles		= new ArrayList<>();
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
		return particles;
	}

	public List<ForceProvider> getForceProviders()
	{
		return forceProviders;
	}

	public void tick()
	{
		this.forceProviders	.stream()
							.forEach(forceProvider ->
							{
								List<Particle> matchedParticles = this.particles.stream()
																				.filter(particle -> forceProvider.match(particle))
																				.collect(Collectors.toList());

								matchedParticles.stream()
												.forEach(particle ->
												{
													this.applyForce(particle, forceProvider);
												});
							});
	}

	private void applyForce(Particle particle, ForceProvider forceProvider)
	{

		//
		double passedTime = 0.0;
		while (passedTime < 1.0)
		{
			//
			Vector force = forceProvider.getForce(particle);

			//identify timeScale
			double timeScale = 1.0 - passedTime;
			boolean correctTimeFrame = false;
			do
			{
				double absoluteDistance = this	.calculateDistance(force, timeScale)
												.absolute();
				correctTimeFrame = absoluteDistance <= 1.0;
				if (!correctTimeFrame)
				{
					timeScale /= 2.0;
				}
			} while (!correctTimeFrame);

			//
			Vector distance = calculateDistance(force, timeScale);
			particle.move(distance);

			//
			passedTime += timeScale;
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

		int getFPS();

	}

	public static interface TimeTickHandler
	{
		public void handle(long timeTick, PhysicsSimulation simulation);
	}

	public Runner getRunner()
	{
		return new Runner()
		{
			private ExecutorService	executorService	= Executors.newCachedThreadPool();
			private TimeTickHandler	timeTickHandler	= null;
			private Lock			lock			= new ReentrantLock();
			private AtomicInteger	fps				= new AtomicInteger();

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
					@Override
					public void run()
					{
						DurationCapture tickDurationCapture = new DurationCapture();
						lock.lock();
						try
						{
							tickDurationCapture.start();
							{
								tick();
							}
							fps.set((int) (1000 / tickDurationCapture.stop()));
						} finally
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
								timeTickHandler.handle(timeTicker.getAndIncrement(), PhysicsSimulation.this);
							} catch (Exception e)
							{
								LOG.error("Error during time tick handler execution", e);
							} finally
							{
								lock.unlock();
							}

							//
							try
							{
								Thread.sleep(1);
								executorService.submit(this);
							} catch (InterruptedException e)
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
				} catch (InterruptedException e)
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
					} catch (InterruptedException e)
					{
					}
				}
				return this;
			}

			@Override
			public int getFPS()
			{
				return this.fps.get();
			}
		};
	}

	public Vector calculateForceFor(Particle particle)
	{
		return this.forceProviders	.stream()
									.filter(forceProvider -> forceProvider.match(particle))
									.map(forceProvider -> forceProvider.getForce(particle))
									.reduce((f1, f2) -> f1.add(f2))
									.get();
	}
}
