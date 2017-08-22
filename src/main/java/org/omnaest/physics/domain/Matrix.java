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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @see Vector
 * @see #builder()
 * @author Omnaest
 */
public class Matrix
{
	protected static final Matrix NULL = new Matrix(new double[0][0]);

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

	public Matrix getSubMatrix(int x1, int y1, int x2, int y2)
	{
		double[][] data2 = new double[y2 - y1 + 1][x2 - x1 + 1];
		for (int y = y1; y <= y2; y++)
		{
			for (int x = x1; x <= x2; x++)
			{
				data2[y - y1][x - x1] = this.get(x, y);
			}
		}
		return new Matrix(data2);
	}

	protected Matrix getSubMatrixModulo(int x1, int y1, int x2, int y2)
	{
		int[] dimensions = this.getDimensions();
		double[][] data2 = new double[y2 - y1 + 1][x2 - x1 + 1];
		for (int y = y1; y <= y2; y++)
		{
			for (int x = x1; x <= x2; x++)
			{
				int xMod = x % dimensions[0];
				int yMod = y % dimensions[1];
				data2[y - y1][x - x1] = this.get(xMod, yMod);
			}
		}
		return new Matrix(data2);
	}

	public double determinant()
	{
		return this.determinantOf(this);
	}

	protected double determinantOf(Matrix matrix)
	{
		//
		double retval = 0.0;

		//
		int dimension = matrix.getDimensions()[0];
		if (dimension != matrix.getDimensions()[1])
		{
			throw new IllegalStateException("Matrix must be square");
		}
		if (dimension > 2)
		{
			for (int ii = 0; ii < dimension; ii++)
			{
				double factor = matrix.get(0, ii);
				double determinant = matrix	.getSubMatrixModulo(1, ii + 1, dimension - 1, dimension - 1 + ii)
											.determinant();
				retval += factor * determinant;
			}
		}
		else if (dimension == 2)
		{
			retval = matrix.get(0, 0) * matrix.get(1, 1) - matrix.get(1, 0) * matrix.get(0, 1);
		}
		else
		{
			throw new IllegalArgumentException("Matrix dimension has to be at least 2");
		}

		//
		return retval;
	}

	protected Matrix invert()
	{
		int[] dimensions = this.getDimensions();

		double[][] data = new double[dimensions[0]][dimensions[1]];
		for (int x = 0; x < dimensions[0]; x++)
		{
			for (int y = 0; y < dimensions[1]; y++)
			{
				data[x][y] = this.data[y][x];
			}
		}

		return new Matrix(data);
	}

	public static interface Builder extends RowBuilder, ColumnBuilder
	{
	}

	public static interface ColumnBuilder
	{
		ColumnBuilder addColumn(Vector vector);

		ColumnBuilder addColumn(double[] values);

		Matrix build();
	}

	public static interface RowBuilder
	{
		RowBuilder addRow(Vector vector);

		RowBuilder addRow(double[] values);

		Matrix build();
	}

	public static Builder builder()
	{
		return new Builder()
		{
			private List<double[]>	rows	= new ArrayList<>();
			private List<double[]>	columns	= new ArrayList<>();

			@Override
			public Matrix build()
			{
				Matrix retval = Matrix.NULL;

				if (!this.columns.isEmpty())
				{
					retval = new Matrix(this.columns.toArray(new double[0][0])).invert();
				}
				else if (!this.rows.isEmpty())
				{
					retval = new Matrix(this.rows.toArray(new double[0][0]));
				}

				return retval;
			}

			@Override
			public RowBuilder addRow(double[] values)
			{
				this.rows.add(values);
				return this;
			}

			@Override
			public ColumnBuilder addColumn(double[] values)
			{
				this.columns.add(values);
				return this;
			}

			@Override
			public RowBuilder addRow(Vector vector)
			{
				return this.addRow(vector.getCoordinates());
			}

			@Override
			public ColumnBuilder addColumn(Vector vector)
			{
				return this.addColumn(vector.getCoordinates());
			}
		};
	}

}
