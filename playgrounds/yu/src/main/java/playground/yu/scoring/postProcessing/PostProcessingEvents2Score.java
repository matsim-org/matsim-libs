/* *********************************************************************** *
 * project: org.matsim.*
 * PostProcessingEvents2Score.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.scoring.postProcessing;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.utils.misc.ConfigUtils;

public class PostProcessingEvents2Score {

	public static void main(String[] args) {
//		String configFilename = "../integration-demandCalibration/test/DestinationUtilOffset/analysis.xml";
//		String popFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.plans.xml.gz";
//		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
//		String eventsFilename = "../integration-demandCalibration/test/DestinationUtilOffset/1000.events.txt.gz";
//		String chartFilenameBase = "../integration-demandCalibration/test/DestinationUtilOffset/1000.departTime_travelTime.";
		String configFilename = "/net/ils/chen/utilityOffsets/analysis.xml";
		String popFilename = "/net/ils/chen/matsim-bse/outputs/4SE_DC/raiseTRB/ITERS/it.1000/1000.plans.xml.gz";
		String netFilename = "/net/work/chen/data/ivtch/input/ivtch-osm.xml";
		String eventsFilename = "/net/ils/chen/matsim-bse/outputs/4SE_DC/raiseTRB/ITERS/it.1000/1000.events.txt.gz";
		String chartFilenameBase = "/net/ils/chen/matsim-bse/outputs/4SE_DC/raiseTRB/ITERS/it.1000/1000.departTime_travelTime.";

		try {
			Config config = ConfigUtils.loadConfig(configFilename);
			Scenario scenario = new ScenarioImpl(config);

			Network net = scenario.getNetwork();
			new MatsimNetworkReader(scenario).readFile(netFilename);

			Population pop = scenario.getPopulation();
			new MatsimPopulationReader(scenario).readFile(popFilename);

			EventsManager events = new EventsManagerImpl();

			CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord scoringFactory = new CharyparNagelScoringFunctionFactoryWithDetailedLegScoreRecord(
					scenario.getConfig().planCalcScore());
			EventsToScore events2score = new EventsToScore(pop, scoringFactory);
			events.addHandler(events2score);
			new MatsimEventsReader(events).readFile(eventsFilename);
		} catch (IOException e) {
			e.printStackTrace();
		}

		LegScoringFunctionWithDetailedRecord.createChart(chartFilenameBase);
	}
}
