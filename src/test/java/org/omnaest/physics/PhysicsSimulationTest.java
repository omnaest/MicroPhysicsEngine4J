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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.omnaest.physics.PhysicsSimulation.Runner;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.force.DistanceForceProvider;
import org.omnaest.physics.domain.force.PointForceProvider;
import org.omnaest.physics.domain.force.utils.DurationCapture;
import org.omnaest.svg.SVGDrawer;
import org.omnaest.svg.SVGUtils;
import org.omnaest.svg.elements.SVGCircle;
import org.omnaest.svg.elements.SVGLine;
import org.omnaest.vector.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicsSimulationTest
{
	private static final Logger LOG = LoggerFactory.getLogger(PhysicsSimulationTest.class);

	private EnzymeReactionCompoundGroup	lastEnzymeReactionCompoundGroup	= null;
	private Particle					firstCenter						= null;
	private Set<Particle>				centerParticles					= new HashSet<>();

	public static class EnzymeReactionCompoundGroup
	{
		private List<Particle>				incomings				= new ArrayList<>();
		private List<Particle>				outgoings				= new ArrayList<>();
		private Particle					center;
		private List<DistanceForceProvider>	distanceForceProviders	= new ArrayList<>();

		public EnzymeReactionCompoundGroup()
		{
			super();
			this.createIncomings();
			this.createOutgoings();
			this.center = new Particle(2);
			this.createForces();
		}

		public EnzymeReactionCompoundGroup(EnzymeReactionCompoundGroup otherEnzymeReactionCompoundGroup)
		{
			this.incomings.addAll(otherEnzymeReactionCompoundGroup.getOutgoings());
			this.createOutgoings();
			this.center = new Particle(2);
			this.createForces();

			this.distanceForceProviders.add(new DistanceForceProvider(this.center, otherEnzymeReactionCompoundGroup.getCenter(), 600));
		}

		private void createForces()
		{
			this.distanceForceProviders.add(new DistanceForceProvider(this.incomings.get(0), this.incomings.get(1), 100));
			this.distanceForceProviders.add(new DistanceForceProvider(this.outgoings.get(0), this.outgoings.get(1), 100));

			this.distanceForceProviders.add(new DistanceForceProvider(this.incomings.get(0), this.center, 100));
			this.distanceForceProviders.add(new DistanceForceProvider(this.incomings.get(1), this.center, 100));

			this.distanceForceProviders.add(new DistanceForceProvider(this.outgoings.get(0), this.center, 100));
			this.distanceForceProviders.add(new DistanceForceProvider(this.outgoings.get(1), this.center, 100));

			this.distanceForceProviders.add(new DistanceForceProvider(this.incomings.get(0), this.outgoings.get(0), 400));
			this.distanceForceProviders.add(new DistanceForceProvider(this.incomings.get(1), this.outgoings.get(1), 400));
		}

		private void createOutgoings()
		{
			this.outgoings.add(new Particle(2));
			this.outgoings.add(new Particle(2));
		}

		private void createIncomings()
		{
			this.incomings.add(new Particle(2));
			this.incomings.add(new Particle(2));
		}

		public List<DistanceForceProvider> getDistanceForceProviders()
		{
			return this.distanceForceProviders;
		}

		public List<Particle> getIncomings()
		{
			return this.incomings;
		}

		public List<Particle> getOutgoings()
		{
			return this.outgoings;
		}

		public Particle getCenter()
		{
			return this.center;
		}

	}

	@Test
	public void testTick() throws Exception
	{
		PhysicsSimulation simulation = PhysicsUtils.newSimulationInstance();

		this.createEnzymeGroup(simulation);

		int width = 20000;
		int heigth = 15000;
		PointForceProvider leftPointForceProvider = new PointForceProvider(this.firstCenter, 0, -heigth / 2 + 100);
		simulation.addForceProvider(leftPointForceProvider);
		//	simulation.addForceProvider(new PointForceProvider(lastCenter, width / 2 - 100, 0));

		//
		Runner runner = simulation.getRunner();

		DurationCapture durationCapture = new DurationCapture();
		AtomicInteger writeDuration = new AtomicInteger(20);
		runner.setTimeTickHandler((timeTick, iSimulation) ->
		{

			if (timeTick % 200 == 0)
			{
				this.createEnzymeGroup(simulation);
			}

			Vector leftPointLocation = leftPointForceProvider.getLocation();
			leftPointForceProvider.setLocation(leftPointLocation.normVector()
																.multiply(leftPointLocation.absolute() * 1.000)
																.rotateZ(0.1));

			long duration = Math.max(1, writeDuration.get());
			if (timeTick % (duration * 10) == 0)
			{
				durationCapture.start();
				//render
				SVGDrawer drawer = SVGUtils	.getDrawer(width, heigth)
											.setEmbedReloadTimer(500, TimeUnit.MILLISECONDS);
				List<Particle> particles = simulation.getParticles();

				Vector origin = new Vector(width / 2, heigth / 2);

				//
				boolean debug = true;

				particles.forEach(particle ->
				{
					boolean isCenterParticle = this.centerParticles.contains(particle);
					if (isCenterParticle || debug)
					{
						Vector location = particle	.getLocation()
													.add(origin);
						int x = (int) location.getX();
						int y = (int) location.getY();
						int r = isCenterParticle ? 200 : 10;
						drawer.add(new SVGCircle(x, y, r).setFillColor("lightgreen"));

						if (debug)
						{
							Vector force = simulation.calculateForceFor(particle);
							Vector targetPoint = location.add(force.multiply(0.1));
							drawer.add(new SVGLine(	(int) location.getX(), (int) location.getY(), (int) targetPoint.getX(),
													(int) targetPoint.getY()).setStrokeColor("red"));
						}
					}

				});

				if (debug)
				{

					simulation	.getForceProviders()
								.forEach(forceProvider ->
								{
									if (forceProvider instanceof DistanceForceProvider)
									{
										DistanceForceProvider distanceForceProvider = (DistanceForceProvider) forceProvider;

										Vector location1 = distanceForceProvider.getParticle1()
																				.getLocation()
																				.add(origin);
										Vector location2 = distanceForceProvider.getParticle2()
																				.getLocation()
																				.add(origin);

										drawer.add(new SVGLine(	(int) location1.getX(), (int) location1.getY(), (int) location2.getX(),
																(int) location2.getY())	.setStrokeWidth(5)
																						.setStrokeColor("yellow"));
									}
									else if (forceProvider instanceof PointForceProvider)
									{
										PointForceProvider pointForceProvider = (PointForceProvider) forceProvider;
										Vector location = pointForceProvider.getLocation()
																			.add(origin);

										int x = (int) location.getX();
										int y = (int) location.getY();
										int r = 20;
										drawer.add(new SVGCircle(x, y, r).setFillColor("yellow"));
									}
								});
				}

				try
				{
					drawer	.renderAsResult()
							.writeToFile(new File("C:/Temp/particles.svg"));
				} catch (Exception e)
				{
					LOG.error("", e);
				}
				writeDuration.set((int) durationCapture.stop());
				System.out.println(writeDuration.get() + "ms");

				System.out.println("fps:" + runner.getFPS());
			}
		});

		runner.run();
		runner.awaitStop();
	}

	private void createEnzymeGroup(PhysicsSimulation simulation)
	{
		EnzymeReactionCompoundGroup enzymeReactionCompoundGroup;
		if (this.lastEnzymeReactionCompoundGroup == null)
		{
			enzymeReactionCompoundGroup = new EnzymeReactionCompoundGroup();
		}
		else
		{
			enzymeReactionCompoundGroup = new EnzymeReactionCompoundGroup(this.lastEnzymeReactionCompoundGroup);
		}

		simulation.addParticle(enzymeReactionCompoundGroup.getCenter());
		simulation.addParticles(enzymeReactionCompoundGroup.getIncomings());
		simulation.addParticles(enzymeReactionCompoundGroup.getOutgoings());
		simulation.addForceProviders(enzymeReactionCompoundGroup.getDistanceForceProviders());

		if (this.firstCenter == null)
		{
			this.firstCenter = enzymeReactionCompoundGroup.getCenter();
		}

		this.centerParticles.add(enzymeReactionCompoundGroup.getCenter());

		this.lastEnzymeReactionCompoundGroup = enzymeReactionCompoundGroup;

	}

}
