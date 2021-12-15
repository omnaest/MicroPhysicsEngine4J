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

import org.junit.Ignore;
import org.junit.Test;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.force.AntiCollisionForceProvider;
import org.omnaest.physics.domain.force.DistanceForceProvider;
import org.omnaest.physics.domain.force.ForceProvider;

public class PhysicsSimulationTest3
{

    @Test
    @Ignore
    public void testTick() throws Exception
    {
        PhysicsSimulation simulation = PhysicsUtils.newSimulationInstance();

        Particle lastParticle = null;
        for (int ii = 0; ii < 100; ii++)
        {
            Particle particle = new Particle(3);
            ForceProvider antiCollisionForceProvider = new AntiCollisionForceProvider(particle, 100);

            simulation.addParticle(particle);
            simulation.addForceProvider(antiCollisionForceProvider);

            if (lastParticle != null)
            {
                ForceProvider distanceForce = new DistanceForceProvider(particle, lastParticle, 200);
                simulation.addForceProvider(distanceForce);
            }
            lastParticle = particle;
        }

        for (int ii = 0; ii < 10000; ii++)
        {
            simulation.tick();
        }

    }

}
