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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.omnaest.physics.component.CallOptimizingForceProviderManager;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.force.ForceProvider;
import org.omnaest.physics.domain.force.ForceProvider.Type;
import org.omnaest.physics.domain.force.utils.DurationCapture;
import org.omnaest.utils.ThreadUtils;
import org.omnaest.vector.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see PhysicsUtils#newSimulationInstance()
 * @author omnaest
 */
public class PhysicsSimulation
{
    private static final Logger LOG = LoggerFactory.getLogger(PhysicsSimulation.class);

    private double cpuUseFactor = 1.0;

    private Set<Particle>      particles      = new LinkedHashSet<>();
    private Set<ForceProvider> forceProviders = new LinkedHashSet<>();

    private CallOptimizingForceProviderManager optimizingForceProviderManager = new CallOptimizingForceProviderManager();

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

    public PhysicsSimulation removeParticles(Set<Particle> particles)
    {
        this.particles.removeAll(particles);
        return this;
    }

    public PhysicsSimulation addForceProvider(ForceProvider forceProvider)
    {
        this.forceProviders.add(forceProvider);
        return this;
    }

    public PhysicsSimulation addForceProviders(Collection<? extends ForceProvider> forceProviders)
    {
        if (forceProviders != null)
        {
            forceProviders.forEach(this::addForceProvider);
        }
        return this;
    }

    public PhysicsSimulation removeForceProviders(ForceProvider... forceProviders)
    {
        return this.removeForceProviders(Arrays.asList(forceProviders));
    }

    public PhysicsSimulation removeForceProviders(Collection<ForceProvider> forceProviders)
    {
        this.forceProviders.removeAll(forceProviders);
        return this;
    }

    public List<Particle> getParticles()
    {
        return new ArrayList<>(this.particles);
    }

    public List<ForceProvider> getForceProviders()
    {
        return this.forceProviders.stream()
                                  .collect(Collectors.toList());
    }

    public void tick()
    {
        this.tick(1.0, null);
    }

    public void tick(double deltaT, ForceProvider.Type forceProviderType)
    {
        Set<ForceProvider> optimizedForceProviders = this.forceProviders.stream()
                                                                        .filter(forceProvider -> forceProviderType == null || forceProvider.getType()
                                                                                                                                           .equals(forceProviderType))
                                                                        //                                                                        .map(forceProvider -> this.optimizingForceProviderManager.wrap(forceProvider))
                                                                        .collect(Collectors.toSet());

        this.particles.stream()
                      .forEach(particle -> this.applyForce(particle, deltaT, optimizedForceProviders));
    }

    private void applyForce(Particle particle, double deltaT, Set<ForceProvider> forceProviders)
    {
        //
        Map<Type, List<ForceProvider>> matchingForceProviders = this.optimizingForceProviderManager.calculateMatchingForceProviders(forceProviders, particle);

        //
        double deltaT1 = deltaT / 2;
        double deltaT10 = deltaT1 / 10;

        this.applySingleDeltaT(particle, deltaT1, matchingForceProviders);
        for (int ii = 0; ii < 10; ii++)
        {
            this.applySingleDeltaT(particle, deltaT10, matchingForceProviders);
        }
    }

    private void applySingleDeltaT(Particle particle, double deltaT, Map<Type, List<ForceProvider>> matchingForceProviders)
    {
        double passedTime = 0.0;
        int depth = 0;
        int maxDepth = 4;
        while (passedTime < deltaT * 0.9999 && depth < maxDepth)
        {
            //
            Vector force = this.optimizingForceProviderManager.calculateForce(matchingForceProviders, particle);

            //identify timeScale
            double timeScale = deltaT - passedTime;
            boolean correctTimeFrame = false;
            do
            {
                double absoluteDistance = this.calculateDistance(force, timeScale)
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

        Runner boost(int duration, TimeUnit timeUnit);

        double getFPS();

        Runner suspend();

    }

    public static interface TimeTickHandler
    {
        public void handle(long timeTick, PhysicsSimulation simulation);
    }

    private static class LongRunningTicker
    {
        private ExecutorService  executorService = Executors.newSingleThreadExecutor();
        private DoubleAdder      timeDuration    = new DoubleAdder();
        private Lock             lock;
        private Consumer<Double> tickEventConsumer;

        public LongRunningTicker(ExecutorService executorService, Lock lock, Consumer<Double> tickEventConsumer)
        {
            super();
            this.executorService = executorService;
            this.lock = lock;
            this.tickEventConsumer = tickEventConsumer;
        }

        public void tickAsync(double duration)
        {
            double factor = 8;
            double root = duration > 1.0 ? 1.0 / factor : factor;
            this.timeDuration.add(Math.pow(duration, root));
        }

        public void start(int numberOfThreads)
        {
            for (int ii = 0; ii < numberOfThreads; ii++)
            {
                this.start();
            }
        }

        public void start()
        {
            this.executorService.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    //
                    DurationCapture tickDurationCapture = new DurationCapture();
                    LongRunningTicker.this.lock.lock();
                    try
                    {
                        tickDurationCapture.start();
                        {
                            double localTimeDuration = LongRunningTicker.this.timeDuration.sumThenReset();
                            LongRunningTicker.this.tickEventConsumer.accept(localTimeDuration);
                        }

                    }
                    finally
                    {
                        LongRunningTicker.this.lock.unlock();
                        LongRunningTicker.this.executorService.submit(this);
                    }
                }
            });
        }

        public void stop()
        {
            this.executorService.shutdown();
            try
            {
                this.executorService.awaitTermination(2, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                //do nothing
            }
        }
    }

    public Runner getRunner()
    {
        return new Runner()
        {
            private ExecutorService executorService = this.newExecutorService();

            private TimeTickHandler         timeTickHandler = null;
            private ReadWriteLock           lock            = new ReentrantReadWriteLock(true);
            private AtomicReference<Double> fps             = new AtomicReference<>(0.0);
            private double                  precision;
            private double                  precisionBoost  = 1.0;
            private boolean                 suspended       = false;

            private LongRunningTicker longRunningTicker = new LongRunningTicker(this.executorService, this.lock.readLock(),
                                                                                timeDuration -> PhysicsSimulation.this.tick(timeDuration,
                                                                                                                            ForceProvider.Type.ALL_MATCHING));

            private ExecutorService newExecutorService()
            {
                int threads = this.calculateProcessorNumberToUse();
                ExecutorService threadPool = Executors.newFixedThreadPool(threads);
                return threadPool;
            }

            private int calculateProcessorNumberToUse()
            {
                return (int) Math.round(Runtime.getRuntime()
                                               .availableProcessors()
                        * PhysicsSimulation.this.cpuUseFactor);
            }

            @Override
            public Runner setTimeTickHandler(TimeTickHandler timeTickHandler)
            {
                this.timeTickHandler = timeTickHandler;
                return this;
            }

            @Override
            public Runner run()
            {
                int numberOfThreads = this.calculateProcessorNumberToUse();
                int numberOfFastCalculationThreads = 1 + numberOfThreads * 2 / 3;
                int numberOfSlowCalculationThreads = 1 + numberOfThreads / 3;

                for (int ii = 0; ii < numberOfFastCalculationThreads; ii++)
                {
                    this.executorService.submit(new Runnable()
                    {
                        private final double PIXEL_PER_SECOND       = 10;
                        private long         durationInMilliseconds = 100;

                        @Override
                        public void run()
                        {
                            DurationCapture tickDurationCapture = new DurationCapture();
                            Lock readLock = lock.readLock();
                            readLock.lock();
                            try
                            {
                                tickDurationCapture.start();
                                {
                                    if (PhysicsSimulation.this.particles.isEmpty() || suspended)
                                    {
                                        ThreadUtils.sleepSilently(10, TimeUnit.MILLISECONDS);
                                    }
                                    else
                                    {
                                        double timeDuration = Math.max(0.1, this.durationInMilliseconds) * precision * precisionBoost * this.PIXEL_PER_SECOND
                                                / 1000.0;
                                        PhysicsSimulation.this.tick(timeDuration, ForceProvider.Type.SPECIFIC);
                                        longRunningTicker.tickAsync(timeDuration);
                                    }
                                }
                                this.durationInMilliseconds = tickDurationCapture.stop();
                                fps.set((1000.0 / this.durationInMilliseconds));
                            }
                            finally
                            {
                                readLock.unlock();
                                executorService.submit(this);
                            }
                        }
                    });
                }

                //
                this.longRunningTicker.start(numberOfSlowCalculationThreads);

                //
                if (this.timeTickHandler != null)
                {
                    this.executorService.submit(new Runnable()
                    {
                        private AtomicLong timeTicker = new AtomicLong();

                        @Override
                        public void run()
                        {
                            //
                            Lock writeLock = lock.writeLock();
                            writeLock.lock();
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
                                writeLock.unlock();
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
            public Runner suspend()
            {
                this.suspended = !this.suspended;
                return this;
            }

            @Override
            public Runner stop()
            {
                try
                {
                    this.executorService.shutdown();
                    this.executorService.awaitTermination(2, TimeUnit.SECONDS);
                    this.longRunningTicker.stop();
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

            private long boostEndTime = 0;

            @Override
            public Runner boost(int duration, TimeUnit timeUnit)
            {
                this.boostEndTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
                this.executorService.submit(() ->
                {
                    LOG.info("Boost for " + timeUnit.toMillis(duration) + "ms");
                    this.precisionBoost = 100.0;

                    ThreadUtils.sleepSilently(duration, timeUnit);

                    while (System.currentTimeMillis() < this.boostEndTime)
                    {
                        ThreadUtils.sleepSilently(duration / 10, timeUnit);
                    }
                    this.precisionBoost = 1.0;
                    LOG.info("Boost ended");
                });
                return this;
            }

        };
    }

    public Vector calculateForceFor(Particle particle)
    {
        return this.forceProviders.stream()
                                  .filter(forceProvider -> forceProvider.match(particle))
                                  .map(forceProvider -> forceProvider.getForce(particle))
                                  .reduce((f1, f2) -> f1.add(f2))
                                  .orElse(Vector.NULL);
    }

    public void reset()
    {
        this.particles.clear();
        this.forceProviders.clear();
    }

    public PhysicsSimulation setCPUUseFactor(double cpuUseFactor)
    {
        this.cpuUseFactor = cpuUseFactor;
        return this;

    }
}
