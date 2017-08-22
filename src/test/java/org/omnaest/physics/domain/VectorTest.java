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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VectorTest
{
	@Test
	public void testRotate() throws Exception
	{
		assertEquals(	1.0, new Vector(1, 0, 0).rotateZ(90)
												.getY(),
						0.0001);
		assertEquals(	1.0, new Vector(1, 0, 0).rotateY(90)
												.getZ(),
						0.0001);
		assertEquals(	1.0, new Vector(0, 0, 1).rotateX(90)
												.getY(),
						0.0001);
	}

	@Test
	public void testMultiplyCross() throws Exception
	{
		Vector cross = new Vector(1, 2, 3).multiplyCross(new Vector(-7, 8, 9));
		assertEquals(-6.0, cross.getX(), 0.001);
		assertEquals(-30.0, cross.getY(), 0.001);
		assertEquals(22.0, cross.getZ(), 0.001);
	}

}
