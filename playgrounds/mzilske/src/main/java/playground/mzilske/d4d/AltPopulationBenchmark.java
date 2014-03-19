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

package playground.mzilske.d4d;

import java.util.HashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */

public class AltPopulationBenchmark {
	public static void main (String[] args){
		runMany();
	}



	private static void runMany(){

		String file = "C:\\jbcache\\749.output_plans.xml";

		Config c = ConfigUtils.createConfig();
		c.global().setNumberOfThreads(2);
		HashMap<Integer, Long> altperf = new HashMap<Integer,Long>();
		HashMap<Integer, Long> perf = new HashMap<Integer,Long>();
		HashMap<Integer, Long> sperf = new HashMap<Integer,Long>();

		for (int i = 0; i<5;i++){
			System.out.println("Parallel Population  " + i);
			Scenario scen = ScenarioUtils.loadScenario(c);
			PopulationReaderMatsimV5 pr = new PopulationReaderMatsimV5(scen);
			long start = System.currentTimeMillis();
			pr.readFile(file);
			long end = System.currentTimeMillis();
			long cperf = end-start;
			System.out.println(i+": "+cperf);
			perf.put(i,cperf);

		}



		for (int i = 0; i<5;i++){
			System.out.println("Alternative Population  " + i);
			Scenario scen = ScenarioUtils.loadScenario(c);
			AltPopulationReaderMatsimV5 pr = new AltPopulationReaderMatsimV5(scen);
			long start = System.currentTimeMillis();
			pr.readFile(file);
			long end = System.currentTimeMillis();
			long cperf = end-start;
			System.out.println(i+": "+cperf);
			altperf.put(i,cperf);

		}
		System.out.println("Alt Perfomance:" + altperf+"\n\n");
		System.out.println("Parr Performance:"+ perf+"\n\n");
		System.out.println("Parr Performance 1thread:"+ sperf);

	}

}
