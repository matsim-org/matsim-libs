/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.vsp.parkAndRide.replanning;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.misc.StringUtils;

/**
 * Removes park-and-ride from a randomly chosen home-work-home sequence.
 * 
 * @author ikaddoura
 *
 */
public class PRRemoveStrategyMod implements PlanStrategyModule {
	private static final Logger log = Logger.getLogger(PRRemoveStrategyMod.class);

	private Scenario sc;
	
	public PRRemoveStrategyMod(Scenario scenario) {
		this.sc = scenario;
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		
			List<PlanElement> planElements = plan.getPlanElements();
			
			PlanIndicesAnalyzer planIndices = new PlanIndicesAnalyzer(plan);
			planIndices.setIndices();
			
			boolean hasHomeActivity = planIndices.hasHomeActivity();
			boolean hasWorkActivity = planIndices.hasWorkActivity();
			
			if (hasHomeActivity == true && hasWorkActivity == true) {
				log.info("Plan contains home and work Activity. Proceeding...");

				final int rndWork = (int)(MatsimRandom.getRandom().nextDouble() * planIndices.getWorkActs().size());
				log.info("PlanElements of possible work Activities: " + planIndices.getWorkActs() + ". Removing park-and-ride of work activity " + planIndices.getWorkActs().get(rndWork) + "...");
				
				int workIndex;
				int maxHomeBeforeWork;
				int minHomeAfterWork;

				// Check if home-work-home sequence contains Park'n'Ride
				
				planIndices.setIndices();
				
				workIndex = planIndices.getWorkActs().get(rndWork);					
				minHomeAfterWork = planIndices.getMinHomeAfterWork(workIndex);
				maxHomeBeforeWork = planIndices.getMaxHomeBeforeWork(workIndex);

				List<Integer> prIndicesToRemove = new ArrayList<Integer>();
				for (Integer prIndex : planIndices.getPrActs()){
					if (prIndex > maxHomeBeforeWork && prIndex < minHomeAfterWork){
						prIndicesToRemove.add(prIndex);
					}
				}
				
				if (prIndicesToRemove.size() == 2) {
					log.info("Home-Work-Home sequence contains two park-and-ride activities. Removing the park-and-ride activities and randomly choose a mode for the remaining leg.");
					int remove0 = prIndicesToRemove.get(0);
					int remove1 = prIndicesToRemove.get(1);
					
					// adjust leg modes in this home-work-home sequence
					workIndex = planIndices.getWorkActs().get(rndWork);					
					minHomeAfterWork = planIndices.getMinHomeAfterWork(workIndex);
					maxHomeBeforeWork = planIndices.getMaxHomeBeforeWork(workIndex);
										
					List<String> availableModes = new ArrayList<String>();
					availableModes.add(TransportMode.car);
					availableModes.add(TransportMode.pt);

                    // TODO: Refactored out of core config
                    // Please just create and add the config group instead.
                    MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) sc.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
                    if (multiModalConfigGroup == null) {
                        multiModalConfigGroup = new MultiModalConfigGroup();
                        sc.getConfig().addModule(multiModalConfigGroup);
                    }
                    String modes = multiModalConfigGroup.getSimulatedModes();
					if (modes != null) {
						String[] parts = StringUtils.explode(modes, ',');
						for (int i = 0, n = parts.length; i < n; i++) {
							String mode = parts[i].trim().intern();
							if (availableModes.contains(mode)){
							} else {
								availableModes.add(mode);
							}
						}
					}
					
					log.info("Choosing mode from: " + availableModes);
					int rndModeIndex = (int)(MatsimRandom.getRandom().nextDouble() * availableModes.size());
					String mode = availableModes.get(rndModeIndex);
					log.info("Chosen mode: " + mode);
					for (int i = 0; i < planElements.size(); i++) {
						PlanElement pe = planElements.get(i);
						if (i == remove0-1 || i == remove0+1 || i == remove1-1 || i == remove1+1){
							// legs before / after P+R
							Leg leg = (Leg) pe;
							leg.setMode(mode);
						}
					}
					
					planElements.remove(remove1 - 1);
					planElements.remove(remove1 - 1);
					
					planElements.remove(remove0);
					planElements.remove(remove0);
				
				} else if (prIndicesToRemove.isEmpty()){
					log.info("Home-Work-Home sequence doesn't contain park-and-ride. Not modifying the plan...");
				} else if (prIndicesToRemove.size() > 2){
					log.warn("More than two park-and-ride activities in this Home-Work-Home sequence. This should not be possible. Not modifying the plan...");
				} else if (prIndicesToRemove.size() < 2){
					log.info("Home-Work-Home sequence only contains one park-and-ride. Not modifying the plan...");	
				}
				
			} else {
				if (hasWorkActivity == false){
					log.info("Plan doesn't contain Work Activity. Not modifying the plan...");
				} else if (hasHomeActivity == false){
					log.info("Plan doesn't contain Home Activity. Not modifying the plan...");
				}
			}
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub
	}
	
}
