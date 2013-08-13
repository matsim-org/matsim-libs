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

package playground.vsp.analysis.modules.ptLines2PaxAnalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;

import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.AbstractAnalyisModule;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.CreateRscript;

/**
 * @author fuerbas after droeder
 *
 */

public class PtLines2PaxAnalysis extends AbstractAnalyisModule {

	private static final Logger log = Logger
			.getLogger(PtLines2PaxAnalysis.class);
	private PtLines2PaxAnalysisHandler handler;
	private Map<Id, TransitLine> lines;
	
	public PtLines2PaxAnalysis(Map<Id, TransitLine> lines, Vehicles vehicles, double interval, int maxSlices) {
		super(PtLines2PaxAnalysis.class.getSimpleName());
		this.handler = new PtLines2PaxAnalysisHandler(interval, maxSlices, lines, vehicles);
		this.lines = lines;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {

	}

	@Override
	public void postProcessData() {

	}

	@Override
	public void writeResults(String outputFolder) {
		String dir;
		for(TransitLines2PaxCounts tl2c: this.handler.getLinesPaxCounts().values()){
			dir = outputFolder + tl2c.getId().toString() + "--";
//			for "all longest" route = route with most stops write line files
			writeLineFiles(dir, tl2c, this.lines.get(tl2c.getId()));
		}
		CreateRscript.createScript(this.lines, outputFolder);
	}
	
//	works with simplistic example
	
	private void writeCounts2File(TransitLines2PaxCounts tl, Integer maxSlice, Counts counts, String file, String typeOfOutput) {
		BufferedWriter w = IOUtils.getBufferedWriter(file);
		Id stopId; 
		Count c;
		Volume v;
		try {
			//create header
			w.write("index;stopId;name");
			for(int i = 0; i < (maxSlice + 1); i++){
				w.write(";" + String.valueOf(i));
			}
			w.write("\n");
			// ...
			log.info("Number of TransitRoutes to be written for "+typeOfOutput+": "+tl.sortRoutesByNumberOfStops().size());
			for(int i = 0; i < tl.sortRoutesByNumberOfStops().size() ; i++){
				w.write("\n");
				TransitRoute tr = tl.sortRoutesByNumberOfStops().get(i);
				log.info("Writing output "+typeOfOutput+" for TransitRoute "+(i+1)+" of "+tl.sortRoutesByNumberOfStops().size()+
						" total routes, id = "+tr.getId()+" length = "+tr.getStops().size());
				for (int noStops = 0; noStops < tr.getStops().size(); noStops++) {	
					stopId = tr.getStops().get(noStops).getStopFacility().getId();
					c = counts.getCount(stopId);
					w.write(String.valueOf(noStops) + ";" + c.getCsId() + ";" + tr.getStops().get(noStops).getStopFacility().getName());
					for(int j = 0; j < (maxSlice + 1); j++){
						v = c.getVolume(j);
						String value = (v == null) ? "--" : String.valueOf(v.getValue());
						w.write(";" + value);
					}
					w.write("\n");
				}
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeLineFiles(String dir, TransitLines2PaxCounts tl2c, TransitLine tl) {
		writeCounts2File(tl2c, tl2c.getMaxSlice(), tl2c.getAlighting(), dir + tl2c.getId().toString() + "--alighting.csv", "alighting");
		writeCounts2File(tl2c, tl2c.getMaxSlice(), tl2c.getBoarding(), dir + tl2c.getId().toString() + "--boarding.csv", "boarding");
		writeCounts2File(tl2c, tl2c.getMaxSlice(), tl2c.getCapacity(), dir + tl2c.getId().toString() + "--capacity.csv", "capacity");
		writeCounts2File(tl2c, tl2c.getMaxSlice(), tl2c.getOccupancy(), dir + tl2c.getId().toString() + "--occupancy.csv", "occupancy");
		writeCounts2File(tl2c, tl2c.getMaxSlice(), tl2c.getTotalPax(), dir + tl2c.getId().toString() + "--totalPax.csv", "totalPax");
	}
	

	public static void main(String[] args) {
		String dir = "Z:\\WinHome\\workspace\\PtRoutes2PaxAna_Daten\\schedule\\";
		VspAnalyzer analyzer = new VspAnalyzer(dir, dir + "tut_10min_0.0.events.xml.gz");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
//		new TransitScheduleReader(sc).readFile(dir + "tut_10min_0.0.transitSchedule.xml.gz");
		new TransitScheduleReader(sc).readFile(dir + "tut_10min_0.0.transitSchedule_1.xml");	//for testing
		new VehicleReaderV1(((ScenarioImpl) sc).getVehicles()).readFile(dir + "tut_10min_0.0.vehicles.xml.gz");
		PtLines2PaxAnalysis ptLinesPax = new PtLines2PaxAnalysis(sc.getTransitSchedule().getTransitLines(), ((ScenarioImpl) sc).getVehicles(), 3600, 24);
		analyzer.addAnalysisModule(ptLinesPax);
		analyzer.run();
	}

}
