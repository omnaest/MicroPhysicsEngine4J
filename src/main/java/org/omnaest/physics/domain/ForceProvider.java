package org.omnaest.physics.domain;

import org.omnaest.vector.Vector;

public interface ForceProvider
{

	public boolean match(Particle particle);

	public Vector getForce(Particle particle);

}
