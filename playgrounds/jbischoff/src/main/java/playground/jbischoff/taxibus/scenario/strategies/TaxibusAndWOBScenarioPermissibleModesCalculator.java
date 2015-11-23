/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.strategies;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;

import playground.jbischoff.taxibus.optimizer.fifo.Lines.LineDispatcher;

/**
 * @author  jbischoff
 *
 */
public class TaxibusAndWOBScenarioPermissibleModesCalculator extends TaxibusPermissibleModesCalculatorImpl {

	private final Scenario scenario;
	
	public TaxibusAndWOBScenarioPermissibleModesCalculator(String[] availableModes, LineDispatcher dispatcher, Scenario scenario) {
		super(availableModes, dispatcher);
		this.scenario = scenario;
	}
	
	
	@Override
	public Collection<String> getPermissibleModes(Plan plan) {
		Collection<String> permissibleModes = super.getPermissibleModes(plan);
		String subpop = (String) scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "subpopulation");
		if (subpop.equals("schedulePt")){
			permissibleModes.remove("tpt");
			
		}
		else if (subpop.equals("teleportPt")){
			permissibleModes.remove("pt");
		}
		
		return permissibleModes;
	}

}
