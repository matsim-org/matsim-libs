/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class IntermodalTripDistanceAnalyser {
	public static void main(String[] args) {
		String runId = "25pct.r05";
		String plansFile = "D:/runs-svn/bvg_intermodal/"+runId+"/"+runId+".output_plans.xml.gz";
		String networkFile = "D:/runs-svn/bvg_intermodal/"+runId+"/"+runId+".output_network.xml.gz";
		String outFilePrefix = "D:/runs-svn/bvg_intermodal/"+runId+"/"+runId+".";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		List<String> modeConnections = new ArrayList<>();
		Set<String> modes = new HashSet<>();
		modes.add("bike");
		modes.add("car");
		modes.add("taxi");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
		spr.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				Plan plan = person.getSelectedPlan();
				String lastActivity = "";
				String lastRelevantMode= null;
				Double lastRelevantLegDistance = null;
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity)
					{
						Activity act = (Activity) pe;
						if ((lastActivity.equals("pt interaction"))&&(!act.getType().equals("pt interaction"))||(!lastActivity.equals("pt interaction"))&&(act.getType().equals("pt interaction")))
							{
								if (lastRelevantLegDistance!=null)
								{
									modeConnections.add(person.getId()+";"+lastRelevantMode+";"+lastRelevantLegDistance);
								}
							}
						lastActivity = act.getType();
					}
					else if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (modes.contains(leg.getMode())){
							lastRelevantLegDistance = calcBeelineLegDistance(leg, scenario);
							lastRelevantMode = leg.getMode();
						} else 
						{
							lastRelevantLegDistance = null;
							lastRelevantMode = null;
						}
					}
				}
			}				
		}
		);
		spr.readFile(plansFile);
	
		JbUtils.collection2Text(modeConnections, outFilePrefix+"intermodalConnectionDistances.csv");
	}


	/**
	 * @param leg
	 * @return
	 */
	private static double calcBeelineLegDistance(Leg leg, Scenario scenario) {
		Link fromLink = scenario.getNetwork().getLinks().get(leg.getRoute().getStartLinkId());
		Link toLink = scenario.getNetwork().getLinks().get(leg.getRoute().getEndLinkId());
		return CoordUtils.calcEuclideanDistance(fromLink.getCoord(), toLink.getCoord());
	}
	
	
}
