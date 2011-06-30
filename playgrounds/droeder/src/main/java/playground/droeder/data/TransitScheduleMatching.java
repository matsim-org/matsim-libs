/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

import playground.droeder.data.graph.MatchingGraph;

/**
 * @author droeder
 *
 */
public class TransitScheduleMatching {

	/**
	 * @param base
	 * @param toMatch
	 */
	public TransitScheduleMatching(){
	}
	
	public TransitSchedule run(TransitSchedule base, TransitSchedule toMatch){
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		TransitSchedule sched = sc.getTransitSchedule();
		
		GraphMatching gm = new GraphMatching(schedule2Graph(base), schedule2Graph(toMatch));
		gm.run();
		
		this.createNewSchedule(sched, gm.getNodes(), gm.getEdges(), base, toMatch);
		return sched;
	}
	
	/**
	 * @param base2
	 * @return
	 */
	private MatchingGraph schedule2Graph(TransitSchedule base2) {
		// TODO Auto-generated method stub
		return null;
	}

	private void createNewSchedule(TransitSchedule newSched, Map<Id, Id> stopBase2Match, Map<Id, Id> routeBase2Match,
			TransitSchedule base, TransitSchedule toMatch){
		
		TransitScheduleFactory fac = newSched.getFactory();
		
		
		
	}

}
