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

import org.omnaest.physics.domain.Particle;
import org.omnaest.vector.Vector;

public class MinimalPointDistanceForceProvider implements ForceProvider
{
	private ForceProvider forceProvider;

	public MinimalPointDistanceForceProvider(Particle particle, double x, double y, double minimumDistance)
	{
		super();
		this.forceProvider = new MinimalDistanceForceProvider(particle, new Particle().setLocation(new Vector(x, y)), minimumDistance);
	}

	@Override
	public boolean match(Particle particle)
	{
		return this.forceProvider.match(particle);
	}

	@Override
	public Vector getForce(Particle particle)
	{
		return this.forceProvider.getForce(particle);
	}

}
