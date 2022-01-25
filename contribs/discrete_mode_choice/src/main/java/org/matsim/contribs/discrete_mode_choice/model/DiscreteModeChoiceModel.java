package org.matsim.contribs.discrete_mode_choice.model;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * Interface for a discrete mode choice model.
 * 
 * @author sebhoerl
 */
public interface DiscreteModeChoiceModel {
	/**
	 * This function should return a list of *chosen* candidates for the given
	 * trips.
	 */
	List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random)
			throws NoFeasibleChoiceException;

	/**
	 * Defines how choices are handled that cannot be taken, because constraints or
	 * missing modes make the choice infeasible.
	 */
	static public enum FallbackBehaviour {
				IGNORE_AGENT, INITIAL_CHOICE, EXCEPTION
	}

	/**
	 * Thrown if there is an infeasible choice to make.
	 */
	static public class NoFeasibleChoiceException extends Exception {
		private static final long serialVersionUID = -7909941248706791794L;

		public NoFeasibleChoiceException(String message) {
			super(message);
		}
	}
}
