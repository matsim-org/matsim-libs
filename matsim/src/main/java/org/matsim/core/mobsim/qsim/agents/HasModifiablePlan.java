/**
 * 
 */
package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author kainagel
 *
 */
public interface HasModifiablePlan {

	Plan getModifiablePlan();
	
	void resetCaches() ;

}
