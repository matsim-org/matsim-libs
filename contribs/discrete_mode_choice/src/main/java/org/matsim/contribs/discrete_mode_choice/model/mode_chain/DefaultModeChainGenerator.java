package org.matsim.contribs.discrete_mode_choice.model.mode_chain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * This is the default mode chain generator. It can be used right away or as a
 * template for more specialized implementatons.
 * 
 * It works as follows: Since the available modes are known and the number of
 * trips in the requested mode chain it is possible to calculate the total
 * amount of chains that can be created: modes ^ trips. Therefore, each distinct
 * chain can be encoded as an integer. What happens then is that a counter is
 * increased every time a new mode chain is requested and a new chain
 * corresponding to that integer is created. This way this generator has a very
 * low memory footprint.
 * 
 * @author sebhoerl
 *
 */
public class DefaultModeChainGenerator implements ModeChainGenerator {
	final private List<String> availableModes;

	final private int numberOfTrips;
	final private int numberOfModes;

	final private long maximumAlternatives;

	private int index = 0;

	public DefaultModeChainGenerator(Collection<String> availableModes, int numberOfTrips) {
		this.availableModes = new ArrayList<>(availableModes);
		this.numberOfModes = availableModes.size();
		this.numberOfTrips = numberOfTrips;
		this.maximumAlternatives = ArithmeticUtils.pow((long) numberOfModes, numberOfTrips);
	}

	public long getNumberOfAlternatives() {
		return maximumAlternatives;
	}

	@Override
	public boolean hasNext() {
		return index < maximumAlternatives;
	}

	@Override
	public List<String> next() {
		if (!hasNext()) {
			throw new IllegalStateException();
		}

		List<String> chain = new ArrayList<>(numberOfTrips);
		int copy = index;

		for (int k = 0; k < numberOfTrips; k++) {
			chain.add(availableModes.get(copy % numberOfModes));
			copy -= copy % numberOfModes;
			copy /= numberOfModes;
		}

		index++;

		return chain;
	}

	static public class Factory implements ModeChainGeneratorFactory {
		@Override
		public ModeChainGenerator createModeChainGenerator(Collection<String> modes, Person person,
				List<DiscreteModeChoiceTrip> trips) {
			return new DefaultModeChainGenerator(modes, trips.size());
		}
	}
}
