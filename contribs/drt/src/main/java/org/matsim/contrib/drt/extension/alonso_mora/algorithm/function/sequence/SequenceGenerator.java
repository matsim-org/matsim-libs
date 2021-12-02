package org.matsim.contrib.drt.extension.alonso_mora.algorithm.function.sequence;

import java.util.List;

import org.matsim.contrib.drt.extension.alonso_mora.algorithm.AlonsoMoraStop;

/**
 * A sequence generator constructs sequences of stops one by one. This means
 * that a call to advance may either extend the current sequence, or, if it has
 * been aborted due to constraints, goes back to a previous solution for further
 * expansion.
 * 
 * @author sebhoerl
 */
public interface SequenceGenerator {
	void advance();

	void abort();

	boolean hasNext();

	List<AlonsoMoraStop> get();

	boolean isComplete();
}
