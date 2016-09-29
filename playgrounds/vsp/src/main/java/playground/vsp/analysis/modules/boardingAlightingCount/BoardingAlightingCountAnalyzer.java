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
package playground.vsp.analysis.modules.boardingAlightingCount;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.utils.heatMap.HeatMap;

/**
 * @author droeder
 *
 */
public class BoardingAlightingCountAnalyzer extends AbstractAnalysisModule{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BoardingAlightingCountAnalyzer.class);
	private BoardAlightEventHandler handler;
//	private Map<Id, Double> boardUnclassifiedTotal;
//	private Map<Id, Double> alightUnclassifiedTotal;
//	private Map<Id, Double> boardStartTotal;
//	private Map<Id, Double> alightEndTotal;
//	private Map<Id, Double> boardSwitchSTotal;
//	private Map<Id, Double> alightSwitchTotal;
	
	private Map<Id<TransitStopFacility>, TransitStopFacility> stops;
	private boolean writeHeatMaps = false;
	private SortedMap<String, HeatMap> heatMaps;
	private Integer gridSize = Integer.MAX_VALUE;
	private SortedMap<String, Map<Id, Double>> totals;
	private final String targetCoordinateSystem;

	/**
	 * Counts number of boarding and alighting per stop/interval. and differs between boardings 
	 * at the beginning or in the middle of a trip and alightings in the middle or at the end of trip.
	 * 
	 * @param sc, the scenario containing the transitStops
	 * @param interval, interval-size in seconds
	 */
	public BoardingAlightingCountAnalyzer(Scenario sc, int interval, String targetCoordinateSystem) {
		super(BoardingAlightingCountAnalyzer.class.getSimpleName());
		this.handler = new BoardAlightEventHandler(interval);
		this.stops = sc.getTransitSchedule().getFacilities();
		this.targetCoordinateSystem = targetCoordinateSystem;
	}
	
	/**
	 * a simple heatmap will be created for the total counts with a number of @heatMapgridSize at the longer side.
	 * @param b
	 * @param heatMapGridSize
	 */
	public void setWriteHeatMaps(boolean b, int heatMapGridSize){
		this.writeHeatMaps = b;
		this.gridSize  = heatMapGridSize;
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
		if(this.writeHeatMaps){
			this.heatMaps = new TreeMap<String, HeatMap>();
		}
		this.totals = new TreeMap<String, Map<Id, Double>>();
		for(Entry<String, Counts> e: this.handler.getClassification2Counts().entrySet()){
			totals.put(e.getKey() + "Total", this.createTotals(e.getValue(), e.getKey()));
		}
	}

	/**
	 * 
	 */
	private Map<Id, Double> createTotals(Counts<Link> counts, String name) {
		// count totals
		Map<Id, Double> totals = new HashMap<Id, Double>();
		Double total;
		for(Count<Link> c: counts.getCounts().values()){
			total = new Double(0.);
			for(Volume v: c.getVolumes().values()){
				total += v.getValue();
			}
			totals.put(c.getId(), total);
		}
		if(this.writeHeatMaps){
			log.warn("Writing heatmaps seems to be broken for a long time. This function is deactivated for the time beeing.");
//			createHeatMapTotals(totals, name + "Totals");
		}
		return totals;
	}
	
	/**
	 * 
	 */
	private void createHeatMapTotals(Map<Id, Double> totals, String name) {
		HeatMap heatmap;
		//create for boarding all
		heatmap = new HeatMap(this.gridSize);
		for(Entry<Id, Double> e: totals.entrySet()){
			if (this.stops.get(e.getKey()) == null) {
				log.info("No entries for type " + e.getKey());
			} else {
				Coord coord = this.stops.get(e.getKey()).getCoord();
				heatmap.addValue(coord, e.getValue());
			}
		}
		heatmap.createHeatMap();
		this.heatMaps.put(name, heatmap);
	}
	

	@Override
	public void writeResults(String outputFolder) {
		writeCSV(outputFolder);
		if(this.writeHeatMaps){
			this.writeHeatMaps(outputFolder, this.targetCoordinateSystem);
		}
	}

	/**
	 * @param outputFolder
	 */
	private void writeHeatMaps(String outputFolder, String targetCoordinateSystem) {
		for(Entry<String, HeatMap> e: this.heatMaps.entrySet()){
			HeatMap.writeHeatMapShape(e.getKey(), e.getValue(), outputFolder + e.getKey() + ".shp", targetCoordinateSystem);
		}
	}

	/**
	 * @param outputFolder
	 */
	private void writeCSV(String outputFolder) {
		BufferedWriter w;
		String header = "id;x;y;";
		for(String s: this.totals.keySet()){
			header = header + s + ";";
		}
		//write totals
		w = IOUtils.getBufferedWriter(outputFolder + "total.csv");
		try {
			w.write(header + "\n");
			for(TransitStopFacility f: this.stops.values()){
				w.write(f.getId().toString() + ";");
				w.write(f.getCoord().getX() + ";");
				w.write(f.getCoord().getY() + ";");
				for(Map<Id, Double> total: this.totals.values()){
					if(total.containsKey(f.getId())){
						w.write(total.get(f.getId()) + ";");
					}else{
						w.write(0. + ";");
					}	
				}
				w.write("\n");
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//write timeSlices
		header = "id;x;y;";
		for(String s: this.handler.getClassification2Counts().keySet()){
			header = header + s + ";";
		}
		for(int i = 0 ; i < this.handler.getMaxTimeSlice() + 1; i++){
			w = IOUtils.getBufferedWriter(outputFolder + i +".csv");
			try {
				w.write(header + "\n");
				for(TransitStopFacility f: this.stops.values()){
					w.write(f.getId().toString() + ";");
					w.write(f.getCoord().getX() + ";");
					w.write(f.getCoord().getY() + ";");
					//boarding-value
					for(Counts counts: this.handler.getClassification2Counts().values()){
						Count count = counts.getCount(Id.create(f.getId(), Link.class));
						if(count == null){
							w.write(0. + ";");
						}else{
							if(count.getVolume(i+1) == null){
								w.write(0. + ";");
							}else{
								w.write(String.valueOf(count.getVolume(i+1).getValue()) + ";");
							}
						}
					}
					w.write("\n");
				}
				w.flush();
				w.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// write departures for stops
		w = IOUtils.getBufferedWriter(outputFolder + "stop2vehicleDepartures.csv");
		try {
			w.write("id;x;y;departures"); w.newLine();
			TransitStopFacility f;
			for(Entry<Id, Double> e: this.handler.getStopToDepartures().entrySet()){
				f = this.stops.get(e.getKey());
				if (f.getCoord() == null) {
					log.info("Stop " + f.getId() + " - " + f.getName() + " has no coordinates associated with. Will ignore this stop and proceed.");
				} else {
					w.write(e.getKey() + ";" + f.getCoord().getX() + ";" + f.getCoord().getY() + ";" + e.getValue().toString()); w.newLine();
				}
			}
			w.flush();
			w.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}

