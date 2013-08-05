/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.ptSubModes.replanning;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

/**
 * @author droeder
 *
 */
class PtSubModePtInteractionRemover implements PlanAlgorithm {
	private static final Logger log = Logger
			.getLogger(PtSubModePtInteractionRemover.class);
	
	protected PtSubModePtInteractionRemover(){
		//do nothing
	}
	
	private boolean thrown = false;

	@Override
	public void run(Plan plan) {
		List<PlanElement> newPlanElements = new ArrayList<PlanElement>();
		newPlanElements.add( plan.getPlanElements().get(0));
		List<PlanElement> temp = new ArrayList<PlanElement>();
		PlanElement e;
		// very complex. easier way?
		for(int i = 1; i < plan.getPlanElements().size(); i++){
			e = plan.getPlanElements().get(i);
			temp.add(e);
			// a plan needs at least 3 PlanElements to work
			if(temp.size() > 1){
				if(e instanceof Activity){
					// a 'subtour' ends, when a non-'pt interaction' occurs
					if(!((Activity) e).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
						// this might be a non-pt-chain or it is a single-transit_walk
						if(temp.size() == 2){
							if(((Leg) temp.get(0)).getMode().equals(TransportMode.transit_walk)){
								if(!thrown){
									log.warn("found act-transitWalk-act without any 'real' pt-leg. LegMode set to 'pt'." +
											" Thrown only once (per Thread).");
									thrown = true;
								}
								((Leg) temp.get(0)).setMode(TransportMode.pt);
								((Leg) temp.get(0)).setRoute(null);
							}
							newPlanElements.addAll(temp);
						}
						// this is a "pt-chain". Throw away all unnecessary pt legs and activities...
						else{
							PlanElement delegate = null;
							// find (at least) the one leg which is not a transitWalk. ignore the activities
							for(int ii = 0; ii < temp.size() - 1; ii += 2 ){
								if(!((Leg) temp.get(ii)).getMode().equals(TransportMode.transit_walk)){
									if(delegate == null){
										delegate = temp.get(ii);
									}else{
										// if more modes used than one, assume there is no fixed mode. Thus set TransportMode.pt
										if(!((Leg) temp.get(ii)).getMode().equals(((Leg) delegate).getMode())){
											delegate = new LegImpl(TransportMode.pt);
										}
									}
								}
							}
							//add the non-transit_walk-leg and the last activity - which must not be an "pt interaction"
							((Leg) delegate).setRoute(null);
							newPlanElements.add(delegate);
							newPlanElements.add(temp.get(temp.size() - 1));
						}
						// clear the temp-list, because all temp-PlanElements are added to the new PlanElements
						temp.clear();
					}
				}
			}
		}
		plan.getPlanElements().clear();
		plan.getPlanElements().addAll(newPlanElements);
	}

}
