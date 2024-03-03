package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;

import java.util.List;

/**
 * Interface to determine which {@link ModeAvailability} are available to an agent and considered when computing best options.
 * <p>
 * This interface also contains the default implementations.
 *
 */
public interface ModeOptions {

	/**
	 * Determine options for one agent.
	 */
	List<ModeAvailability> get(Person person);

	/**
	 * Return whether an option allows to use the mode.
	 */
	default boolean allowUsage(ModeAvailability option) {
		return option.isModeAvailable();
	}

	/**
	 * The mode is always available and considered.
	 * This also means that there should be no daily costs associated with the mode.
	 */
	final class AlwaysAvailable implements ModeOptions {

		private static final List<ModeAvailability> YES = List.of(ModeAvailability.YES);
		private static final List<ModeAvailability> NO = List.of(ModeAvailability.NO);

		@Override
		public List<ModeAvailability> get(Person person) {
			return YES;
		}

	}

	/**
	 * Plans are considered with and without this mode.
	 */
	final class ConsiderYesAndNo implements ModeOptions {

		private static final List<ModeAvailability> BOTH = List.of(ModeAvailability.YES, ModeAvailability.NO);

		@Override
		public List<ModeAvailability> get(Person person) {
			return BOTH;
		}

	}


	/**
	 * Consider both options if car is available, otherwise none.
	 */
	final class ConsiderIfCarAvailable implements ModeOptions {

		@Override
		public List<ModeAvailability> get(Person person) {

			if (PersonUtils.canUseCar(person))
				return ConsiderYesAndNo.BOTH;

			return AlwaysAvailable.NO;
		}
	}
}
