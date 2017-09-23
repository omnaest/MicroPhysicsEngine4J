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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.omnaest.physics.domain.Particle;
import org.omnaest.vector.Vector;

/**
 * A {@link ForceProvider} which forces multiple {@link Particle}s to be on one line
 * 
 * @author omnaest
 */
public class LineForceProvider implements ForceProvider
{
	protected Collection<Particle> particles;

	protected Supplier<Double>	distance	= () -> 0.0;
	protected Supplier<Double>	strength	= () -> 0.1;

	public LineForceProvider(Particle... particles)
	{
		this(Arrays.asList(particles));
	}

	public LineForceProvider(Collection<Particle> particles)
	{
		super();
		this.particles = particles	.stream()
									.collect(Collectors.toList());
	}

	public LineForceProvider setDistance(double distance)
	{
		return this.setDistance(() -> distance);
	}

	public LineForceProvider setDistance(Supplier<Double> distance)
	{
		this.distance = distance;
		return this;
	}

	public LineForceProvider setStrength(double strength)
	{
		return this.setStrength(() -> strength);
	}

	public LineForceProvider setStrength(Supplier<Double> strength)
	{
		this.strength = strength;
		return this;
	}

	@Override
	public boolean match(Particle particle)
	{
		return this.particles	.stream()
								.anyMatch(p -> ObjectUtils.equals(p, particle));
	}

	@Override
	public Vector getForce(Particle particle)
	{
		Vector delta = Vector.NULL;

		long countOfOtherParticles = this.particles	.stream()
													.filter(p -> ObjectUtils.notEqual(p, particle))
													.count();
		if (countOfOtherParticles >= 2)
		{
			Vector location = particle.getLocation();

			Vector center = this.particles	.stream()
											.filter(p -> ObjectUtils.notEqual(p, particle))
											.map(p -> p.getLocation())
											.reduce((l1, l2) -> l1.add(l2))
											.get()
											.divide(countOfOtherParticles);

			List<Vector> closestPoints = this.particles	.stream()
														.filter(p -> ObjectUtils.notEqual(p, particle))
														.map(p -> p.getLocation())
														.collect(Collectors.toList());

			Collections.sort(closestPoints, (v1, v2) -> -1 * Double.compare(location.subtract(v1)
																					.absolute(),
																			location.subtract(v2)
																					.absolute()));

			while (closestPoints.size() > 2)
			{
				closestPoints.remove(0);
			}

			if (closestPoints.size() == 2)
			{
				Iterator<Vector> iterator = closestPoints.iterator();

				Vector closestPoint1 = iterator.next();
				Vector closestPoint2 = iterator.next();

				Vector lineDirection = closestPoint2.subtract(closestPoint1);

				delta = location.closestDirectionToLine(center, lineDirection);
			}
		}

		//
		final double distance = this.distance.get();

		if (delta.absolute() <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random(), Math.random());
		}

		double absoluteDistanceDelta = delta.absolute() - distance;
		double multiplier = this.strength.get();

		Vector force = delta.normVector()
							.multiply(absoluteDistanceDelta)
							.multiply(multiplier);
		return force;
	}

	@Override
	public String toString()
	{
		return "LineForceProvider [particles=" + this.particles + ", distance=" + this.distance + ", strength=" + this.strength + "]";
	}

}