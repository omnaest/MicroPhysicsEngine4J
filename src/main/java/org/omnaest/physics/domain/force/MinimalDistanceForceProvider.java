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

import java.util.function.Supplier;

import org.omnaest.physics.domain.Particle;
import org.omnaest.vector.Vector;

public class MinimalDistanceForceProvider implements ForceProvider
{
	private Particle			particle1;
	private Particle			particle2;
	private Supplier<Double>	distanceSupplier;
	protected double			strength	= 100.0;

	public MinimalDistanceForceProvider(Particle particle1, Particle particle2, double distance)
	{
		this(particle1, particle2, () -> distance);
	}

	public MinimalDistanceForceProvider(Particle particle1, Particle particle2, Supplier<Double> distanceSupplier)
	{
		super();
		this.particle1 = particle1;
		this.particle2 = particle2;
		this.distanceSupplier = distanceSupplier;
	}

	public MinimalDistanceForceProvider setStrength(double strength)
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
		final double distance = this.distanceSupplier.get();

		if (delta.absolute() <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random());
		}
		delta = delta.multiply(particle == this.particle1 ? -1.0 : 1.0);
		double absoluteDistanceDelta = distance - delta.absolute();
		double multiplier = this.strength;

		if (delta.absolute() > distance)
		{
			multiplier = 0.0;
		}

		Vector force = delta.normVector()
							.multiply(absoluteDistanceDelta)
							.multiply(multiplier)
							.multiply(-1);
		return force;
	}
}