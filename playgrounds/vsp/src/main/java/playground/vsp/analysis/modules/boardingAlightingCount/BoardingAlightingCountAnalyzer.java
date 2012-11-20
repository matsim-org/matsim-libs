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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.vsp.analysis.modules.AbstractAnalyisModule;

import com.vividsolutions.jts.geom.Polygon;

/**
 * @author droeder
 *
 */
public class BoardingAlightingCountAnalyzer extends AbstractAnalyisModule{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BoardingAlightingCountAnalyzer.class);
	private BoardAlightEventHandler handler;
	private Map<Id, Double> boardTotal;
	private Map<Id, Double> alightTotal;
	private Map<Id, TransitStopFacility> stops;
	private boolean writeHeatMaps = false;
//	private HashMap<String, HashMap<String, Polygon>> alightGeometries;
//	private double tileSize = Double.MAX_VALUE;

	public BoardingAlightingCountAnalyzer(Scenario sc, int interval) {
		super(BoardingAlightingCountAnalyzer.class.getSimpleName());
		this.handler = new BoardAlightEventHandler(interval);
		this.stops = sc.getTransitSchedule().getFacilities();
	}
	
	public void setWriteHeatMaps(boolean b, double heatMapTileSize){
		this.writeHeatMaps = b;
//		this.tileSize  = heatMapTileSize;
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
		this.createTotals();
		if(this.writeHeatMaps){
			createHeatMapGeometries();
		}
	}

	/**
	 * 
	 */
	private void createHeatMapGeometries() {
		//TODO[dr]
//		this.alightGeometries = new HashMap<String, HashMap<String, Polygon>>();
//		HeatMap heatmap;
//		//create for board
//		heatmap = new HeatMap(this.tileSize);
//		//total
		
		
		
	}

	/**
	 * 
	 */
	private void createTotals() {
		// count totals
		Double total;
		this.boardTotal = new HashMap<Id, Double>();
		for(Count c: this.handler.getBoarding().getCounts().values()){
			total = new Double(0.);
			for(Volume v: c.getVolumes().values()){
				total += v.getValue();
			}
			this.boardTotal.put(c.getLocId(), total);
		}
		
		this.alightTotal = new HashMap<Id, Double>();
		for(Count c: this.handler.getAlight().getCounts().values()){
			total = new Double(0.);
			for(Volume v: c.getVolumes().values()){
				total += v.getValue();
			}
			this.alightTotal.put(c.getLocId(), total);
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		writeCSV(outputFolder);
		if(this.writeHeatMaps){
			this.writeHeatMaps(outputFolder);
		}
	}

	/**
	 * @param outputFolder
	 */
	private void writeHeatMaps(String outputFolder) {
		// TODO[dr] Auto-generated method stub
		
	}

	/**
	 * @param outputFolder
	 */
	private void writeCSV(String outputFolder) {
		BufferedWriter w;
		String header = "id;x;y;board;alight;";
		//write totals
		w = IOUtils.getBufferedWriter(outputFolder + "total.csv");
		try {
			w.write(header + "\n");
			for(TransitStopFacility f: this.stops.values()){
				w.write(f.getId().toString() + ";");
				w.write(f.getCoord().getX() + ";");
				w.write(f.getCoord().getY() + ";");
				if(this.boardTotal.containsKey(f.getId())){
					w.write(this.boardTotal.get(f.getId()) + ";");
				}else{
					w.write(0. + ";");
				}
				if(this.alightTotal.containsKey(f.getId())){
					w.write(this.alightTotal.get(f.getId()) + ";");
				}else{
					w.write(0. + ";");
				}
				w.write("\n");
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//write timeSlices
		for(int i = 0 ; i < this.handler.getMaxTimeSlice() + 1; i++){
			w = IOUtils.getBufferedWriter(outputFolder + i +".csv");
			try {
				w.write(header + "\n");
				for(TransitStopFacility f: this.stops.values()){
					w.write(f.getId().toString() + ";");
					w.write(f.getCoord().getX() + ";");
					w.write(f.getCoord().getY() + ";");
					//boarding-value
					Count count = this.handler.getBoarding().getCounts().get(f.getId());
					if(count == null){
						w.write(0. + ";");
					}else{
						if(count.getVolume(i) == null){
							w.write(0. + ";");
						}else{
							w.write(String.valueOf(count.getVolume(i).getValue()) + ";");
						}
					}
					//boarding-value
					count = this.handler.getAlight().getCounts().get(f.getId());
					if(count == null){
						w.write(0. + ";");
					}else{
						if(count.getVolume(i) == null){
							w.write(0. + ";");
						}else{
							w.write(String.valueOf(count.getVolume(i).getValue()) + ";");
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
	}
}

