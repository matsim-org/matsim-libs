package org.matsim.contrib.ev.strategic.infrastructure;

import java.util.Collection;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * The charger provider interface contains the functionality to identify viable
 * chargers for a potential charging process.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public interface ChargerProvider {
	/**
	 * Return relevant chargers that can be used for the given charging request and
	 * the given person.
	 */
	Collection<ChargerSpecification> findChargers(Person person, Plan plan, ChargerRequest request);

	/**
	 * This class represents a request that is made to a ChargingProvider. A
	 * charging request can either be for a leg-based charging slot (along a ride)
	 * or an activity-based charging slot (when an agent chargers during a sequence
	 * of activities).
	 */
	public record ChargerRequest(Activity startActivity, Activity endActivity, Leg leg, double duration) {
		public boolean isLegBased() {
			return leg != null;
		}

		public ChargerRequest(Activity startActivity, Activity endActivity) {
			this(startActivity, endActivity, null, 0.0);
		}

		public ChargerRequest(Leg leg, double duration) {
			this(null, null, leg, duration);
		}

		public ChargerRequest(Leg leg) {
			this(null, null, leg, Double.NaN);
		}
	}
}
