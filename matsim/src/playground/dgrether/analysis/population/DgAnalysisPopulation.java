/* *********************************************************************** *
 * project: org.matsim.*
 * PlanComparison.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
/**
 * This Class provides a data object to compare to iterations. 
 * @author dgrether
 * 
 */
public class DgAnalysisPopulation {
	
	private static final Logger log = Logger.getLogger(DgAnalysisPopulation.class);
	
	public static final Id RUNID1 = new IdImpl("run1");
	public static final Id RUNID2 = new IdImpl("run2");
	
	private Map<Id, DgPersonData> table;
	/**
	 * Creates a PlanComparison Object with the initial size
	 * @param size
	 */
	public DgAnalysisPopulation() {
     table = new LinkedHashMap<Id, DgPersonData>();
	}
	
	public Map<Id, DgPersonData> getPersonData() {
		return table;
	}
	
	public int calculateNumberOfCarPlans(Id runId) {
		int carplans = 0;
		for (DgPersonData d : table.values()) {
			if (d.getPlanData().get(runId).getPlan().getType().equals(PlanImpl.Type.CAR)){
				carplans++;
			}
		}
		return carplans;
	}

}
