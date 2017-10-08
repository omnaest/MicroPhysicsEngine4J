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

/**
 * @see #setMass(double)
 * @see #setStrength(double)
 * @author omnaest
 */
public class PointForceProvider implements ForceProvider
{
	protected Particle			particle;
	protected Vector			location;
	protected Supplier<Double>	strength	= () -> 0.9;
	protected Supplier<Double>	mass		= () -> 1.0;
	private Type				type		= Type.SPECIFIC;

	public PointForceProvider(double... coordinates)
	{
		this(null, coordinates);
		this.type = Type.ALL_MATCHING;
	}

	public PointForceProvider(Particle particle, Vector location)
	{
		this.particle = particle;
		this.location = location;
	}

	public PointForceProvider(Particle particle, double... coordinates)
	{
		this(particle, new Vector(coordinates));
	}

	@Override
	public Type getType()
	{
		return this.type;
	}

	public PointForceProvider setStrength(Supplier<Double> strength)
	{
		this.strength = strength;
		return this;
	}

	public PointForceProvider setStrength(double strength)
	{
		this.strength = () -> strength;
		return this;
	}

	public PointForceProvider setMass(Supplier<Double> mass)
	{
		this.mass = mass;
		return this;
	}

	public PointForceProvider setMass(double mass)
	{
		this.mass = () -> mass;
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
							.multiply(this.strength.get() * this.mass.get());
		return force;
	}

	@Override
	public String toString()
	{
		return "PointForceProvider [particle=" + this.particle + ", location=" + this.location + ", strength=" + this.strength + "]";
	}

}