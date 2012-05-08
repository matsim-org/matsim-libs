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
package playground.droeder.eMobility.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.api.experimental.events.handler.GenericEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.eMobility.analysis.StateStore.StateEntry;
import playground.droeder.eMobility.events.SoCChangeEvent;

/**
 * @author droeder
 *
 */
public class SoCEventHandler implements GenericEventHandler{
	private static final Logger log = Logger.getLogger(SoCEventHandler.class);
	
	private static String INPUTDIR = "D:/VSP/svn/shared/volkswagen_internal/matsimOutput/test/";
	private static String OUTPUTDIR = INPUTDIR + "ITERS/it.0/charts/";
	private static String NETWORK = INPUTDIR + "output_network.xml.gz";
	private static String EVENTS = INPUTDIR + "ITERS/it.0/0.events.xml.gz";
	private StateStore stateStore;
	private Network net;
	private Map<Id, Id> lastTravelledLink;

	public SoCEventHandler(Network net){
		this.net = net;
		this.stateStore = new StateStore();
		this.lastTravelledLink = new HashMap<Id, Id>();
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(GenericEvent event) {
		SoCChangeEvent e;
		if(event instanceof SoCChangeEvent){
			e = (SoCChangeEvent) event;
//			stateStore.addValue(e.getVehId(), e.getSoC(), e.getTime(), this.net.getLinks().get(e.getLinkId()).getLength());
		}else if(event.getAttributes().get("type").equals(SoCChangeEvent.TYPE)){
			e = new SoCChangeEvent(event);
		}else{
			return;
		}
		Double travelledDistance;
		if(!this.lastTravelledLink.containsKey(e.getVehId())){
			travelledDistance = this.net.getLinks().get(e.getLinkId()).getLength();
		}else{
			if(this.lastTravelledLink.get(e.getVehId()).equals(e.getLinkId())){
				travelledDistance = 0.;
			}else{
				travelledDistance = this.net.getLinks().get(e.getLinkId()).getLength();
			}
		}
		this.stateStore.addValue(e.getVehId(), e.getSoC(), e.getTime(), travelledDistance);
		this.lastTravelledLink.put(e.getVehId(), e.getLinkId());
	}
	
	public void dumpData(String directory){
		log.info("dumping data to " + directory + "...");
		if(!new File(directory).exists()){
			new File(directory).mkdirs();
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(directory + "stateChange.txt");
		XYLineChart timeChart ;
		XYLineChart distChart ;
		
		try {
			for(StateEntry s: this.stateStore.getEntries()){
				timeChart = new XYLineChart("TimeDependend SoC-Change", "time [s]", "SoC [kWh]");
				distChart = new XYLineChart("DistanceDependend SoC-Change", "distance [m]", "SoC [kWh]");;
				writer.write(s.getId().toString() + "\n");
				writer.write("time:\t");
				for(Double d: s.getTime()){
					writer.write(d + "\t");
				}
				writer.newLine();
				writer.write("dist:\t");
				for(Double d: s.getDist()){
					writer.write(d + "\t");
				}
				writer.newLine();
				writer.write("SoC:\t");
				for(Double d: s.getSoC()){
					writer.write(d + "\t");
				}
				writer.newLine();
				
				timeChart.addSeries(s.getId().toString(), toArray(s.getTime()), toArray(s.getSoC()));
				timeChart.saveAsPng(directory + s.getId() + "_time2SoCChart.png", 1920, 1200);
				distChart.addSeries(s.getId().toString(), toArray(s.getDist()), toArray(s.getSoC()));
				distChart.saveAsPng(directory + s.getId() + "dist2SoCChart.png", 1920, 1200);
			}
			
			
			writer.flush();
			writer.close();
			log.info("finished...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private double[] toArray(Double[] d){
		double[] dd = new double[d.length];
		
		for(int i = 0; i < d.length; i++){
			dd[i] = d[i];
		}
		return dd;
	}
	
	public static void main(String[] args){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(NETWORK);
		EventsManager manager = EventsUtils.createEventsManager();
		SoCEventHandler handler = new SoCEventHandler(sc.getNetwork());
		manager.addHandler(handler);
		
		new MatsimEventsReader(manager).readFile(EVENTS);
		handler.dumpData(OUTPUTDIR);
		
	}

}

class StateStore{
	
	private HashMap<Id, StateEntry> store;


	public StateStore(){
		this.store =  new HashMap<Id, StateEntry>();
	}
	
	
	public void addValue(Id vehId, double soc, double time, double additionalDistance){
		if(!this.store.containsKey(vehId)){
			this.store.put(vehId, new StateEntry(vehId));
		}
		this.store.get(vehId).addValue(soc, time, additionalDistance);
	}
	
	public Collection<StateEntry> getEntries(){
		return this.store.values();
	}

	class StateEntry{
		
		List<Double> soc, dist, time;
		Id id;
		
		public StateEntry(Id id){
			this.soc = new ArrayList<Double>();
			this.dist = new ArrayList<Double>();
			this.time = new ArrayList<Double>();
			this.id = id;
		}
		
		public Id getId(){
			return this.id;
		}

		public void addValue(double soc, double time, double additionalDistance) {
			this.soc.add(soc);
			this.time.add(time);
			if(this.dist.size() > 0){
				this.dist.add(this.dist.get(this.dist.size() -1 ) + additionalDistance);
			}else{
				this.dist.add(additionalDistance);
			}
		}
		
		public Double[] getSoC(){
			return this.soc.toArray(new Double[this.soc.size()]);
		}
		
		public Double[] getDist(){
			return this.dist.toArray(new Double[this.soc.size()]);
		}
		
		public Double[] getTime(){
			return this.time.toArray(new Double[this.soc.size()]);
		}
		
	}
}


