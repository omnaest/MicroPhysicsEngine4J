package org.omnaest.physics.domain;

public interface ForceProvider
{

	public boolean match(Particle particle);

	public Vector getForce(Particle particle);

}
