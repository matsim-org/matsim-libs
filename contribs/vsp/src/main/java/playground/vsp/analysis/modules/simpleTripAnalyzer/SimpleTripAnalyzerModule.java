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
package playground.vsp.analysis.modules.simpleTripAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * A simple analysis-class for a very basic MATSim-Scenario, i.e it should be used 
 * with physical simulation of car-trips only. All other modes must be teleported. Thus,
 * this class will throw a runtime-exception when {@link ScenarioConfigGroup#isUseTransit()} is true. 
 * 
 * @author droeder
 *
 */
public class SimpleTripAnalyzerModule extends AbstractAnalysisModule{

	private SimpleTripAnalyzer analyzer;
	private Population p;
	private Map<String, Map<Integer, Integer>> dist;
	private Map<String, Map<Integer, Integer>> tt;
	private String prefix;

	public SimpleTripAnalyzerModule(Config c, Network net, Population p, String prefix) {
		super("SimpleTripAnalyzer");
		this.analyzer = new SimpleTripAnalyzer(c, net, p.getPersons().keySet()); 
		this.p = p;
		this.prefix = prefix;
	}
	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(SimpleTripAnalyzerModule.class);

	

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> l = new ArrayList<EventHandler>();
		l.add(analyzer);
		return l;
	}

	@Override
	public void preProcessData() {
		analyzer.reset(-1);
	}

	@Override
	public void postProcessData() {
		analyzer.run(p);
		this.dist = calcWriteDistanceDistribution(analyzer.getTraveller());
		this.tt = calcAndWriteTTDistribution(analyzer.getTraveller());
	}

	@Override
	public void writeResults(String outputFolder) {
		String prefix = (this.prefix == null) ? "" : (this.prefix + ".");
		analyzer.dumpData(new File(outputFolder).getAbsolutePath() + System.getProperty("file.separator"), prefix);
		dumpData(this.dist, new File(outputFolder).getAbsolutePath() + System.getProperty("file.separator") + prefix + "distanceShare.csv.gz");
		dumpData(this.tt, new File(outputFolder).getAbsolutePath() + System.getProperty("file.separator") + prefix + "ttShare.csv.gz");
	}
	
	/**
	 * @param traveller
	 */
	public static Map<String, Map<Integer, Integer>> calcAndWriteTTDistribution(Map<Id<Person>, Traveller> traveller) {
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
		return map;
	}
	
	public static Map<String, Map<Integer, Integer>> calcWriteDistanceDistribution(Map<Id<Person>, Traveller> traveller) {
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
		return map;
	}
	
	/**
	 * @param map
	 * @param file
	 * @return 
	 */
	public static void dumpData(Map<String, Map<Integer, Integer>> map, String file) {
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
	private static void increase(Map<Integer, Integer> temp, Double value) {
		for(Integer i : temp.keySet()){
			if(value <= i){
				temp.put(i, temp.get(i) + 1);
				return;
			}
		}
	}
	
	
	
	private static Map<Integer, Integer> getColumn(Map<String, Map<Integer,Integer>> map, List<Integer> distribution, String mode){
		if(map.containsKey(mode)) return map.get(mode);
		Map<Integer, Integer> temp = new LinkedHashMap<Integer, Integer>();
		for(Integer i : distribution){
			temp.put(i, 0);
		}
		map.put(mode, temp);
		return temp;
	}

}

