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
package org.omnaest.physics.domain.force;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.omnaest.physics.domain.Particle;
import org.omnaest.vector.Vector;

/**
 * @see LineForceProvider
 * @author omnaest
 */
public class LineForceProviderTest
{

	@Test
	public void testGetForce() throws Exception
	{
		Particle particle1 = new Particle(3).setLocation(new Vector(-100, 0, 0));
		Particle particle2 = new Particle(3).setLocation(new Vector(100, 0, 0));
		Particle particle3 = new Particle(3).setLocation(new Vector(0, 100, 0));
		Collection<Particle> particles = Arrays.asList(particle1, particle2, particle3);
		LineForceProvider lineForceProvider = new LineForceProvider(particles);

		for (int ii = 0; ii < 1000; ii++)
		{
			Vector force = lineForceProvider.getForce(particle3);
			particle3.move(force);
			//System.out.println(particle3.getLocation());
		}

		assertTrue(particle3.getLocation()
							.distanceTo(new Vector(0, 0, 0)) < 5.0);
	}

}
