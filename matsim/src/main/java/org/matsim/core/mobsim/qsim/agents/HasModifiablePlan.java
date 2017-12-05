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

	int getCurrentLinkIndex();
	// not totally obvious that this should be _here_, but it really only makes sense together with the modifiable plan/within-da replanning
	// capability.  Maybe should find a different name for the interface. kai, nov'17
	
}
