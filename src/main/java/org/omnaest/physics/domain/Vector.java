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

import java.util.Arrays;

public class Vector
{
	public static final Vector NULL = new Vector(0, 0, 0);

	private double[] coordinates;

	public Vector(double x, double y)
	{
		super();
		this.coordinates = new double[] { x, y };
	}

	public Vector(double x, double y, double z)
	{
		super();
		this.coordinates = new double[] { x, y, z };
	}

	public Vector(double x)
	{
		super();
		this.coordinates = new double[] { x };
	}

	public Vector(double... coordinates)
	{
		super();
		this.coordinates = coordinates;
	}

	public double getX()
	{
		return this.coordinates[0];
	}

	public double getY()
	{
		return this.coordinates[1];
	}

	public double getZ()
	{
		return this.coordinates[2];
	}

	public double[] getCoordinates()
	{
		return this.coordinates;
	}

	public Vector subtract(Vector vector)
	{
		return this.add(vector.multiply(-1));
	}

	public Vector add(Vector vector)
	{
		double[] coordinates = vector.getCoordinates();
		double[] addedCoordinates = new double[coordinates.length];
		for (int ii = 0; ii < this.determineCommonDimension(coordinates); ii++)
		{
			addedCoordinates[ii] = this.coordinates[ii] + coordinates[ii];
		}
		return new Vector(addedCoordinates);
	}

	private int determineCommonDimension(double[] coordinates)
	{
		return Math.min(coordinates.length, this.coordinates.length);
	}

	public double multiplyScalar(Vector vector)
	{
		double[] coordinates = vector.getCoordinates();
		double retval = 0.0;
		for (int ii = 0; ii < this.determineCommonDimension(coordinates); ii++)
		{
			retval += this.coordinates[ii] * coordinates[ii];
		}
		return retval;
	}

	public double absolute()
	{
		return Math.sqrt(this.multiplyScalar(this));
	}

	public Vector normVector()
	{
		double scalarValue = this.absolute();
		double range = 0.000000001;
		return scalarValue > range || scalarValue < -range ? this.divide(scalarValue) : new Vector(0, 0);
	}

	public Vector multiply(double multiplier)
	{
		double[] multiplyedCoordinates = new double[this.coordinates.length];
		for (int ii = 0; ii < this.coordinates.length; ii++)
		{
			multiplyedCoordinates[ii] = this.coordinates[ii] * multiplier;
		}
		return new Vector(multiplyedCoordinates);
	}

	public Vector divide(double divider)
	{
		return this.multiply(1.0 / divider);
	}

	/**
	 * Rotates in degree around the z-axis
	 *
	 * @param angle
	 * @return
	 */
	public Vector rotateZ(double angle)
	{
		return this.rotate(0, 0, angle);
	}

	public Vector rotateY(double angle)
	{
		return this.rotate(0, angle, 0);
	}

	public Vector rotateX(double angle)
	{
		return this.rotate(angle, 0, 0);
	}

	public Vector rotate(double angleX, double angleY, double angleZ)
	{
		int dimension = this.getDimension();
		return this	.getRotationMatrixXY(angleZ)
					.multiply(this.getRotationMatrixXZ(angleY))
					.multiply(this.getRotationMatrixYZ(angleX))
					.getSubMatrix(dimension, dimension)
					.multiply(this);
	}

	private int getDimension()
	{
		return this.coordinates.length;
	}

	private Matrix getRotationMatrixXY(double angleZ)
	{
		double cos = Math.cos(angleZ / 180 * Math.PI);
		double sin = Math.sin(angleZ / 180 * Math.PI);
		return new Matrix(new double[][] { new double[] { cos, -sin, 0 }, new double[] { sin, cos, 0 }, new double[] { 0, 0, 1 } });
	}

	private Matrix getRotationMatrixYZ(double angleX)
	{
		double cos = Math.cos(angleX / 180 * Math.PI);
		double sin = Math.sin(angleX / 180 * Math.PI);
		return new Matrix(new double[][] { new double[] { 1, 0, 0 }, new double[] { 0, cos, sin }, new double[] { 0, -sin, cos } });
	}

	private Matrix getRotationMatrixXZ(double angleY)
	{
		double cos = Math.cos(angleY / 180 * Math.PI);
		double sin = Math.sin(angleY / 180 * Math.PI);
		return new Matrix(new double[][] { new double[] { cos, 0, -sin }, new double[] { 0, 1, 0 }, new double[] { sin, 0, cos } });
	}

	@Override
	public String toString()
	{
		return "[coordinates=" + Arrays.toString(this.coordinates) + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.coordinates);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Vector other = (Vector) obj;
		if (!Arrays.equals(this.coordinates, other.coordinates))
			return false;
		return true;
	}

}
