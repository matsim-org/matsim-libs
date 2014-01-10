/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.julia.distribution;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class ResponsibilityScoringFunctionFactory implements
		ScoringFunctionFactory {
	
	private CharyparNagelScoringFunctionFactory delegate;
	private ArrayList<ResponsibilityEvent> allRevents;

	public ResponsibilityScoringFunctionFactory(Config config, Network network, ArrayList<ResponsibilityEvent> allRevents){
		this.delegate = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), network);
		this.allRevents = allRevents;
		
	}
	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		
			ArrayList<ResponsibilityEvent> rEventsOfPerson = new ArrayList<ResponsibilityEvent>();
			Id personId = plan.getPerson().getId();
			for(ResponsibilityEvent re: allRevents){
				if(re.getResponsiblePersonId().equals(personId)){
					rEventsOfPerson.add(re);
				}
			}	
		return new ResponsibilityScoringFunction(plan.getPerson().getId(), delegate.createNewScoringFunction(plan), rEventsOfPerson);
	}

}
