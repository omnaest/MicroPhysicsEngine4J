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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.omnaest.physics.domain.ForceProvider;
import org.omnaest.physics.domain.Particle;
import org.omnaest.vector.Vector;

public class AntiCollisionForceProvider implements ForceProvider
{
	private Particle		particle;
	protected double		strength			= 100;
	private int				collisionDistance;
	private Set<Particle>	exclusionParticles	= Collections.emptySet();

	public AntiCollisionForceProvider(Particle particle, int collisionDistance)
	{
		super();
		this.particle = particle;
		this.collisionDistance = collisionDistance;

	}

	public int getCollisionDistance()
	{
		return this.collisionDistance;
	}

	public AntiCollisionForceProvider setStrength(double strength)
	{
		this.strength = strength;
		return this;
	}

	public Particle getParticle()
	{
		return this.particle;
	}

	@Override
	public boolean match(Particle particle)
	{
		return !this.particle.equals(particle) && !this.exclusionParticles.contains(particle);
	}

	@Override
	public Vector getForce(Particle particle)
	{
		Vector delta = particle	.getLocation()
								.subtract(this.particle.getLocation());
		double distance = delta.absolute();
		if (distance <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random()).divide(this.strength);
		}

		if (distance > this.collisionDistance)
		{
			delta = delta.multiply(0);
		}

		double effectiveDistance = this.collisionDistance - distance;

		Vector force = delta.normVector()
							.multiply(effectiveDistance * effectiveDistance)
							.multiply(this.strength);
		return force;
	}

	public AntiCollisionForceProvider setExclusionParticles(Particle... exclusionParticles)
	{

		this.exclusionParticles = Arrays.asList(exclusionParticles)
										.stream()
										.collect(Collectors.toSet());
		return this;
	}
}