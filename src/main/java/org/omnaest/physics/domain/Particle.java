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
package org.omnaest.physics.domain;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.ArrayUtils;
import org.omnaest.vector.Vector;

public class Particle
{
	private AtomicReference<Vector> location = new AtomicReference<>();

	public Particle(int dimensions)
	{
		this(new Vector(ArrayUtils.toPrimitive(IntStream.range(0, dimensions)
														.mapToObj(value -> Math.random())
														.collect(Collectors.toList())
														.toArray(new Double[0]))));

	}

	protected Particle(Vector location)
	{
		super();
		this.location.set(location);
	}

	public Vector getLocation()
	{
		return this.location.get();
	}

	public Particle setLocation(Vector location)
	{
		this.location.set(location);
		return this;
	}

	public Particle move(Vector distance)
	{
		this.location.set(this.location	.get()
										.add(distance));
		return this;
	}

	public static Particle newAverageParticle(Collection<Particle> particles)
	{
		Vector location = particles	.stream()
									.map(particle -> particle.getLocation())
									.reduce((l1, l2) -> l1	.add(l2)
															.divide(2))
									.orElse(Vector.NULL);
		return new Particle(location);
	}
}
