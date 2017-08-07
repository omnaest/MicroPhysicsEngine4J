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
package org.omnaest.physics.force;

import org.omnaest.physics.domain.ForceProvider;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.Vector;

public class DistanceForceProvider implements ForceProvider
{
	private Particle	particle1;
	private Particle	particle2;
	private double		distance;
	protected double	strength	= 1000.0;

	public DistanceForceProvider(Particle particle1, Particle particle2, double distance)
	{
		super();
		this.particle1 = particle1;
		this.particle2 = particle2;
		this.distance = distance;
	}

	public DistanceForceProvider setStrength(double strength)
	{
		this.strength = strength;
		return this;
	}

	public Particle getParticle1()
	{
		return this.particle1;
	}

	public Particle getParticle2()
	{
		return this.particle2;
	}

	@Override
	public boolean match(Particle particle)
	{
		return particle.equals(this.particle1) || particle.equals(this.particle2);
	}

	@Override
	public Vector getForce(Particle particle)
	{
		Vector delta = this.particle1	.getLocation()
										.subtract(this.particle2.getLocation());
		if (delta.absolute() <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random());
		}
		delta = delta.multiply(particle == this.particle1 ? -1.0 : 1.0);
		double absoluteDistanceDelta = this.distance - delta.absolute();
		double multiplier = this.strength;
		Vector force = delta.normVector()
							.multiply(absoluteDistanceDelta)
							.multiply(multiplier) //50
							.multiply(-1);
		return force;
	}
}