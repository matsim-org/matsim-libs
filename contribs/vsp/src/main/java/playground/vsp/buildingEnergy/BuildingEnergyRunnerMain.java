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
package playground.vsp.buildingEnergy;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.vsp.analysis.modules.simpleTripAnalyzer.SimpleTripAnalyzer;
import playground.vsp.analysis.modules.simpleTripAnalyzer.Traveller;
import playground.vsp.analysis.modules.simpleTripAnalyzer.Trip;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;


/**
 * @author droeder
 *
 */
public abstract class BuildingEnergyRunnerMain {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyRunnerMain.class);
	private static int LENGTH = 2; 

	/**
	 * 
	 * @param args {configfile agents2exclude.csv}
	 */
	public static void main(String[] args) {
		boolean overwrite = false;
		if(args.length == 0){
			args = new String[]{"E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\baseConfig.xml",
					"E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\pIds2removeForCaseStudies.txt"};
			overwrite = true;
		}
		if(args.length != LENGTH){
			log.error("expecting " + LENGTH + " arguments...");
			System.exit(-1);
		}
		OutputDirectoryLogging.catchLogEntries();
		
		log.info("running " + BuildingEnergyRunnerMain.class.getName() );
		String configFilename = args[0];
		String agents2exclude = args[1];
		log.info("configfile: " + configFilename);
		log.info("pIds 2 remove: " + agents2exclude);

		// initialize
		Config c = ConfigUtils.loadConfig(configFilename);
		Scenario sc = loadScenario(c, agents2exclude);
		// run
		Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting(
				overwrite ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.addControlerListener(new MyControlerListener(c, sc.getNetwork()));
		controler.run();
	}
	
	/**
	 * @param configFilename
	 * @param agents2exclude
	 * @return
	 */
	private static Scenario loadScenario(Config c, String agents2exclude) {
		if(agents2exclude == "null"){
			// no agents 2 exclude
			return ScenarioUtils.loadScenario(c);
		}else if(!new File(agents2exclude).exists()){
			throw new NullPointerException(agents2exclude + " does not exist.");
		}
		log.warn("removing agents");
		Scenario sc = ScenarioUtils.loadScenario(c);
		int removed = 0;
		for(Id<Person> id : getAgents2Exclude(agents2exclude)){
			if(sc.getPopulation().getPersons().remove(id) != null){
				removed++;
			}
		}
		log.warn("removed " + removed + " agents.");
		return sc;
	}

	/**
	 * @param agents2exclude
	 * @return
	 */
	private static Set<Id<Person>> getAgents2Exclude(String agents2exclude) {
		BufferedReader r = IOUtils.getBufferedReader(agents2exclude);
		Set<Id<Person>> ids = new HashSet<>();
		try {
			String line = r.readLine();
			while(line!=null){
				ids.add(Id.create(line,Person.class ));
				line = r.readLine();
			}
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ids;
	}

	// static class which plug the analysis to the services and generates some more output (distance- & tt-shares)
	private static class MyControlerListener implements IterationStartsListener,
												IterationEndsListener{
		
		private SimpleTripAnalyzer analyzer;
		
		MyControlerListener(Config c, Network net){
			analyzer =  new SimpleTripAnalyzer(c, net, null);
		}
		
		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			if(event.getIteration() == event.getServices().getConfig().controler().getLastIteration()){
				analyzer.reset(event.getIteration());
				event.getServices().getEvents().addHandler(analyzer);
			}
		}
		
		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			if(event.getIteration() == event.getServices().getConfig().controler().getLastIteration()){
                analyzer.run(event.getServices().getScenario().getPopulation());
				String path = event.getServices().getControlerIO().getOutputPath() + System.getProperty("file.separator");
				String prefix = event.getServices().getConfig().controler().getRunId() + ".";
				analyzer.dumpData(path, prefix);
				calcWriteDistanceDistribution(analyzer.getTraveller(), path + prefix + "distanceShare.csv.gz");
				calcAndWriteTTDistribution(analyzer.getTraveller(), path + prefix + "ttShare.csv.gz");
			}
		}
		
		/**
		 * @param traveller
		 */
		private void calcAndWriteTTDistribution(Map<Id<Person>, Traveller> traveller, String file) {
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
				for(Trip trip : t.getTrips()){
					Map<Integer, Integer> temp = getColumn(map, distribution, trip.getMode());
					increase(temp, trip.getDuration());
				}
			}
			dumpData(map, file);
		}
		
		/**
		 * @param traveller
		 */
		private void calcWriteDistanceDistribution(Map<Id<Person>, Traveller> traveller, String file) {
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
				for(Trip trip : t.getTrips()){
					Map<Integer, Integer> temp = getColumn(map, distribution, trip.getMode());
					increase(temp, trip.getDist());
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


