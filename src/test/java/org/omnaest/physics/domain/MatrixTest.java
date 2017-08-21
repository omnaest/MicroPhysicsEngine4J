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

public class MatrixTest
{

	@Test
	public void testMultiply() throws Exception
	{
		Matrix matrixA = new Matrix(new double[] { 3, 2, 1 }, new double[] { 1, 0, 2 });
		Vector vector = new Vector(1, 0, 4);

		Vector result = matrixA.multiply(vector);

		//		System.out.println(matrixA);
		//		System.out.println();
		//		System.out.println(vector);
		//		System.out.println();
		//		System.out.println(result);

		assertEquals(7.0, result.getX(), 0.001);
		assertEquals(9.0, result.getY(), 0.001);
	}

	@Test
	public void testGetSubMatrix() throws Exception
	{
		Matrix matrixA = new Matrix(new double[] { 3, 2, 1 }, new double[] { 1, 0, 2 }, new double[] { 4, 4, 4 });

		Matrix subMatrix = matrixA.getSubMatrix(2, 3);
		assertEquals(2, subMatrix.getDimensions()[0]);
		assertEquals(3, subMatrix.getDimensions()[1]);
		assertEquals(3, subMatrix.get(0, 0), 0.001);
		assertEquals(0, subMatrix.get(1, 1), 0.001);
		assertEquals(4, subMatrix.get(1, 2), 0.001);
	}

}
