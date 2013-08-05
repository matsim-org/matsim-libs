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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author droeder
 *
 */
class ReturnToOldModesStrategy extends AbstractMultithreadedModule {

	private static final Logger log = Logger
			.getLogger(ReturnToOldModesStrategy.class);
	private Map<Id, List<String>> originalModes;

	protected ReturnToOldModesStrategy(Scenario sc, Map<Id, List<String>> originalModes) {
		super(sc.getConfig().global());
		this.originalModes = originalModes;
	}


	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ReturnToOldLegMode(this.originalModes);
	}

	private class ReturnToOldLegMode implements PlanAlgorithm{
		
		private Map<Id, List<String>> originalModes;

		public ReturnToOldLegMode(Map<Id, List<String>> originalModes){
			this.originalModes = originalModes;
		}

		private boolean thrown = false;
		@Override
		public void run(Plan plan) {
			if(this.originalModes.containsKey(plan.getPerson().getId())){
				List<String> legModes = this.originalModes.get(plan.getPerson().getId());
				
				if(((legModes.size() * 2) + 1) != plan.getPlanElements().size()){
					log.warn("Person " + plan.getPerson().getId() + " is probably no longer using original pt-subModes. " +
							" Removing OriginalLegmodes...");
					this.originalModes.remove(plan.getPerson().getId());
				}else{
					//modify the planElements
					for(int i = 1; i < plan.getPlanElements().size(); i += 2){
						Leg l = (Leg) plan.getPlanElements().get(i);
						String mode = legModes.get(i/2);
						if(!l.getMode().equals(mode)){
							if(!this.thrown){
								log.warn("Changing Legmode for person " + plan.getPerson().getId() + " from " + l.getMode() 
										+ " to " + mode + ". Thrown only once (per thread)...");
								this.thrown = true;
							}
							l.setMode(mode);
						}
					}
				}
			}
			
		}
	}

}

