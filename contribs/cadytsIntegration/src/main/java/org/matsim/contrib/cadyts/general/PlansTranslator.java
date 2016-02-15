/**
 * 
 */
package org.matsim.contrib.cadyts.general;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author nagel
 *
 */
public interface PlansTranslator<T> {
	
	cadyts.demand.Plan<T> getCadytsPlan(final Plan plan) ; 
	
}
