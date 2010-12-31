/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.LEGO.miniscenario.create;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.LEGO.miniscenario.run.scoring.DestinationChoiceScoring;

public class EpsilonComputer implements PlanAlgorithm {
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private DestinationChoiceScoring scorer;
			
	public EpsilonComputer(ScenarioImpl scenario, ConfigReader configReader, String type, TreeMap<Id, ActivityFacility> typedFacilities,
			DestinationChoiceScoring scorer) {		
		this.type = type;
		this.typedFacilities = typedFacilities;
		this.scorer = scorer;
	}
		
	@Override
	public void run(Plan plan) {
		Person p = plan.getPerson();
		//ceck if plan contains activity of type
		boolean typeInPlan = false;
		for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().startsWith(type)) typeInPlan = true;
			}
		}
		double maxEpsilon = 0.0;
		if (typeInPlan) {
			for (Facility f : typedFacilities.values()) {
				ActivityImpl act = new ActivityImpl(type, new IdImpl(1));
				act.setFacilityId(f.getId());
				double epsilon = scorer.getDestinationScore((PlanImpl)p.getSelectedPlan(), act, false);
				
				if (epsilon > maxEpsilon) {
					maxEpsilon = epsilon;
				}
			}
		}
		String desiresDesc = ((PersonImpl)p).getDesires().getDesc();
		((PersonImpl)p).getDesires().setDesc(desiresDesc + "_" + maxEpsilon);	
	}
}