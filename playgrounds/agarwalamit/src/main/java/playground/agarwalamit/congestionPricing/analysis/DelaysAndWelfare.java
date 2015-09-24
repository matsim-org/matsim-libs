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

import playground.agarwalamit.analysis.congestion.AbsoluteDelays;
import playground.agarwalamit.analysis.userBenefits.UserBenefitsAndTotalWelfare;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * @author amit
 */

public class DelaysAndWelfare {

	static String clusterPathDesktop = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run11/policies/";
	static String [] runCases =  {"implV3","implV4","implV6"};

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

		outputDir = clusterPathDesktop+runCase;

		Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputDir);
		CongestionHandlerImplV3 impl3 = null ;
		CongestionHandlerImplV4 implV4 = null ;
//		CongestionHandlerImplV6 implV6 = null ;
		switch(runCase){
		case "implV3" :
			impl3 = new CongestionHandlerImplV3(manager, (ScenarioImpl) sc);
			manager.addHandler(impl3);
			break;
		case "implV4" : 
			implV4 = new CongestionHandlerImplV4(manager, sc);
			manager.addHandler(implV4);
			break;
		case "implV6" : 
//			implV6 = new CongestionHandlerImplV6(manager, sc);
//			manager.addHandler(implV6);
			break;
		}

		reader.readFile(outputDir+"/ITERS/it.1500/1500.events.xml.gz");

		switch(runCase){
		case "implV3" :
			impl3.writeCongestionStats(outputDir+"/ITERS/it.1500/congestionStats.csv");
			break;
		case "implV4":
			implV4.writeCongestionStats(outputDir+"/ITERS/it.1500/congestionStats.csv");
			break;
		case "implV6":
//			implV6.writeCongestionStats(outputDir+"/ITERS/it.1500/congestionStats.csv");
			break;
		}
	}


}
