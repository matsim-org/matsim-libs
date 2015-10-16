/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.synpop.sim;

import org.matsim.contrib.common.collections.Composite;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.Collection;

/**
 * @author johannes
 *
 */
public class MarkovEngineListenerComposite extends Composite<MarkovEngineListener> implements MarkovEngineListener {

	@Override
	public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
		for(MarkovEngineListener listener : components) listener.afterStep(population, mutations, accepted);
	}
}
