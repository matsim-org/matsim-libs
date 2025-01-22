package org.matsim.contrib.ev.strategic.infrastructure;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * An implementation of the ChargerProvider that delegates to several other
 * providers.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class CompositeChargerProvider implements ChargerProvider {
	private final Collection<ChargerProvider> delegates;

	public CompositeChargerProvider(Collection<ChargerProvider> delegates) {
		this.delegates = delegates;
	}

	@Override
	public Collection<ChargerSpecification> findChargers(Person person, Plan plan, ChargerRequest request) {
		List<ChargerSpecification> chargers = new LinkedList<>();

		for (ChargerProvider delegate : delegates) {
			chargers.addAll(delegate.findChargers(person, plan, request));
		}

		return chargers;
	}
}
