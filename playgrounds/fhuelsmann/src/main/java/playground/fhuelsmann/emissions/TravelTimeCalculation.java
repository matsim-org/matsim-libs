package playground.fhuelsmann.emissions;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

public class TravelTimeCalculation implements LinkEnterEventHandler,LinkLeaveEventHandler, 
AgentArrivalEventHandler,AgentDepartureEventHandler {

	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();
		
	public Map<String,Map<String, LinkedList<String>>> getTravelTimes() {
		return travelTimes;
	}

	private final Map<String,Map<String, LinkedList<String>>> travelTimes= new
	TreeMap<String,Map<String, LinkedList<String>>>();
	
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
		if (this.linkenter.containsKey(event.getPersonId())) {
			//mit Aktivität
			if (this.agentarrival.containsKey(personId)) {
				double enterTime = this.linkenter.get(personId);
				double arrivalTime = this.agentarrival.get(personId);
				double departureTime = this.agentdeparture.get(personId);
				double travelTime = event.getTime() - enterTime -departureTime+ arrivalTime;
			/*	System.out.println(travelTime); 
				System.out.println("----mit Aktivität---");
				System.out.println("LinkId: " + linkId); 
				System.out.println("PersonId: " + personId);
				System.out.println("entertime: " + enterTime);*/
				
				
				String personalId= personId.toString();
		//		System.out.println("TravelTime: " + travelTime);
				this.agentarrival.remove(personId);
				
				if (this.travelTimes.get(linkId+"") != null){				
				if (this.travelTimes.get(linkId+"").containsKey(personalId)){
						this.travelTimes.get(linkId+"").get(personalId+"").push("----mit Aktivität---,"+travelTime +","+  enterTime);
						}else{
							LinkedList<String> list = new LinkedList<String>();
							list.push("----mit Aktivität---,"+travelTime + ","+ enterTime);
							this.travelTimes.get(linkId+"").put(personalId+"",list);}}
				else{
					
							LinkedList<String> list = new LinkedList<String>();
							list.push("----mit Aktivität---,"+travelTime + ","+ enterTime);
							Map<String,LinkedList<String>> map = new TreeMap<String,LinkedList<String>>();
							map.put(personalId+"", list);
							this.travelTimes.put(linkId+"",map);
							
				}
			}
			else { // Ohne Aktivität
					
					
					double enterTime = this.linkenter.get(personId);
					double travelTime = event.getTime() - enterTime;
										
			//		System.out.println("----ohne Aktivität---");
			//		System.out.println("LinkId: " + linkId); 
			//		System.out.println("PersonId: " + personId);
					String personalId= personId.toString();
					//System.out.println(personalId);
			//		System.out.println("TravelTime: " + travelTime);
			//		System.out.println("entertime: " + enterTime);
					
					if (this.travelTimes.get(linkId+"") != null){
						if (this.travelTimes.get(linkId+"").containsKey(personalId)){
							this.travelTimes.get(linkId+"").get(personalId).push("----ohne Aktivität---,"+travelTime+","+ enterTime);
							}else{
								LinkedList<String> list = new LinkedList<String>();
								list.push("----ohne Aktivität---,"+travelTime + ","+ enterTime);
								this.travelTimes.get(linkId+"").put(personalId,list);}}
					else{
						
						LinkedList<String> list = new LinkedList<String>();
						list.push("----ohne Aktivität---,"+travelTime +","+ enterTime);
						Map<String,LinkedList<String>> map = new TreeMap<String,LinkedList<String>>();
						map.put(personalId, list);
						this.travelTimes.put(linkId +"",map);
						}				
					}
			}	
		}
	
					    
}
