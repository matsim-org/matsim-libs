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
package playground.vsp.analysis.modules.ptTripAnalysis;


/**
 * @author aneumann, droeder
 *
 */
public class BvgTripAnalysisRunnerV4 {
//			extends AbstractAnalyisModule{
//	
//	private final static Logger log = Logger.getLogger(BvgTripAnalysisRunnerV4.class);
//
//	private ScenarioImpl scenario;
//	private TTtripAnalysisV4 ana;
//	
//	public BvgTripAnalysisRunnerV4(String ptDriverPrefix){
//		super(BvgTripAnalysisRunnerV4.class.getSimpleName(), ptDriverPrefix);
//		log.info("enabled");
//	}
//	
//	public void init(ScenarioImpl scenario, Set<Feature> shapeFile) {
//		this.scenario = scenario;
//		this.ana = new TTtripAnalysisV4(scenario.getConfig().transit().getTransitModes(), scenario.getConfig().plansCalcRoute().getNetworkModes());
//
//		Geometry g =  (Geometry) shapeFile.iterator().next().getAttribute(0);
//		Map<String, Geometry> zones =  new HashMap<String, Geometry>();
//		zones.put("Berlin", g);
//		this.ana.addZones(zones);
//	}
//
//	@Override
//	public List<EventHandler> getEventHandler() {
//		List<EventHandler> handler = new LinkedList<EventHandler>();
//		handler.add(this.ana.getEventHandler());
//		return handler;
//	}
//
//	@Override
//	public void preProcessData() {
//		this.ana.preProcessData(this.scenario.getPopulation());
//	}
//
//	@Override
//	public void postProcessData() {
//		// nothing to do here
//	}
//
//	@Override
//	public void writeResults(String outputFolder) {
//		this.ana.writeResults(outputFolder);
//	}

}