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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;

import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.ptRoutes2paxAnalysis.CreateRscript;

/**
 * @author aneumann, fuerbas after droeder
 *
 */

public class PtLines2PaxAnalysis extends AbstractAnalysisModule {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PtLines2PaxAnalysis.class);
	private final String SEP = "--";
	private PtLines2PaxAnalysisHandler handler;
	private double interval;
	private int maxSlices;
	
	public PtLines2PaxAnalysis(Map<Id<TransitLine>, TransitLine> lines, Vehicles vehicles, double interval, int maxSlices) {
		super(PtLines2PaxAnalysis.class.getSimpleName());
		this.handler = new PtLines2PaxAnalysisHandler(interval, maxSlices, lines, vehicles);
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
		// nothing to do here
	}

	@Override
	public void postProcessData() {
		// nothing to do here
	}

	@Override
	public void writeResults(String outputFolder) {
		String dir = outputFolder;
		for(TransitLines2PaxCounts tl2c: this.handler.getLinesPaxCounts().values()){
			writeLineFiles(dir, tl2c);
		}
		CreateRscript.createScriptFromTransitLines2PaxCounts(this.handler.getLinesPaxCounts(), dir, interval, maxSlices);
	}
	
	private void writeCounts2File(List<TransitRouteStop> transitRouteStops, int maxSlice, Counts counts, String outputFilename) {
		BufferedWriter w = IOUtils.getBufferedWriter(outputFilename);
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
			
			for (int noStops = 0; noStops < transitRouteStops.size(); noStops++) {	
				Id stopId = transitRouteStops.get(noStops).getStopFacility().getId();
				Count count = counts.getCount(stopId);
				w.write(String.valueOf(noStops) + ";" + count.getCsLabel() + ";" + transitRouteStops.get(noStops).getStopFacility().getName());
				
				for(int j = 0; j <= maxSlice; j++){
					Volume volume = count.getVolume(j+1);
					String value = (volume == null) ? this.SEP : String.valueOf(volume.getValue());
					w.write(";" + value);
				}
				w.write("\n");
			}
			
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeLineFiles(String dir, TransitLines2PaxCounts tl2c) {
		for(int i = 0; i < tl2c.getRouteList().size() ; i++){
			List<TransitRouteStop> transitRouteStops = tl2c.getRouteList().get(i).getStops();
			int maxSlice = tl2c.getMaxSlice().intValue();
			String filePrefix = dir + tl2c.getId() + this.SEP + tl2c.getRouteList().get(i).getId().toString();
			writeCounts2File(transitRouteStops, maxSlice, tl2c.getAlighting(), filePrefix + this.SEP + "alighting.csv");
			writeCounts2File(transitRouteStops, maxSlice, tl2c.getBoarding(), filePrefix + this.SEP + "boarding.csv");
			writeCounts2File(transitRouteStops, maxSlice, tl2c.getCapacity(), filePrefix + this.SEP + "capacity.csv");
			writeCounts2File(transitRouteStops, maxSlice, tl2c.getOccupancy(), filePrefix + this.SEP + "occupancy.csv");
			writeCounts2File(transitRouteStops, maxSlice, tl2c.getTotalPax(), filePrefix + this.SEP + "totalPax.csv");
		}
	}

	public static void main(String[] args) {
//		String dir = "Z:\\WinHome\\workspace\\PtRoutes2PaxAna_Daten\\schedule\\";
		String dir = "D:\\runs-svn\\intermodal\\r5-w1000\\";
		VspAnalyzer analyzer = new VspAnalyzer(dir, dir + "r5-w1000.output_events.xml.gz");
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(sc).readFile(dir + "r5-w1000.output_transitSchedule.xml.gz");
//		new TransitScheduleReader(sc).readFile(dir + "tut_10min_0.0.transitSchedule_1.xml");	//for testing
		new VehicleReaderV1(((MutableScenario) sc).getTransitVehicles()).readFile(dir + "r5-w1000.output_transitVehicles.xml.gz");
		PtLines2PaxAnalysis ptLinesPax = new PtLines2PaxAnalysis(sc.getTransitSchedule().getTransitLines(), ((MutableScenario) sc).getTransitVehicles(), 3600, 24);
		analyzer.addAnalysisModule(ptLinesPax);
		analyzer.run();
	}
}
