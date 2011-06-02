package playground.sergioo.Events;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;

//the program is currently calculating the destination flows
//public class OutFlowInfoCollectorWithPt implements LinkEnterEventHandler,LinkLeaveEventHandler
public class OutFlowInfoCollectorWithPt implements LinkEnterEventHandler, AgentArrivalEventHandler
//public class OutFlowInfoCollectorWithPt implements AgentArrivalEventHandler
//public class OutFlowInfoCollectorWithPt implements LinkEnterEventHandler,LinkLeaveEventHandler,AgentArrivalEventHandler 
{
	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id, int[]> linkOutFlow; // define
	private Map<Id, Link> filteredEquilNetLinks; // define personId, linkId
	private HashMap<Id, Id> lastEnteredLink=new HashMap<Id, Id>(); // define
	private boolean isOldEventFile;
	private double networkLength=0;
	int setAggregationLevel=5; // do not forget to set the aggregation level!!!!!!

	public OutFlowInfoCollectorWithPt(Map<Id, Link> filteredEquilNetLinks,
			boolean isOldEventFile,int binSizeInSeconds) { // to create the class FlowInfoCollector
		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.isOldEventFile = isOldEventFile;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	public void reset(int iteration) {linkOutFlow = new HashMap<Id, int[]>();} // reset the variables (private
//												// ones)
//	public void handleEvent(LinkLeaveEvent event) {  
//		linkLeave(event.getLinkId(), event.getTime());
//	}
	

	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getPersonId(), event.getLinkId());
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId())!=null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())){
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getPersonId(),null); //reset value
			}
		}
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId())!=null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())){
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getPersonId(),null); //reset value
			}
		}
	}
	
//	public void handleEvent(AgentArrivalEvent event) {
//		linkLeave(event.getLinkId(), event.getTime());}

	private void linkLeave(Id linkId, double time) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {return;} // if the link is not in the link set, then exit the method
		if (!linkOutFlow.containsKey(linkId)) {
			linkOutFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]); // set the number of intervals
		}
		int[] bins = linkOutFlow.get(linkId);
		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));
		if (time < 86400) {
			bins[binIndex] = bins[binIndex] + 1; // count the number of agents
													// for each link at each time interval
		}
	}

	public void printLinkFlows() throws FileNotFoundException { // print
		PrintWriter writer = new PrintWriter(new File("./data/youssef/resFlow.txt"));
		for (Id linkId : linkOutFlow.keySet()) {
			
			Link link = filteredEquilNetLinks.get(linkId);
			writer.print(linkId + " - " + link.getCoord() + ": ");
            int[] bins = linkOutFlow.get(linkId);
            int[] binsTwo = new int[bins.length];
            double[] flowAggregation = new double[(binsTwo.length)/setAggregationLevel+1]; 
                        
			for (int i = 0; i < bins.length; i++) {
			    binsTwo[i]=bins[i]*3600/binSizeInSeconds;
			    double sumAggregation=0;
				if((i+1)%setAggregationLevel==0&(!(i==0))){  
					for(int j=1;j<setAggregationLevel;j++){
					sumAggregation=sumAggregation+binsTwo[i+1-setAggregationLevel+j];
				    }
				flowAggregation[(i+1)/setAggregationLevel]= sumAggregation/setAggregationLevel;
				writer.print(flowAggregation[(i+1)/setAggregationLevel] + "\t");
				}
					//out flow calculation
			}
              
			writer.println();
		}
		writer.close();
	}
	
//	public void printLinkWeights() { // print
//		for (Id linkId:linkOutFlow.keySet()){
//			
//			Link link = filteredEquilNetLinks.get(linkId);;
//			networkLength = networkLength+link.getNumberOfLanes()*link.getLength(); //calculate total network length
//			}
//				
//		for (Id linkId : linkOutFlow.keySet()) {
//			
//			Link link = filteredEquilNetLinks.get(linkId);
//			System.out.print(linkId + " - " + link.getCoord() + ": "+link.getLength());
//			int[] bins = linkOutFlow.get(linkId);
//            for (int j=1;j<((bins.length)/setAggregationLevel+1);j++){
//            System.out.print((link.getLength())*(link.getNumberOfLanes()) + "\t"); 
//            // for later compute the weighted flow. link length over network length. 
//            }
//            
//			System.out.println();
//		}
//	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return linkOutFlow;
	}

}
