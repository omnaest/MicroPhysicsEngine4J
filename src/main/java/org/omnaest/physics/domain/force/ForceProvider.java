package org.omnaest.physics.domain.force;

import org.omnaest.physics.domain.Particle;
import org.omnaest.vector.Vector;

public interface ForceProvider
{

	public boolean match(Particle particle);

	public Vector getForce(Particle particle);

}
