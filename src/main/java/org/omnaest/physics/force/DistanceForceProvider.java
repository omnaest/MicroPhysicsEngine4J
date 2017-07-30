package org.omnaest.physics.force;

import org.omnaest.physics.domain.ForceProvider;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.Vector;

public class DistanceForceProvider implements ForceProvider
{
	private Particle	particle1;
	private Particle	particle2;
	private double		distance;

	public DistanceForceProvider(Particle particle1, Particle particle2, double distance)
	{
		super();
		this.particle1 = particle1;
		this.particle2 = particle2;
		this.distance = distance;
	}

	public Particle getParticle1()
	{
		return particle1;
	}

	public Particle getParticle2()
	{
		return particle2;
	}

	@Override
	public boolean match(Particle particle)
	{
		return particle.equals(particle1) || particle.equals(particle2);
	}

	@Override
	public Vector getForce(Particle particle)
	{
		Vector delta = particle1.getLocation()
								.subtract(particle2.getLocation());
		if (delta.absolute() <= 0.001)
		{
			delta = new Vector(Math.random(), Math.random());
		}
		delta = delta.multiply(particle == particle1 ? -1.0 : 1.0);
		double absoluteDistanceDelta = distance - delta.absolute();
		double multiplier = 100;// Math.pow(0.0001, 2);
		Vector force = delta.normVector()
							.multiply(absoluteDistanceDelta)
							.multiply(multiplier) //50
							.multiply(-1);
		return force;
	}
}