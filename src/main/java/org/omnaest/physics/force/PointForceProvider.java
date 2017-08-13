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

public class PointForceProvider implements ForceProvider
{
	private Particle	particle;
	private Vector		location;
	protected double	strength	= 0.9;

	public PointForceProvider(double x, double y)
	{
		this(null, x, y);
	}

	public PointForceProvider(Particle particle, double x, double y)
	{
		super();
		this.particle = particle;
		this.location = new Vector(x, y);
	}

	public PointForceProvider setStrength(double strength)
	{
		this.strength = strength;
		return this;
	}

	public Particle getParticle()
	{
		return this.particle;
	}

	public Vector getLocation()
	{
		return this.location;
	}

	public PointForceProvider setLocation(Vector location)
	{
		this.location = location;
		return this;
	}

	@Override
	public boolean match(Particle particle)
	{
		return this.particle == null || this.particle.equals(particle);
	}

	@Override
	public Vector getForce(Particle particle)
	{
		Vector delta = particle	.getLocation()
								.subtract(this.location);
		if (delta.absolute() <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random());
		}
		delta = delta.multiply(-1.0);
		Vector force = delta.normVector()
							.multiply(delta.absolute() * delta.absolute())
							.multiply(this.strength);
		return force;
	}
}