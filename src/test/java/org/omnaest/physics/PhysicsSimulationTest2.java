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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;
import org.omnaest.physics.PhysicsSimulation.Runner;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.force.AntiCollisionForceProvider;
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

public class PhysicsSimulationTest2
{
    private static final Logger LOG = LoggerFactory.getLogger(PhysicsSimulationTest2.class);

    private Set<Particle> centerParticles = new HashSet<>();

    @Test
    @Ignore
    public void testTick() throws Exception
    {
        PhysicsSimulation simulation = PhysicsUtils.newSimulationInstance();

        int width = 2000;
        int heigth = 1500;

        //
        Runner runner = simulation.getRunner();

        DurationCapture durationCapture = new DurationCapture();
        AtomicInteger writeDuration = new AtomicInteger(20);
        runner.setTimeTickHandler((timeTick, iSimulation) ->
        {

            if (timeTick % 1000 == 0)
            {
                Particle particleIn1 = new Particle(2);
                Particle particleIn2 = new Particle(2);
                Particle particleOut1 = new Particle(2);
                Particle particleOut2 = new Particle(2);
                Particle center = new Particle(2);
                simulation.addParticle(particleIn1);
                simulation.addParticle(particleIn2);
                simulation.addParticle(particleOut1);
                simulation.addParticle(particleOut2);
                simulation.addParticle(center);

                simulation.addForceProvider(new PointForceProvider(center, 0, 0).setStrength(0.1));

                simulation.addForceProvider(new AntiCollisionForceProvider(particleIn1, 30));
                simulation.addForceProvider(new AntiCollisionForceProvider(particleIn2, 30));
                simulation.addForceProvider(new AntiCollisionForceProvider(particleOut1, 30));
                simulation.addForceProvider(new AntiCollisionForceProvider(particleOut2, 30));
                simulation.addForceProvider(new AntiCollisionForceProvider(center, 30));

                simulation.addForceProvider(new AntiCollisionForceProvider(center, 200)
                                                                                       .setExclusionParticles(particleIn1, particleIn2, particleOut1,
                                                                                                              particleOut2)
                                                                                       .setStrength(1000));

                simulation.addForceProvider(new DistanceForceProvider(center, particleIn1, 100));
                simulation.addForceProvider(new DistanceForceProvider(center, particleIn2, 100));
                simulation.addForceProvider(new DistanceForceProvider(center, particleOut1, 100));
                simulation.addForceProvider(new DistanceForceProvider(center, particleOut2, 100));

                simulation.addForceProvider(new DistanceForceProvider(particleIn1, particleOut1, 200));
                simulation.addForceProvider(new DistanceForceProvider(particleIn1, particleOut2, 200));
                simulation.addForceProvider(new DistanceForceProvider(particleIn2, particleOut1, 200));
                simulation.addForceProvider(new DistanceForceProvider(particleIn2, particleOut2, 200));
            }

            long duration = Math.max(1, writeDuration.get());
            if (timeTick % (duration * 10) == 0)
            {
                durationCapture.start();
                //render
                SVGDrawer drawer = SVGUtils.getDrawer(width, heigth)
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
                        Vector location = particle.getLocation()
                                                  .add(origin);
                        int x = (int) location.getX();
                        int y = (int) location.getY();
                        int r = isCenterParticle ? 200 : 10;
                        drawer.add(new SVGCircle(x, y, r).setFillColor("lightgreen"));

                        if (debug)
                        {
                            Vector force = simulation.calculateForceFor(particle);
                            Vector targetPoint = location.add(force.multiply(0.1));
                            drawer.add(new SVGLine((int) location.getX(), (int) location.getY(), (int) targetPoint.getX(),
                                                   (int) targetPoint.getY()).setStrokeColor("red"));
                        }
                    }

                });

                if (debug)
                {

                    simulation.getForceProviders()
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

                                      drawer.add(new SVGLine((int) location1.getX(), (int) location1.getY(), (int) location2.getX(),
                                                             (int) location2.getY()).setStrokeWidth(5)
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
                                  else if (forceProvider instanceof AntiCollisionForceProvider)
                                  {
                                      AntiCollisionForceProvider antiCollisionForceProvider = (AntiCollisionForceProvider) forceProvider;
                                      Vector location = antiCollisionForceProvider.getParticle()
                                                                                  .getLocation()
                                                                                  .add(origin);

                                      double collisionDistance = antiCollisionForceProvider.getCollisionDistance();

                                      int x = (int) location.getX();
                                      int y = (int) location.getY();
                                      int r = (int) collisionDistance;
                                      drawer.add(new SVGCircle(x, y, r).setStrokeColor("purple")
                                                                       .setFillOpacity(0.1));
                                  }
                              });
                }

                try
                {
                    drawer.renderAsResult()
                          .writeToFile(new File("C:/Temp/particles.svg"));
                }
                catch (Exception e)
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

}
