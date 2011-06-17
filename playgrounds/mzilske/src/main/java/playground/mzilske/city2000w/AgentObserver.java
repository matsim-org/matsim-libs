package playground.mzilske.city2000w;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.mzilske.freight.FreightConstants;

public class AgentObserver implements ActivityEndEventHandler, LinkEnterEventHandler, ActivityStartEventHandler{

	public static class CostElements {
		private Id personId;
		
		private double time;
		
		private double distance;

		public CostElements(Id personId, double time, double distance) {
			super();
			this.personId = personId;
			this.time = time;
			this.distance = distance;
		}

		Id getPersonId() {
			return personId;
		}

		double getTime() {
			return time;
		}

		double getDistance() {
			return distance;
		}
	}
	
	private static Logger logger = Logger.getLogger(AgentObserver.class);
	
	private List<List<CostElements>> costCollector = new ArrayList<List<CostElements>>();
	
	private Network network;
	
	private Map<Id, Double> startTimePerPerson = new HashMap<Id, Double>();
	
	private Map<Id, Double> timeOnTheRoadPerAgent = new HashMap<Id, Double>();
	
	private Map<Id, Double> distanceOnTheRoadPerAgent = new HashMap<Id, Double>();
	
	private String outFile;
	
	
	
	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

	public AgentObserver(String caseStudyConfDescription, Network network) {
		super();
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		List<CostElements> elements = new ArrayList<CostElements>();
		Set<Id> ids = timeOnTheRoadPerAgent.keySet();
		for(Id id : ids){		
			double time = timeOnTheRoadPerAgent.get(id);
			double distance = 0.0;
			if(distanceOnTheRoadPerAgent.containsKey(id)){
				distance = distanceOnTheRoadPerAgent.get(id);
			}
			logger.info(id + " distance=" + distance + " time=" + time);
			elements.add(new CostElements(id,distance,time));
		}
		costCollector.add(elements);
		distanceOnTheRoadPerAgent.clear();
		timeOnTheRoadPerAgent.clear();
		startTimePerPerson.clear();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(FreightConstants.END.equals(event.getActType())){
			double timeOnTheRoad = event.getTime() - startTimePerPerson.get(event.getPersonId());
			if(timeOnTheRoadPerAgent.containsKey(event.getPersonId())){
				double time = timeOnTheRoadPerAgent.get(event.getPersonId());
				double newTime = time + timeOnTheRoad;
				timeOnTheRoadPerAgent.put(event.getPersonId(), newTime);
			}
			else{
				timeOnTheRoadPerAgent.put(event.getPersonId(), timeOnTheRoad);
			}
		}
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double linkDist = network.getLinks().get(event.getLinkId()).getLength();
		if(distanceOnTheRoadPerAgent.containsKey(event.getPersonId())){
			double distance = distanceOnTheRoadPerAgent.get(event.getPersonId());
			double newDistance = distance + linkDist;
			distanceOnTheRoadPerAgent.put(event.getPersonId(), newDistance);
		}
		else{
			distanceOnTheRoadPerAgent.put(event.getPersonId(), linkDist);
		}
		
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(FreightConstants.START.equals(event.getActType())){
			startTimePerPerson.put(event.getPersonId(), event.getTime());
		}
		
	}
	
	public void writeStats(){
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		int iteration = 0;
		try{
			for(List<CostElements> elements : costCollector){
				for(CostElements e : elements){

					writer.write(iteration + ";" + e.personId + ";" + e.getDistance() + ";" + e.getTime());
					writer.write("\n");


				}
				iteration++;
			}
			writer.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
