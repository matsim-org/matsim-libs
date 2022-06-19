package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;

import java.util.List;

/**
 * Interface to determine which mode options are available to an agent and considered when computing best options.
 * <p>
 * This interface also contains the default implementations.
 *
 * @param <T> enum listing the possible options
 */
public interface ModeOptions<T extends Enum<?>> {

	/**
	 * Determine options for one agent.
	 */
	List<T> get(Person person);

	/**
	 * Return whether an option allows to use the mode. Normally only one of the option should forbid using the mode at all.
	 */
	boolean allowUsage(T option);

	/**
	 * The mode is always available and considered.
	 */
	final class AlwaysAvailable implements ModeOptions<ModeAvailability> {

		private static final List<ModeAvailability> YES = List.of(ModeAvailability.YES);
		private static final List<ModeAvailability> NO = List.of(ModeAvailability.NO);

		@Override
		public List<ModeAvailability> get(Person person) {
			return YES;
		}

		@Override
		public boolean allowUsage(ModeAvailability option) {
			return option == ModeAvailability.YES;
		}
	}

	/**
	 * Plans are considered with and without this mode.
	 */
	final class ConsiderYesAndNo implements ModeOptions<ModeAvailability> {

		private static final List<ModeAvailability> BOTH = List.of(ModeAvailability.YES, ModeAvailability.NO);

		@Override
		public List<ModeAvailability> get(Person person) {
			return BOTH;
		}

		@Override
		public boolean allowUsage(ModeAvailability option) {
			return option == ModeAvailability.YES;
		}
	}


	/**
	 * Consider both options if car is available, otherwise none.
	 */
	final class ConsiderIfCarAvailable implements ModeOptions<ModeAvailability> {

		@Override
		public List<ModeAvailability> get(Person person) {

			if (PersonUtils.canUseCar(person))
				return ConsiderYesAndNo.BOTH;

			return AlwaysAvailable.NO;
		}

		@Override
		public boolean allowUsage(ModeAvailability option) {
			return option == ModeAvailability.YES;
		}
	}
}
