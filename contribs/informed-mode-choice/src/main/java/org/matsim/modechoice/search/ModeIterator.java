package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.doubles.DoubleIterator;

sealed interface ModeIterator extends DoubleIterator permits ModeArrayIterator, ModeLongIterator, ModeIntIterator {

	/**
	 * Maximum number of iterations. Memory usage will increase the more iterations are done.
	 */
	int MAX_ITER = 10_000_000;

	default int maxIters() {
		return MAX_ITER;
	}

}
