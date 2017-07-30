package org.omnaest.physics.force;

import org.omnaest.physics.domain.ForceProvider;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.Vector;

public class PointForceProvider implements ForceProvider
{
	private Particle	particle;
	private Vector		location;

	public PointForceProvider(Particle particle, double x, double y)
	{
		super();
		this.particle = particle;
		this.location = new Vector(x, y);
	}

	public Particle getParticle()
	{
		return this.particle;
	}

	public Vector getLocation()
	{
		return location;
	}

	public PointForceProvider setLocation(Vector location)
	{
		this.location = location;
		return this;
	}

	@Override
	public boolean match(Particle particle)
	{
		return this.particle.equals(particle);
	}

	@Override
	public Vector getForce(Particle particle)
	{
		Vector delta = particle	.getLocation()
								.subtract(this.location);
		if (delta.absolute() <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random());
		}
		delta = delta.multiply(-1.0);
		Vector force = delta.normVector()
							.multiply(delta.absolute() * delta.absolute())
							.multiply(0.9);
		return force;
	}
}