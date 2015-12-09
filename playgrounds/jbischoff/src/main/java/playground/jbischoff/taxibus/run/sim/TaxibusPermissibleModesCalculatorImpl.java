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

package playground.jbischoff.taxibus.run.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PermissibleModesCalculator;

import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LineDispatcher;

/**
 * @author  jbischoff
 *
 */
public class TaxibusPermissibleModesCalculatorImpl implements PermissibleModesCalculator, TaxibusPermissibleModesCalculator {

	private final LineDispatcher dispatcher;
	private List<String> availableModes;
	
	public TaxibusPermissibleModesCalculatorImpl(String[] availableModes, LineDispatcher dispatcher) {
		this.availableModes = new ArrayList<>();
		this.availableModes.addAll(Arrays.asList(availableModes));
		if (this.availableModes.contains("taxibus")){
			this.availableModes.remove("taxibus");
		}
		this.dispatcher = dispatcher;
	}
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.taxibus.run.sim.TaxibusPermissibleModesCalculator#getPermissibleModes(org.matsim.api.core.v01.population.Plan)
	 */
	@Override
	public Collection<String> getPermissibleModes(Plan plan) {
		boolean isServedByTaxibus = true;
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Activity){
				Activity act = (Activity) pe;
				if (!dispatcher.coordIsServedByLine(act.getCoord())){
					isServedByTaxibus = false;
//					System.out.println(plan.getPerson().getId() +" not served");
					break;
				} 
			}
		}
		List<String> allModes = new ArrayList<>();
		allModes.addAll(availableModes);
		if (isServedByTaxibus){
			allModes.add("taxibus");
		}
		return allModes;
	}

}
