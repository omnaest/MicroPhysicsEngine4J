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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.omnaest.physics.PhysicsSimulation.Runner;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.force.DistanceForceProvider;
import org.omnaest.physics.domain.force.MinimalDistanceForceProvider;
import org.omnaest.physics.domain.force.MinimalPointDistanceForceProvider;
import org.omnaest.physics.domain.force.PointForceProvider;
import org.omnaest.physics.domain.force.utils.DurationCapture;
import org.omnaest.svg.SVGDrawer;
import org.omnaest.svg.SVGUtils;
import org.omnaest.svg.elements.SVGCircle;
import org.omnaest.svg.elements.SVGLine;
import org.omnaest.vector.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DominoSimulationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(DominoSimulationTest.class);

    private List<Domino> dominos = new ArrayList<>();

    private Map<Particle, Set<Domino>> particles = new HashMap<>();

    protected static class Domino
    {
        private Particle    particleTop;
        private Particle    particleBottom;
        private Set<Domino> otherDominos;

        public Domino(Particle particle, Particle particle2)
        {
            super();
            this.particleBottom = particle;
            this.particleTop = particle2;
        }

        public Set<Particle> getParticles()
        {
            Set<Particle> retset = new HashSet<>();
            retset.add(this.particleBottom);
            retset.add(this.particleTop);
            return retset;
        }

        public Domino attachToSimulation(PhysicsSimulation simulation)
        {
            simulation.addParticle(this.particleTop);
            simulation.addParticle(this.particleBottom);
            int distance = 100;
            simulation.addForceProvider(new DistanceForceProvider(this.particleTop, this.particleBottom, distance));

            //
            this.addForceToFilterParticle(simulation, distance, this.particleTop);
            this.addForceToFilterParticle(simulation, distance, this.particleBottom);

            //
            return this;
        }

        private void addForceToFilterParticle(PhysicsSimulation simulation, int distance, Particle filterParticle)
        {
            this.otherDominos.stream()
                             .filter(domino -> domino.getParticles()
                                                     .contains(filterParticle))
                             .flatMap(domino -> domino.getParticles()
                                                      .stream())
                             .filter(particle -> !filterParticle.equals(particle))
                             .forEach(particle ->
                             {
                                 simulation.addForceProvider(new MinimalDistanceForceProvider(filterParticle, particle, distance * 2));
                             });
        }

        public void render(SVGDrawer drawer)
        {
            int x1 = (int) this.particleTop.getLocation()
                                           .getX();
            int y1 = (int) this.particleTop.getLocation()
                                           .getY();
            drawer.add(new SVGCircle(x1, y1, 20));

            int x2 = (int) this.particleBottom.getLocation()
                                              .getX();
            int y2 = (int) this.particleBottom.getLocation()
                                              .getY();
            drawer.add(new SVGCircle(x2, y2, 20));

            drawer.add(new SVGLine(x1, y1, x2, y2));
        }

        public Domino addDominos(Stream<Domino> otherDominos)
        {
            this.otherDominos = otherDominos.collect(Collectors.toSet());
            return this;
        }
    }

    @Before
    public void setUp()
    {
        this.initParticles();
    }

    @Test
    @Ignore
    public void testTick() throws Exception
    {
        PhysicsSimulation simulation = PhysicsUtils.newSimulationInstance();

        this.createDomino(simulation);

        int width = 4000;
        int heigth = 3000;

        double angle = 360 / this.particles.size();
        this.particles.keySet()
                      .forEach(new Consumer<Particle>()
                      {
                          private Vector rotationVector = new Vector(1500, 0);

                          @Override
                          public void accept(Particle particle)
                          {
                              this.rotationVector = this.rotationVector.rotateZ(angle);
                              simulation.addForceProvider(new PointForceProvider(particle, this.rotationVector.getX(),
                                                                                 this.rotationVector.getY()).setStrength(0.01));
                          }
                      });

        this.particles.keySet()
                      .forEach(particle ->
                      {
                          simulation.addForceProvider(new MinimalPointDistanceForceProvider(particle, 500, 0, 0));
                      });

        //
        Runner runner = simulation.getRunner();

        DurationCapture durationCapture = new DurationCapture();
        AtomicInteger writeDuration = new AtomicInteger(20);
        runner.setTimeTickHandler((timeTick, iSimulation) ->
        {

            if (timeTick % 1000 == 0)
            {
                System.out.println("Create domino");
                this.createDomino(simulation);
            }

            if (timeTick % 100 == 0)
            {
                durationCapture.start();
                //render
                SVGDrawer drawer = SVGUtils.getDrawer(-width / 2, -heigth / 2, width, heigth)
                                           .setEmbedReloadTimer(500, TimeUnit.MILLISECONDS);

                this.dominos.stream()
                            .forEach(domino -> domino.render(drawer));

                //
                try
                {
                    drawer.renderAsResult()
                          .writeToFile(new File("C:/Temp/dominos.svg"));
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

    private void createDomino(PhysicsSimulation simulation)
    {
        Particle particle1 = this.findRandomParticle();
        Particle particle2 = this.findRandomParticle();
        Set<Domino> dominosToParticle1 = this.particles.get(particle1);
        Set<Domino> dominosToParticle2 = this.particles.get(particle2);
        Domino domino = new Domino(particle1, particle2).addDominos(Stream.concat(dominosToParticle1.stream(), dominosToParticle2.stream()))
                                                        .attachToSimulation(simulation);
        this.dominos.add(domino);
    }

    private Particle findRandomParticle()
    {
        List<Particle> particles = new ArrayList<>(this.particles.keySet());
        Collections.shuffle(particles);
        return particles.get(0);
    }

    private void initParticles()
    {
        for (int ii = 0; ii < 100; ii++)
        {
            this.particles.put(new Particle(2), new HashSet<>());
        }
    }

}
