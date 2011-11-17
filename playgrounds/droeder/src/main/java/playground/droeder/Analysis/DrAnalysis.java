/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.droeder.Analysis.handler.NetworkAnalysisHandler;
import playground.droeder.Analysis.handler.PopulationAnalysisHandler;
import playground.droeder.gis.DaShapeWriter;
import playground.kai.analysis.CalcLegTimes;

/**
 * @author droeder
 *
 */
public class DrAnalysis {
	private static final Logger log = Logger.getLogger(DrAnalysis.class);
	private static final String PLANS = ".plans.xml.gz";
	private static final String EVENTS = ".events.xml.gz";
	
	private Scenario sc;
	private String outputDir;
	private String iteration;

	private Map<Id, SortedMap<String, Double>> numAgentsByLinkAndSlice;
	/**
	 * if iteration == null the last iteration in the config is used
	 * @param configFile
	 * @param iteration
	 */
	public DrAnalysis(String configFile, Integer iteration){
		Config c = ConfigUtils.loadConfig(configFile);
		if(iteration == null){
			this.iteration = "ITERS/it." + String.valueOf(c.controler().getLastIteration()) + "/" +
					String.valueOf(c.controler().getLastIteration());
		}else{
			this.iteration = "ITERS/it." + String.valueOf(iteration) + "/" + String.valueOf(iteration);
		}
		this.outputDir = c.controler().getOutputDirectory();
		String plans = getFileDir(this.PLANS);
		c.plans().setInputFile(plans);
		this.sc = ScenarioUtils.loadScenario(c);
	}
	
	public String getOutDir(){
		return this.outputDir;
	}
	
	public Network getNet(){
		return this.sc.getNetwork();
	}
	
	/**
	 * @return
	 */
	private String getFileDir(String type) {
		String file = this.outputDir + this.iteration + type;
		if(!new File(file).exists()){
			try {
				throw new FileNotFoundException(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
	
	public void run(){
		String events = getFileDir(this.EVENTS);
		EventsManager manager = EventsUtils.createEventsManager();
		
		CalcLegTimes calcLegTimesKai = new CalcLegTimes(this.sc);
		manager.addHandler(calcLegTimesKai);

		org.matsim.analysis.CalcLegTimes calcLegTimes = new org.matsim.analysis.CalcLegTimes();
		manager.addHandler(calcLegTimes);

		NetworkAnalysisHandler netAnalysis = new NetworkAnalysisHandler(this.sc.getNetwork(), 3600, this.sc.getConfig().getQSimConfigGroup().getFlowCapFactor());
		manager.addHandler(netAnalysis);
		
		PopulationAnalysisHandler popAnalysis =  new PopulationAnalysisHandler();
		manager.addHandler(popAnalysis);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(manager);
		reader.parse(events);
		
		calcLegTimesKai.writeStats(this.outputDir + "legTimesKai.txt");
		calcLegTimes.writeStats(this.outputDir + "legTimes.txt");
		this.numAgentsByLinkAndSlice = netAnalysis.getNumAgentsByLinkAndSlice();
		netAnalysis.dumpCsv(this.outputDir);
		netAnalysis.dumpShp(this.outputDir, this.sc.getNetwork());
		popAnalysis.dumpCsv(this.outputDir, this.sc.getConfig().getQSimConfigGroup().getFlowCapFactor());
	}
	
	public Map<Id, SortedMap<String, Double>> getNumAgentsByLinkAndSlice(){
		return this.numAgentsByLinkAndSlice;
	}
	
	public static void diffNetShp(Map<Id, SortedMap<String, Double>> base, Network baseNet, Map<Id, SortedMap<String, Double>> plan, Network planNet, String outFile){
		Map<Id, Link> links = new HashMap<Id, Link>();
		Map<Id, SortedMap<String, Object>> diffNet = new HashMap<Id, SortedMap<String,Object>>();
		SortedMap<String, Object> temp;
		double baseV, planV;
		for(Link l: baseNet.getLinks().values()){
			links.put(l.getId(), l);
			temp = new TreeMap<String, Object>();
			if(base.containsKey(l.getId())){
				baseV = base.get(l.getId()).get("absolute");
			}else{
				baseV = 0.;
			}
			if(plan.containsKey(l.getId())){
				planV = plan.get(l.getId()).get("absolute");
			}else{
				planV = 0.;
			}
			temp.put("plan-base", (planV - baseV));
			diffNet.put(l.getId(), temp);
		}
		
		for(Link l : planNet.getLinks().values()){
			if(!links.containsKey(l.getId())){
				links.put(l.getId(), l);
				links.put(l.getId(), l);
				temp = new TreeMap<String, Object>();
				if(base.containsKey(l.getId())){
					baseV = base.get(l.getId()).get("absolute");
				}else{
					baseV = 0.;
				}
				if(plan.containsKey(l.getId())){
					planV = plan.get(l.getId()).get("absolute");
				}else{
					planV = 0.;
				}
				temp.put("plan-base", (planV - baseV));
				diffNet.put(l.getId(), temp);
			}
		}
		DaShapeWriter.writeLinks2Shape(outFile, links, diffNet);
	}
	
}
