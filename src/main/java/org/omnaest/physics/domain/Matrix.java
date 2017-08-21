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

import java.util.Locale;

public class Matrix
{
	private double[][] data;

	public Matrix(double[]... data)
	{
		super();
		this.data = data;
	}

	public Matrix(Vector vector)
	{
		super();
		double[] coordinates = vector.getCoordinates();
		this.data = new double[coordinates.length][1];

		int ii = 0;
		for (double value : coordinates)
		{
			this.data[ii++] = new double[] { value };
		}
	}

	public Vector multiply(Vector vector)
	{
		Matrix matrixB = new Matrix(vector);
		Matrix result = this.multiply(matrixB);
		int dimensionY = result.getDimensions()[1];
		double[] coordinates = new double[dimensionY];
		for (int ii = 0; ii < dimensionY; ii++)
		{
			coordinates[ii] = result.get(0, ii);
		}
		return new Vector(coordinates);
	}

	public Matrix multiply(Matrix matrixB)
	{
		int[] dimensionsA = this.getDimensions();
		int[] dimensionsB = matrixB.getDimensions();

		if (dimensionsA[0] != dimensionsB[1])
		{
			throw new IllegalArgumentException("x dimension of A must be equal to y dimension of B");
		}

		int freeDimension = dimensionsA[0];

		double[][] data2 = new double[dimensionsA[1]][dimensionsB[0]];

		for (int y = 0; y < data2.length; y++)
		{
			for (int x = 0; x < data2[y].length; x++)
			{
				double sum = 0;

				for (int ii = 0; ii < freeDimension; ii++)
				{
					double a = this.get(ii, y);
					double b = matrixB.get(x, ii);
					sum += a * b;
				}
				data2[y][x] = sum;
			}
		}

		return new Matrix(data2);
	}

	public double get(int x, int y)
	{
		return this.data[y][x];
	}

	public int[] getDimensions()
	{
		return new int[] { this.data[0].length, this.data.length };
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		int[] dimensions = this.getDimensions();
		for (int y = 0; y < dimensions[1]; y++)
		{
			for (int x = 0; x < dimensions[0]; x++)
			{
				double value = this.get(x, y);
				sb.append(String.format(Locale.ENGLISH, "% 6.2f", value) + " ");
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	public Matrix getSubMatrix(int dimensionX, int dimensionY)
	{
		double[][] data2 = new double[dimensionY][dimensionX];
		for (int y = 0; y < data2.length; y++)
		{
			for (int x = 0; x < data2[y].length; x++)
			{
				data2[y][x] = this.get(x, y);
			}
		}
		return new Matrix(data2);
	}

}
