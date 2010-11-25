package playground.fhuelsmann.emission;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;




public class DataStructureOfSingleEventAttributes implements LinkEnterEventHandler,LinkLeaveEventHandler, 
AgentArrivalEventHandler,AgentDepartureEventHandler {
	
		
	private final Network network;
	
	
	public DataStructureOfSingleEventAttributes(final Network network) {
			this.network = network;
	}
	
	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();
	
		
	//LinkID as key --> PersonID as key -->Elemente sind in einer Liste gespeichert, wie z.B. SingleEvent 
	public Map<Id,Map<Id, LinkedList<SingleEvent>>> getTravelTimes() {
		return travelTimes;
	}
	
	private final Map<Id,Map<Id, LinkedList<SingleEvent>>> travelTimes= new
	TreeMap<Id,Map<Id, LinkedList<SingleEvent>>>();
	
	public Map<Id, Double> getLinkenter() {
		return linkenter;
	}

	public Map<Id, Double> getAgentarrival() {
		return agentarrival;
	}

	public Map<Id, Double> getAgentdeparture() {
		return agentdeparture;
	}


	public void reset(int iteration) {
		
		this.linkenter.clear();
		this.agentarrival.clear();
		this.agentdeparture.clear();
		System.out.println("reset...");
	}

	public void handleEvent(LinkEnterEvent event) {
		this.linkenter.put(event.getPersonId(), event.getTime());
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		this.agentarrival.put(event.getPersonId(), event.getTime());
	}
	
	public void handleEvent(AgentDepartureEvent event) {
		this.agentdeparture.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {	
	
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();
		
		//get attributes of the network per link
		Link link = this.network.getLinks().get(linkId);
		double distance = link.getLength();
//		int roadType = link.getType();
		int roadType = 55;
		int freeVelocity = (int) link.getFreespeed();
		
		if (this.linkenter.containsKey(event.getPersonId())) {						
			//with activity
			if (this.agentarrival.containsKey(personId)) {
				
				double enterTime = this.linkenter.get(personId);
				double arrivalTime = this.agentarrival.get(personId);
				double departureTime = this.agentdeparture.get(personId);

				double travelTime = event.getTime() - enterTime -departureTime+ arrivalTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);
				System.out.print("freespeed: " + averageSpeed);
				
				this.agentarrival.remove(personId);
				
				//Data structure is built --> Map(Map(linkedList); 
				//to avoid overriding the elements of the same linkId, if there are two events with the same LinkId the new elements are also saved 
				if (this.travelTimes.get(linkId) != null){				
					//adds the Objects of the already existing LinkId to the List
					if (this.travelTimes.get(linkId).containsKey(personId)){
						
						SingleEvent tempSingleEvent = new SingleEvent("----mit Aktivität---" , travelTime+"",averageSpeed, 
		 						personId+"",distance,roadType,enterTime+"", freeVelocity,linkId+"");
						
						
						this.travelTimes.get(linkId).get(personId).push(tempSingleEvent);
						}
					//List must be created when there is no PersonID
					else{
							LinkedList<SingleEvent> list = new LinkedList<SingleEvent>();
							SingleEvent tempSingleEvent = new SingleEvent("----mit Aktivität---" , travelTime+"",averageSpeed, 
			 						personId+"",distance,roadType,enterTime+"", freeVelocity, linkId+"");
							
							list.push(tempSingleEvent);
							this.travelTimes.get(linkId+"").put(personId,list);
							}
					}
				
				else{
					
					LinkedList<SingleEvent> list = new LinkedList<SingleEvent>();
							
					SingleEvent tempSingleEvent = new SingleEvent("----mit Aktivität---" , travelTime+"",averageSpeed, 
			 						personId+"",distance,roadType,enterTime+"", freeVelocity, linkId+"");
							
					list.push(tempSingleEvent);
					Map<Id,LinkedList<SingleEvent>> map = new TreeMap<Id,LinkedList<SingleEvent>>();
							
					map.put(personId, list);
					this.travelTimes.put(linkId,map);
							
					}	
				}	
			
			else { // without activity
					
				
				double enterTime = this.linkenter.get(personId);
				double travelTime = event.getTime() - enterTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				
				if (this.travelTimes.get(linkId) != null){
					
			
					if (this.travelTimes.get(linkId).containsKey(personId)){
 
						SingleEvent tempSingleEvent = new SingleEvent("----ohne Aktivität---" , travelTime+"",averageSpeed, 
		 						personId+"",distance,roadType,enterTime+"", freeVelocity, linkId+"");
						
						this.travelTimes.get(linkId+"").get(personId).push(tempSingleEvent);
						}
					
					//List must be created when there is no PersonID
					else{
							
							LinkedList<SingleEvent> list = new LinkedList<SingleEvent>();
						
							SingleEvent tempSingleEvent = new SingleEvent("----ohne Aktivität---" , travelTime+"",averageSpeed, 
			 						personId+"",distance,roadType,enterTime+"", freeVelocity,linkId+"");
							
							list.push(tempSingleEvent);
							this.travelTimes.get(linkId).put(personId,list);
							}
					}
		
				else{
						
						
						LinkedList<SingleEvent> list = new LinkedList<SingleEvent>();
							
						SingleEvent tempSingleEvent = new SingleEvent("----ohne Aktivität---" , travelTime+"",averageSpeed, 
			 						personId+"",distance,roadType,enterTime+"", freeVelocity, linkId+"");
							
						list.push(tempSingleEvent);
						Map<Id,LinkedList<SingleEvent>> map = new TreeMap<Id,LinkedList<SingleEvent>>();
						map.put(personId, list);
						this.travelTimes.put(linkId,map);
						}
					}
			}
	}
}

