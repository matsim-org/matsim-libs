package playground.jbischoff.BAsignals.analysis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

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
AgentArrivalEventHandler, AgentDepartureEventHandler, LaneEnterEventHandler {

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
	
	public double getAverageAdaptiveTravelTime(){
	if (this.getPassedAgents() == 0) return 0;
		Double att = 0.0;
		for (Entry<Id,Double> entry : ttmap.entrySet()){
			if (this.getPassedCars().contains(entry.getKey())){
				att += entry.getValue();
			}
		}
		att = att / this.getPassedAgents();
		return att;
	}
	
	public double getAverageTravelTime(){
		
		Double att = 0.0;
		for (Entry<Id,Double> entry : ttmap.entrySet()){
				att += entry.getValue();
		}
		att = att / ttmap.size();
		return att;
	}
}
