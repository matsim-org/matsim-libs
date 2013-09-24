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
package playground.droeder.buildingEnergy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public abstract class BuildingEnergyMain {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyMain.class);
	private static int LENGTH = 1; 

	/**
	 * 
	 * @param args {configfile}
	 */
	public static void main(String[] args) {
		boolean overwrite = false;
		if(args.length == 0){
			args = new String[]{"C:/Users/Daniel/Desktop/buildingEnergy/config.xml"};
			overwrite = true;
		}
		if(args.length != LENGTH){
			log.error("expecting " + LENGTH + " arguments...");
			System.exit(-1);
		}
		OutputDirectoryLogging.catchLogEntries();
		
		log.info("running " + BuildingEnergyMain.class.getName() );
		String configFilename = args[0];
		log.info("configfile: " + configFilename);

		// initialize
		Config c = ConfigUtils.loadConfig(configFilename);
		if(overwrite){
//			ConfigUtils.modifyFilePaths(c, "E:/sandbox/org.matsim");
		}
		Scenario sc = ScenarioUtils.loadScenario(c);
		// run
		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(overwrite);
		controler.addControlerListener(new MyControlerListener(c, sc.getNetwork()));
		controler.run();
	}
	
	// static class which plug the analysis to the controler and generates some more output (distance- & tt-shares) 
	private static class MyControlerListener implements IterationStartsListener,
												IterationEndsListener{
		
		private SimpleTripAnalyzer analyzer;
		
		MyControlerListener(Config c, Network net){
			analyzer =  new SimpleTripAnalyzer(c, net);
		}
		
		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			if(event.getIteration() == event.getControler().getConfig().controler().getLastIteration()){
				analyzer.reset(event.getIteration());
				event.getControler().getEvents().addHandler(analyzer);
			}
		}
		
		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			if(event.getIteration() == event.getControler().getConfig().controler().getLastIteration()){
				analyzer.run(event.getControler().getPopulation());
				String path = event.getControler().getControlerIO().getOutputPath() + System.getProperty("file.separator");
				String prefix = event.getControler().getConfig().controler().getRunId() + ".";
				analyzer.dumpData(path, prefix);
				calcWriteDistanceDistribution(analyzer.getTraveller(), path + prefix + "distanceShare.csv.gz");
				calcAndWriteTTDistribution(analyzer.getTraveller(), path + prefix + "ttShare.csv.gz");
			}
		}
		
		/**
		 * @param traveller
		 */
		private void calcAndWriteTTDistribution(Map<Id, Traveller> traveller, String file) {
			@SuppressWarnings("serial")
			List<Integer> distribution =  new ArrayList<Integer>(){{
				add(0);
				add(300);
				add(600);
				add(900);
				add(1200);
				add(1500);
				add(1800);
				add(2700);
				add(3600);
				add(5400);
				add(7200);
				add(Integer.MAX_VALUE);
			}};
			Map<String, Map<Integer, Integer>> map = new TreeMap<String, Map<Integer,Integer>>();
			for(Traveller t :traveller.values()){
				for(Trip trip : t.trips){
					Map<Integer, Integer> temp = getColumn(map, distribution, trip.mode);
					increase(temp, trip.getDuration());
				}
			}
			dumpData(map, file);
		}
		
		/**
		 * @param traveller
		 */
		private void calcWriteDistanceDistribution(Map<Id, Traveller> traveller, String file) {
			@SuppressWarnings("serial")
			List<Integer> distribution =  new ArrayList<Integer>(){{
				add(0);
				add(100);
				add(200);
				add(500);
				add(1000);
				add(2000);
				add(5000);
				add(10000);
				add(20000);
				add(50000);
				add(100000);
				add(Integer.MAX_VALUE);
			}};
			Map<String, Map<Integer, Integer>> map = new TreeMap<String, Map<Integer,Integer>>();
			for(Traveller t :traveller.values()){
				for(Trip trip : t.trips){
					Map<Integer, Integer> temp = getColumn(map, distribution, trip.mode);
					increase(temp, trip.dist);
				}
			}
			dumpData(map, file);
		}
		
		/**
		 * @param map
		 * @param file
		 */
		private void dumpData(Map<String, Map<Integer, Integer>> map, String file) {
			BufferedWriter w = IOUtils.getBufferedWriter(file);
			Map<Integer, Integer> header = map.values().iterator().next();
			try {
				w.write(";");
				for(Integer i :header.keySet()){
					w.write(i + ";");
				}
				w.write("\n");
				for(Entry<String, Map<Integer, Integer>> e: map.entrySet()){
					w.write(e.getKey() +";");
					for(Integer i: e.getValue().values()){
						w.write(i + ";");
					}w.write("\n");
				}
				w.flush();
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * @param temp
		 * @param dist
		 */
		private void increase(Map<Integer, Integer> temp, Double value) {
			for(Integer i : temp.keySet()){
				if(value <= i){
					temp.put(i, temp.get(i) + 1);
					return;
				}
			}
		}
		
		
		
		private Map<Integer, Integer> getColumn(Map<String, Map<Integer,Integer>> map, List<Integer> distribution, String mode){
			if(map.containsKey(mode)) return map.get(mode);
			Map<Integer, Integer> temp = new LinkedHashMap<Integer, Integer>();
			for(Integer i : distribution){
				temp.put(i, 0);
			}
			map.put(mode, temp);
			return temp;
		}
		
	}
	
}


