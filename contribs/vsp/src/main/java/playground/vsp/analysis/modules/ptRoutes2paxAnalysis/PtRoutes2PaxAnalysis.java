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
package playground.vsp.analysis.modules.ptRoutes2paxAnalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
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
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author droeder
 *
 */
public class PtRoutes2PaxAnalysis extends AbstractAnalysisModule {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtRoutes2PaxAnalysis.class);
	private PtRoutes2PaxAnalysisHandler handler;
	private Map<Id<TransitLine>, TransitLine> lines;
	private double interval;
	private int maxSlices;

	public PtRoutes2PaxAnalysis(Map<Id<TransitLine>, TransitLine> lines, Vehicles vehicles, double interval, int maxSlices) {
		super(PtRoutes2PaxAnalysis.class.getSimpleName());
		this.handler = new PtRoutes2PaxAnalysisHandler(interval, maxSlices, lines, vehicles);
		this.lines = lines;
		this.interval = interval;
		this.maxSlices = maxSlices;
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new ArrayList<EventHandler>();
		handler.add(this.handler);
		return handler;
	}

	@Override
	public void preProcessData() {
		// do nothing
	}

	@Override
	public void postProcessData() {
		// do nothing
	}

	@Override
	public void writeResults(String outputFolder) {
		
		String dir;
		for(TransitLineContainer lc: this.handler.getTransitLinesContainer().values()){
			// put all routes of one line into an separat folder as an routes id is not necessarily unique
			dir = outputFolder + lc.getId().toString() + "--";
//			File f = new File(dir);
//			if(!f.exists()) f.mkdirs();
			//parse the routes
			for(TransitRouteContainer rc : lc.getTransitRouteContainer().values()){
				writeRouteFiles(dir, rc, this.lines.get(lc.getId()).getRoutes().get(rc.getId()));
			}
		}
		CreateRscript.createScript(this.lines, outputFolder, interval, maxSlices);
	}

	/**
	 * @param dir
	 * @param rc
	 * @param transitRoute 
	 */
	private void writeRouteFiles(String dir, TransitRouteContainer rc, TransitRoute transitRoute) {
		writeCounts2File(transitRoute, rc.getMaxSlice(), rc.getAlighting(), dir + rc.getId().toString() + "--alighting.csv");
		writeCounts2File(transitRoute, rc.getMaxSlice(), rc.getBoarding(), dir + rc.getId().toString() + "--boarding.csv");
		writeCounts2File(transitRoute, rc.getMaxSlice(), rc.getCapacity(), dir + rc.getId().toString() + "--capacity.csv");
		writeCounts2File(transitRoute, rc.getMaxSlice(), rc.getOccupancy(), dir + rc.getId().toString() + "--occupancy.csv");
		writeCounts2File(transitRoute, rc.getMaxSlice(), rc.getTotalPax(), dir + rc.getId().toString() + "--totalPax.csv");
	}

	/**
	 * @param transitRoute 
	 * @param maxSlice 
	 * @param alighting
	 * @param string
	 */
	private void writeCounts2File(TransitRoute transitRoute, Integer maxSlice, Counts counts, String file) {
		BufferedWriter w = IOUtils.getBufferedWriter(file);
		Id<Link> stopId; 
		Count c;
		Volume v;
		try {
			//create header
			w.write("index;stopId;name");
			DecimalFormat rFormat = new DecimalFormat("00");
			for(int i = 0; i < maxSlices; i++){
				int begin = (int) (i * interval);
				//Interval begin, eg. "H08M30" : hour 8, minute 30
				String beginString = "H" + rFormat.format(begin / (60 * 60)) + "M" + rFormat.format((begin / 60) % 60);
				int end = (int) ((i + 1) * interval);
				//Interval end
				String endString = "H" + rFormat.format(end / (60 * 60)) + "M" + rFormat.format((end / 60) % 60);
				w.write("; " + beginString + "_to_" + endString);
			}
			// measurements after the last slice
			int begin = (int) (maxSlices * interval);
			String beginString = "H" + rFormat.format(begin / (60 * 60)) + "M" + rFormat.format((begin / 60) % 60);
			w.write("; " + beginString);
			
			w.write("\n");
			// write numbers for stops in the correct order
			for(int i = 0; i < transitRoute.getStops().size(); i++){
				stopId = Id.create(i, Link.class);
				c = counts.getCount(stopId);
				w.write(String.valueOf(i) + ";" + c.getCsLabel() + ";" + transitRoute.getStops().get(i).getStopFacility().getName());
				for(int j = 0; j < (maxSlice + 1); j++){
					v = c.getVolume(j+1);
					String value = (v == null) ? "--" : String.valueOf(v.getValue());
					w.write(";" + value);
				}
				w.write("\n");
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String dir = "E:\\VSP\\svn\\droeder\\southAfrica\\testReRoute\\testReRoute3Old\\";
		VspAnalyzer analyzer = new VspAnalyzer(dir, dir + "ITERS\\it.300\\testReRoute3Old.300.events.xml.gz");
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(sc).readFile(dir + "ITERS\\it.299\\testReRoute3Old.299.transitSchedule.xml.gz");
		new VehicleReaderV1(((MutableScenario) sc).getTransitVehicles()).readFile(dir + "ITERS\\it.299\\testReRoute3Old.299.vehicles.xml.gz");
		PtRoutes2PaxAnalysis ptRoutesPax = new PtRoutes2PaxAnalysis(sc.getTransitSchedule().getTransitLines(), ((MutableScenario) sc).getTransitVehicles(), 3600, 24);
		analyzer.addAnalysisModule(ptRoutesPax);
		analyzer.run();
		
		
	}
}

