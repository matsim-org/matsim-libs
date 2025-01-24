package org.matsim.contrib.ev.strategic.access;

import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

import com.google.common.collect.Sets;

/**
 * This implementation checks the subscriptions of each person and the required
 * subscriptions to access the chargers. If a person has the required
 * subscription, the charger can be used.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AttributeBasedChargerAccess implements ChargerAccess {
	private final SubscriptionRegistry subscriptions;

	public AttributeBasedChargerAccess(SubscriptionRegistry subscriptions) {
		this.subscriptions = subscriptions;
	}

	@Override
	public boolean hasAccess(Person person, ChargerSpecification charger) {
		Set<String> chargerSubscriptions = subscriptions.getChargerSubscriptions(charger);

		if (chargerSubscriptions.size() == 0) {
			return true;
		}

		Set<String> personSubscriptions = subscriptions.getPersonSubscriptions(person);
		return Sets.union(personSubscriptions, chargerSubscriptions).size() > 0;
	}

	@Override
	public boolean hasAccess(Person person, Charger charger) {
		return hasAccess(person, charger.getSpecification());
	}
}
