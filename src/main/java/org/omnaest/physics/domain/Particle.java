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

import java.util.concurrent.atomic.AtomicReference;

import org.omnaest.vector.Vector;

public class Particle
{
	private AtomicReference<Vector> location = new AtomicReference<>();

	public Particle()
	{
		super();
		this.location.set(new Vector(Math.random(), Math.random()));
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

}
