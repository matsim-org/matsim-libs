package org.matsim.contribs.discrete_mode_choice.model.mode_chain;

import java.util.Iterator;
import java.util.List;

/**
 * For chain-based choices such as plan-based or tour-based choice models, a set
 * of possible chains of modes needs to be created. This interface defines a
 * process that iteratively constructs such chains.
 * 
 * @author sebhoerl
 */
public interface ModeChainGenerator extends Iterator<List<String>> {
	long getNumberOfAlternatives();
}
