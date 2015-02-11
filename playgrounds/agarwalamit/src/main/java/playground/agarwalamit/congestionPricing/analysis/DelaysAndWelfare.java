/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.congestionPricing.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.congestion.AbsoluteDelays;
import playground.agarwalamit.analysis.userBenefits.UserBenefitsAndTotalWelfare;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * @author amit
 */

public class DelaysAndWelfare {

	static String clusterPathDesktop = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/";
	static String [] runCases =  {"implV3","implV4"};

	public static void main(String[] args) {
		
		new AbsoluteDelays(clusterPathDesktop).runAndWrite(runCases);
		new UserBenefitsAndTotalWelfare(clusterPathDesktop).runAndWrite(runCases);
		
		for(String runCase :runCases){
			writeCongestionStatsExternally(runCase);
		}
		
	}

	private static void writeCongestionStatsExternally(String runCase){

		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);

		String outputDir = null;

		switch(runCase){
		case "implV3" : outputDir = clusterPathDesktop+"implV3";
		break;
		case "implV4" : outputDir = clusterPathDesktop+"implV4";
		break;
		}

		Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputDir);
		CongestionHandlerImplV3 impl3 = null ;
		CongestionHandlerImplV4 implV4 = null ;
		switch(runCase){
		case "implV3" :
			impl3 = new CongestionHandlerImplV3(manager, (ScenarioImpl) sc);
			manager.addHandler(impl3);
			break;
		case "implV4" : 
			implV4 = new CongestionHandlerImplV4(manager, sc);
			manager.addHandler(implV4);
			break;
		}

		reader.readFile(outputDir+"/ITERS/it.1000/1000.events.xml.gz");
		switch(runCase){
		case "implV3" :
			impl3.writeCongestionStats(outputDir+"/ITERS/it.1000/congestionStats.csv");
			break;
		case "implV4":
			implV4.writeCongestionStats(outputDir+"/ITERS/it.1000/congestionStats.csv");
			break;
		}
	}


}
