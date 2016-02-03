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
package playground.agarwalamit.analysis.legMode;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;

import playground.agarwalamit.utils.MapUtils;
import playground.benjamin.utils.BkNumberUtils;

/**
 * Generate Modal Share from last iteration plans file and events file.
 * 
 * @author amit
 */
public class ModalShareGenerator {
	
	private static final Logger LOG = Logger.getLogger(ModalShareGenerator.class);
	
	public void getModalShareFromEvents(){
		// ZZ_TODO should get modal share from events as well.
		throw new RuntimeException("Not implemented yet.");
	}
	
	public SortedMap<String, Double > getMode2PctShareFromPlans (final Population population){
		SortedMap<String , Double> mode2PctShare = new TreeMap<String, Double>();
		LOG.info("=====Modal split is calculated using input plans file.=====");
		SortedSet<String> usedModes = getUsedModes(population);

		LOG.info("=====The following transport modes are used: " + usedModes+".=====");
		Map<String, Integer> mode2NoOfLegs = getMode2NoOfLegs(population);
		int totalNoOfLegs = MapUtils.intValueSum(mode2NoOfLegs);
		
		for(String mode : mode2NoOfLegs.keySet()){
			double noOfLegs = (double) mode2NoOfLegs.get(mode);
			double noOfLegPct = BkNumberUtils.roundDouble(100. * ((double) noOfLegs / (double) totalNoOfLegs), 3);
			mode2PctShare.put(mode, noOfLegPct);
		}
		return mode2PctShare;
	}

	public SortedMap<String, Integer> getMode2NoOfLegs(final Population pop) {
		SortedMap<String, Integer> mode2noOfLegs = new TreeMap<String, Integer>();
		SortedSet<String> usedModes = getUsedModes(pop);
		
		for(String mode : usedModes){
			int noOfLegs = 0;
			for(Person person : pop.getPersons().values()){
				PlanImpl plan = (PlanImpl) person.getSelectedPlan();
				List<PlanElement> planElements = plan.getPlanElements();
				for(PlanElement pe : planElements){
					if(pe instanceof Leg){
						Leg leg = (Leg) pe;
						String legMode = leg.getMode();
						if(legMode.equals(mode)){
							noOfLegs ++;
						}

					}
				}
			}
			mode2noOfLegs.put(mode, noOfLegs);
		}
		return mode2noOfLegs;
	}

	private SortedSet<String> getUsedModes(final Population pop) {
		SortedSet<String> usedModes = new TreeSet<String>();
		for(Person person : pop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					String legMode = leg.getMode();
					if(!usedModes.contains(legMode)){
						usedModes.add(legMode);
					}
				}
			}
		}
		return usedModes;
	}
}