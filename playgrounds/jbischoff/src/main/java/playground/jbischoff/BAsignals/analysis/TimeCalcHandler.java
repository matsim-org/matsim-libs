package playground.jbischoff.BAsignals.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;

import playground.jbischoff.BAsignals.model.AdaptiveControllHead;

public class TimeCalcHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
AgentArrivalEventHandler, AgentDepartureEventHandler, LaneEnterEventHandler  {
	private static final Logger log = Logger.getLogger(TimeCalcHandler.class);

	private Map<Id,Double> arrivaltimestofbspn;
	private Map<Id,Double> arrivaltimestofbcb;

	private Map<Id,Double> arrivaltimesfromfbspn;
	private Map<Id,Double> arrivaltimesfromfbcb;
	private Map<Id,Double> ttmap;
	private AdaptiveControllHead ach;
	private Set<Id> carsPassed;
	private Set<Id> wannabeadaptiveLanes;

	
public TimeCalcHandler(AdaptiveControllHead ach){
	this.ach=ach;
	this.ttmap = new TreeMap<Id,Double>();
	this.carsPassed = new HashSet<Id>();
	this.wannabeadaptiveLanes = new HashSet<Id>();
	this.fillWannaBes();
	this.arrivaltimesfromfbcb = new TreeMap<Id,Double>();
	this.arrivaltimesfromfbspn = new TreeMap<Id,Double>();
	this.arrivaltimestofbcb = new TreeMap<Id,Double>();
	this.arrivaltimestofbspn = new TreeMap<Id,Double>();
	this.arrivaltimesfromfbcb.put(new IdImpl(0),0.0);
	this.arrivaltimestofbspn.put(new IdImpl(0),0.0);
	this.arrivaltimestofbcb.put(new IdImpl(0),0.0);
	this.arrivaltimesfromfbspn.put(new IdImpl(0),0.0);


	
}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		this.ttmap.put(event.getPersonId(), agentTt - event.getTime()) ;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		this.ttmap.put(event.getPersonId(), agentTt + event.getTime()) ;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		
		
		this.ttmap.put(event.getPersonId(), agentTt + event.getTime()) ;	
		if (event.getPersonId().toString().endsWith("SPN_SDF")){
			Double tr = this.arrivaltimestofbspn.get(event.getPersonId());
			
			if (tr == null){
			this.arrivaltimestofbspn.put(event.getPersonId(), event.getTime());	
			}
			else{
				this.arrivaltimesfromfbspn.put(event.getPersonId(), event.getTime());	

			}
		}
			if (event.getPersonId().toString().endsWith("CB_SDF")){
				Double tr = this.arrivaltimestofbcb.get(event.getPersonId());
				if (tr == null){
				this.arrivaltimestofbcb.put(event.getPersonId(), event.getTime());	
				}
				else{
					this.arrivaltimesfromfbcb.put(event.getPersonId(), event.getTime());	

				}
			}
		
		

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Double agentTt = this.ttmap.get(event.getPersonId());
		if (agentTt == null){
			this.ttmap.put(event.getPersonId(), 0-event.getTime()) ;		

		} else {
			this.ttmap.put(event.getPersonId(), agentTt - event.getTime()) ;		

		}
		
		
	}
	public Map<Id, Double> getTtmap() {
		return ttmap;
	}
	
	private void fillWannaBes(){
		//mock up adaptive lanes to create comparable travel times LSA-SLV
		for (int i = 2100; i<2113; i++){ //Signalsystem 17
				this.wannabeadaptiveLanes.add(new IdImpl(i));
		}
		for (int i = 2000; i<2013; i++){ //Signalsystem 18
			this.wannabeadaptiveLanes.add(new IdImpl(i));
		}
		for (int i = 1900; i<1913; i++){ //Signalsystem 1
			this.wannabeadaptiveLanes.add(new IdImpl(i));
		}
		
		
		
		
	}
	@Override
	public void handleEvent(LaneEnterEvent event) {
//		if (this.ach.laneIsAdaptive(event.getLaneId()) & (!event.getLaneId().toString().endsWith(".ol")))
//		 actually the nicer way
		
		if (this.wannabeadaptiveLanes.contains(event.getLaneId()))
		this.carsPassed.add(event.getPersonId());

	}
	public long getPassedAgents() {
		return this.carsPassed.size();
	}

	public Set<Id> getPassedCars() {
		return carsPassed;
	}
	
	
	public void exportArrivalTime(int iteration,String outdir){
		String filename = outdir+iteration+".arrivalTimesFromFB_CB.csv";
		this.exportMaptoCVS(this.arrivaltimesfromfbcb,  filename);
		filename = outdir+iteration+".arrivalTimesFromFB_SPN.csv";
		this.exportMaptoCVS(this.arrivaltimesfromfbspn,  filename);
		filename = outdir+iteration+".arrivalTimesToFB_CB.csv";
		this.exportMaptoCVS(this.arrivaltimestofbcb,  filename);
		filename = outdir+iteration+".arrivalTimesToSPN_CB.csv";
		this.exportMaptoCVS(this.arrivaltimestofbspn,  filename);
		filename = outdir+iteration+".latestArrivals.csv";

		this.exportLatestArrivals(filename);
	}
	
	private void exportLatestArrivals(String filename) {
		try {
			FileWriter writer = new FileWriter(filename);
			writer.append(Collections.max(this.arrivaltimestofbcb.values())+";"+Collections.max(this.arrivaltimestofbspn.values())+";"+Collections.max(this.arrivaltimesfromfbcb.values())+";"+Collections.max(this.arrivaltimesfromfbspn.values())+";\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void exportMaptoCVS(Map<Id, Double> atmap,  String filename) {
		try {
			FileWriter writer = new FileWriter(filename);
		
		for (Entry<Id,Double> e : atmap.entrySet()){
		writer.append(e.getKey().toString()+";"+e.getValue()+";"+"\n");	
		}
		writer.flush();
		writer.close();
		log.info("Wrote "+filename);
		} catch (IOException e1) {
			log.error("cannot write to file: "+filename);
			e1.printStackTrace();
		}
	
		
		
	}
	
	public double getLatestArrivalCBSDF(){
		return Collections.max(this.arrivaltimestofbcb.values()).doubleValue();
		
	}	
	public double getLatestArrivalSPNSDF(){
		return Collections.max(this.arrivaltimestofbspn.values()).doubleValue();
		
	}
	public double getLatestArrivalSDFCB(){
		return Collections.max(this.arrivaltimesfromfbcb.values()).doubleValue();
		
	}
	public double getLatestArrivalSDFSPN(){
		return Collections.max(this.arrivaltimesfromfbspn.values()).doubleValue();
		
	}
	
	public int getAverageTravelTime(){
		
		Double att = 0.0;
		for (Entry<Id,Double> entry : ttmap.entrySet()){
				att += entry.getValue();
		}
		att = att / ttmap.size();
		return att.intValue();
	}
	public int getAverageAdaptiveTravelTime(){
		if (this.getPassedAgents() == 0) return 0;
			Double att = 0.0;
			for (Entry<Id,Double> entry : ttmap.entrySet()){
				if (this.getPassedCars().contains(entry.getKey())){
					att += entry.getValue();
				}
			}
			att = att / this.getPassedAgents();
			return att.intValue();
			

			
		}
}
