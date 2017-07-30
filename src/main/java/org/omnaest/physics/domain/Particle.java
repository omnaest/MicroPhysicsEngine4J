package org.omnaest.physics.domain;

public class Particle
{
	private Vector location;

	public Particle()
	{
		super();
		this.location = new Vector(Math.random(), Math.random());
	}

	public Vector getLocation()
	{
		return location;
	}

	public void setLocation(Vector location)
	{
		this.location = location;
	}

	public void move(Vector distance)
	{
		this.location = this.location.add(distance);
	}

}
