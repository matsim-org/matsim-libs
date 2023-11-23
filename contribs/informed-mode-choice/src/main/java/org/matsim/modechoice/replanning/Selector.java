package org.matsim.modechoice.replanning;

import javax.annotation.Nullable;
import java.util.Collection;

interface Selector<T> {

	/**
	 * Select one entity from a list of candidates. Return null when not applicable.
	 */
	@Nullable
	T select(Collection<T> candidates);
}
