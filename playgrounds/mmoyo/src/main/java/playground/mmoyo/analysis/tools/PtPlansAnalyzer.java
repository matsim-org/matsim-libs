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

package playground.mmoyo.analysis.tools;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.tools.PtPlanAnalyzer.PtPlanAnalysisValues;
import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;

/**calculates the sum of:
 * 	 	-walkTime in seconds, 
 * 		-in veh travel time in seconds, 
 * 		-in veh travel distance in seconds, 
 * 		-number of transfers 
 * and writes a tabular file (excel) with these date per agent*/
public class PtPlansAnalyzer implements PersonAlgorithm{ 
	private final String NR= "\n";
	private final String TB= "\t";
	private StringBuffer sBuff = new StringBuffer();
	private boolean onlySelectedPlan;
	private PtPlanAnalyzer ptPlanAnalyzer;
	
	public PtPlansAnalyzer (final Network network, final TransitSchedule schedule, final boolean onlySelectedPlan){
		ptPlanAnalyzer = new PtPlanAnalyzer(network, schedule);
		this.onlySelectedPlan = onlySelectedPlan;
		sBuff.append("AGENT\tPLAN_i\tSELECTED\tTR_WALK_TIME\tTR_TRAVEL_TIME\tTR_TRAVEL_DISTANCE\tTRANSFERS\tPT_LEGS\tPT_TRIPS\n");
	}

	@Override
	public void run (Person person) {
		if (onlySelectedPlan){
			run(person.getSelectedPlan());
		}else{
			int planIdx=0;
			for (Plan plan :person.getPlans()){ 
				run(plan);
				planIdx++;
			}	
		}
	}

	private void run(Plan plan){
		PtPlanAnalysisValues v = ptPlanAnalyzer.run(plan);
		Person person = plan.getPerson();
		int planIdx = person.getPlans().indexOf(plan);
		sBuff.append(person.getId() + TB +  planIdx + TB + plan.isSelected() + TB + v.getTransitWalkTime_secs() + TB +  v.trTravelTime_secs() + TB + v.getInVehDist_mts() + TB + v.getTransfers_num() + TB + v.getPtLegs_num() + TB + v.getPtTrips_num() + NR); //Store values in string buffer		
	}

	private void writeAnalysis(final String outFile){
		new TextFileWriter().write(sBuff.toString(), outFile, false);
	}
	
	public static void main(String[] args) {
		String popFile = "";
		String netFile = "";
		String scheduleFile = "";
		String outFile = "";
		boolean onlySelectedPlan = false;
		
		DataLoader dLoader = new DataLoader();
		Network net = dLoader.readNetwork(netFile);
		TransitSchedule schedule = dLoader.readTransitSchedule(scheduleFile);
		
		PtPlansAnalyzer ptPlansAnalyzer = new PtPlansAnalyzer(net, schedule, onlySelectedPlan);
		ScenarioImpl scn = (ScenarioImpl) dLoader.createScenario();
		PopSecReader popSecReader = new PopSecReader (scn, ptPlansAnalyzer);
		popSecReader.readFile(popFile);
		ptPlansAnalyzer.writeAnalysis(outFile);
	}

}