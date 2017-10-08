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
package org.omnaest.physics.component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.omnaest.physics.domain.Particle;
import org.omnaest.physics.domain.force.ForceProvider;
import org.omnaest.physics.domain.force.ForceProvider.Type;
import org.omnaest.vector.Vector;

public class CallOptimizingForceProviderManager
{
	private Map<Particle, Map<Type, AtomicInteger>>	particleToTypeToCallCounter	= new ConcurrentHashMap<>();
	private Map<Particle, Map<Type, Vector>>		particleToTypeToLastVector	= new ConcurrentHashMap<>();

	public CallOptimizingForceProviderManager()
	{
		super();
	}

	private Map<Type, AtomicInteger> newTypeToCounterMap()
	{
		EnumMap<Type, AtomicInteger> retmap = new EnumMap<>(Type.class);
		retmap.put(Type.SPECIFIC, new AtomicInteger());
		retmap.put(Type.ALL_MATCHING, new AtomicInteger());
		return retmap;
	}

	public ForceProvider wrap(ForceProvider forceProvider)
	{
		return new ForceProvider()
		{
			@Override
			public boolean match(Particle particle)
			{
				return forceProvider.match(particle);
			}

			@Override
			public Vector getForce(Particle particle)
			{
				Vector force = forceProvider.getForce(particle);

				CallOptimizingForceProviderManager.this.particleToTypeToCallCounter	.computeIfAbsent(	particle,
																										p -> CallOptimizingForceProviderManager.this.newTypeToCounterMap())
																					.get(this.getType())
																					.incrementAndGet();

				return force;
			}

			@Override
			public Type getType()
			{
				return forceProvider.getType();
			}

			@Override
			public boolean equals(Object obj)
			{
				return forceProvider.equals(obj) || ObjectUtils	.identityToString(this)
																.equals(ObjectUtils.identityToString(obj));
			}

			@Override
			public int hashCode()
			{
				return forceProvider.hashCode();
			}

		};
	}

	public Map<Type, List<ForceProvider>> calculateMatchingForceProviders(Set<ForceProvider> forceProviders, Particle particle)
	{
		Map<Type, Boolean> typeToAvoidMap = this.calculateTypeToAvoidMap(particle);
		return forceProviders	.stream()
								.filter(forceProvider -> !typeToAvoidMap.get(forceProvider.getType()))
								.filter(forceProvider -> forceProvider.match(particle))
								.collect(Collectors.groupingBy(forceProvider -> forceProvider.getType()));
	}

	private Map<Type, Boolean> calculateTypeToAvoidMap(Particle particle)
	{
		return Arrays	.asList(Type.values())
						.stream()
						.collect(Collectors.toMap(type -> type, type -> this.avoid(type, particle)));
	}

	private boolean avoid(Type type, Particle particle)
	{
		Map<Type, AtomicInteger> typeToCallCounter = this.particleToTypeToCallCounter.computeIfAbsent(particle, p -> this.newTypeToCounterMap());
		int callCountForType = typeToCallCounter.get(type)
												.get();
		int callCountForInverseType = typeToCallCounter	.get(type.inverse())
														.get();
		boolean hasBothTypes = callCountForType > 0 && callCountForInverseType > 0;
		return hasBothTypes && callCountForType > callCountForInverseType * 10;
	}

	public Vector calculateForce(Map<Type, List<ForceProvider>> forceProviders, Particle particle)
	{
		return Arrays	.asList(Type.values())
						.stream()
						.map(type ->
						{
							Vector force = Vector.NULL;

							List<ForceProvider> matchingForceProviders = forceProviders.get(type);
							if (matchingForceProviders == null || matchingForceProviders.isEmpty())
							{
								force = this.particleToTypeToLastVector	.computeIfAbsent(particle, p -> new ConcurrentHashMap<>())
																		.getOrDefault(type, Vector.NULL);
							}
							else
							{
								force = matchingForceProviders	.stream()
																.map(forceProvider -> forceProvider.getForce(particle))
																.reduce((f1, f2) -> f1.add(f2))
																.orElse(Vector.NULL);

								this.particleToTypeToLastVector	.computeIfAbsent(particle, p -> new ConcurrentHashMap<>())
																.put(type, force);
							}
							return force;
						})
						.reduce((f1, f2) -> f1.add(f2))
						.get();
	}

}
