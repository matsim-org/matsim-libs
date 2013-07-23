/* *********************************************************************** *
 * project: org.matsim.*
 * DgAirStuckedAnalysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import air.analysis.stuck.BoardingDeniedStuckEvaluator;
import air.analysis.stuck.CollectBoardingDeniedStuckEventHandler;


/**
 * @author dgrether
 *
 */
public class DgAirStuckAnalysis {

	/**
	 */
	public static void main(String[] args) {
		String populationFile = "/home/dgrether/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/population_september_2011_tabelle_2.2.2.xml.gz";
		String[] runNumbers ={
//				"1836", 
//				"1837",
//				"1838",
//				"1839",
//				"1840",
//				"1841",			
//				"1848"
//				,
//				"1849",
//				"1850",
//				"1851",
//				"1852",
//				"1853"
				
//				"1854",
//				"1855",
//				"1856",
//				"1857",
//				"1858",
//				"1859",
//				
//				"1860",
//				"1861",
//				"1862",
//				"1863",
//				"1864"

			"1865",
			"1866",
			"1867",
			"1868",
			"1869",
				"1870"

		};
		String iteration = "600";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(populationFile);
		for (String runNumber : runNumbers) {
			String events = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".events.xml.gz";
			String outputFilePrefix = "/home/dgrether/data/work/repos/runs-svn/run"+runNumber+"/ITERS/it."+iteration+"/"+runNumber+"."+iteration+".";

			EventsManager eventsManager = EventsUtils.createEventsManager();
			CollectBoardingDeniedStuckEventHandler handler = new CollectBoardingDeniedStuckEventHandler();
			eventsManager.addHandler(handler);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(events);
			new BoardingDeniedStuckEvaluator(handler.getBoardingDeniedStuckEventsByPersonId(), sc.getPopulation()).writeToFiles(outputFilePrefix);
		}
	}

}
