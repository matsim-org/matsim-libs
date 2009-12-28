/* *********************************************************************** *
 * project: org.matsim.*
 * DgModeSwitchAnalyzer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.population;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PlanImpl.Type;
import org.matsim.core.utils.collections.Tuple;


/**
 * Create distinct groups from a given population by mode choice.
 * @author dgrether
 */
public class DgModeSwitchPlanTypeAnalyzer {
	
	private DgAnalysisPopulation pop;
	
	private Map<Tuple<Type, Type>, DgAnalysisPopulation> classifiedPops;

	public DgModeSwitchPlanTypeAnalyzer(DgAnalysisPopulation ana){
		this.pop = ana;
		this.classifiedPops = new HashMap<Tuple<Type, Type>, DgAnalysisPopulation>();
		this.classifyPopulationByPlanType();
	}
	
	private void classifyPopulationByPlanType(){		
		for (DgPersonData d : pop.getPersonData().values()) {
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);

			Tuple<Type, Type> modeSwitchTuple = new Tuple<Type, Type>(((PlanImpl) planDataRun1.getPlan()).getType(), ((PlanImpl) planDataRun2.getPlan()).getType());

			DgAnalysisPopulation p = this.classifiedPops.get(modeSwitchTuple);
			if (p == null){
				p = new DgAnalysisPopulation();
				this.classifiedPops.put(modeSwitchTuple, p);
			}
			p.getPersonData().put(d.getPersonId(), d);
		}
	}
	
	public DgAnalysisPopulation getPersonsForModeSwitch(Tuple<PlanImpl.Type, PlanImpl.Type> modes) {
		return this.classifiedPops.get(modes);
	}
	
	
}
