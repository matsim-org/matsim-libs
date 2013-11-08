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
package playground.vsp.buildingEnergy.energyCalculation;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * 
 * Analyzes the number of persons working/ performing home activities. 
 * Note, a person performing a certain activity-type more than once, will
 * be counted only once
 * @author droeder
 *
 */
public class BuildingEnergyPlansAnalyzer extends AbstractPersonAlgorithm {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(BuildingEnergyPlansAnalyzer.class);
	private String homeType;
	private String workType;
	private int workCnt = 0;
	private int homeCnt = 0;

	public BuildingEnergyPlansAnalyzer(String homeType, String workType) {
		this.homeType = homeType;
		this.workType = workType;
	}

	@Override
	public void run(Person person) {
		boolean work = false;
		boolean home = false;
		List<PlanElement> pe = person.getSelectedPlan().getPlanElements();
		for(int i = 0; i< pe.size(); i += 2){
			Activity a = (Activity) pe.get(i);
			if(!work){
				if(a.getType().equals(workType)){
					work = true;
					workCnt++;
				}
			}
			if(!home){
				if(a.getType().equals(homeType)){
					home = true;
					homeCnt++;
				}
			}
			// we know everything we need to know, return
			if(home && work) return;
		}
	}

	/**
	 * @return the workCnt
	 */
	public final int getWorkCnt() {
		return workCnt;
	}


	/**
	 * @return the homeCnt
	 */
	public final int getHomeCnt() {
		return homeCnt;
	}


}

