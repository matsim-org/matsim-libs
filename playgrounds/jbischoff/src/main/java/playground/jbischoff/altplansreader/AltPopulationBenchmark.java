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

package playground.jbischoff.altplansreader;

import java.util.HashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ParallelPopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */

public class AltPopulationBenchmark {
	public static void main (String[] args){
		
	 runMany();
//	 runOne();
//	 runTest();
	
	
	}
	
	

	private static void runMany(){
		
		String file = "C:\\jbcache\\749.output_plans.xml";
		
		Config c = ConfigUtils.createConfig();
		c.network().setInputFile("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\popreader\\749.output_network.xml");
		c.global().setNumberOfThreads(2);
		HashMap<Integer, Long> altperf = new HashMap<Integer,Long>();
		HashMap<Integer, Long> perf = new HashMap<Integer,Long>();
		HashMap<Integer, Long> sperf = new HashMap<Integer,Long>();
		
		for (int i = 0; i<5;i++){
			System.out.println("Parallel Population  " + i);
			
			Scenario scen = ScenarioUtils.loadScenario(c);
			ParallelPopulationReaderMatsimV4 pr = new ParallelPopulationReaderMatsimV4(scen);
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
			AltPopulationReaderMatsimV4 pr = new AltPopulationReaderMatsimV4(scen);
			long start = System.currentTimeMillis();
			pr.readFile(file);
			long end = System.currentTimeMillis();
			long cperf = end-start;
			System.out.println(i+": "+cperf);
			altperf.put(i,cperf);
			
		}
		c.global().setNumberOfThreads(1);
		for (int i = 0; i<5;i++){
			System.out.println("Parallel Population  " + i);
			
			Scenario scen = ScenarioUtils.loadScenario(c);
			ParallelPopulationReaderMatsimV4 pr = new ParallelPopulationReaderMatsimV4(scen);
			long start = System.currentTimeMillis();
			pr.readFile("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\popreader\\749.output_plans.xml");
			long end = System.currentTimeMillis();
			long cperf = end-start;
			System.out.println(i+": "+cperf);
			sperf.put(i,cperf);
			
		}
		System.out.println("Alt Perfomance:" + altperf+"\n\n");
		System.out.println("Parr Performance:"+ perf+"\n\n");
		System.out.println("Parr Performance 1thread:"+ sperf);
		
	}

}
