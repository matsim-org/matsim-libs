package playground.wrashid.nan.extended;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

//the program is currently calculating the destination flows
//public class OutFlowInfoCollectorWithPt implements LinkEnterEventHandler,LinkLeaveEventHandler
public class OutFlowInfoCollectorWithPt implements LinkEnterEventHandler, PersonArrivalEventHandler
//public class OutFlowInfoCollectorWithPt implements AgentArrivalEventHandler
//public class OutFlowInfoCollectorWithPt implements LinkEnterEventHandler,LinkLeaveEventHandler,AgentArrivalEventHandler 
, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler
{
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;
	
	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id<Link>, int[]> linkOutFlow; // define
	private Map<Id<Link>, Link> filteredEquilNetLinks; // define personId, linkId
	private HashMap<Id<Person>, Id<Link>> lastEnteredLink=new HashMap<>(); // define
	private boolean isOldEventFile;
	private double networkLength=0;
	int setAggregationLevel=5; // do not forget to set the aggregation level!!!!!!

	public OutFlowInfoCollectorWithPt(Map<Id<Link>, Link> filteredEquilNetLinks,
			boolean isOldEventFile,int binSizeInSeconds) { // to create the class FlowInfoCollector
		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.isOldEventFile = isOldEventFile;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
		linkOutFlow = new HashMap<>();
	} // reset the variables (private
//												// ones)
//	public void handleEvent(LinkLeaveEvent event) {  
//		linkLeave(event.getLinkId(), event.getTime());
//	}
	

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		lastEnteredLink.put(driverId, event.getLinkId());
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId())!=null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())){
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getPersonId(),null); //reset value
			}
		}
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		if (lastEnteredLink.containsKey(driverId) && lastEnteredLink.get(driverId)!=null) {
			if (lastEnteredLink.get(driverId).equals(event.getLinkId())){
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(driverId,null); //reset value
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

	public void printLinkFlows() { // print
		
		for (Id linkId : linkOutFlow.keySet()) {
			
			Link link = filteredEquilNetLinks.get(linkId);
			System.out.print(linkId + " - " + link.getCoord() + ": ");
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
				System.out.print(flowAggregation[(i+1)/setAggregationLevel] + "\t");
				}
					//out flow calculation
			}
              
			System.out.println();
		}
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

	public HashMap<Id<Link>, int[]> getLinkOutFlow() {
		return linkOutFlow;
	}

	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

}
