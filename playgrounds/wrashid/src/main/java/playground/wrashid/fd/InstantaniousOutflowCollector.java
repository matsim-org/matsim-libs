package playground.wrashid.fd;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;


public class InstantaniousOutflowCollector implements LinkLeaveEventHandler,
		PersonArrivalEventHandler {

	private int binSizeInSeconds; // set the length of interval
	private HashMap<Id, int[]> linkOutFlow; // define
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks; // define

	// linkId
	private DoubleValueHashMap<Id> lastLinkLeaveTime = new DoubleValueHashMap<Id>(); // define
	private HashMap<Id, LinkedList<InterVehicleInterval>> leaveEvents = new HashMap<Id, LinkedList<InterVehicleInterval>>();
	private IntegerValueHashMap<Id> numberOfVehiclesLeavingLinkAtSameTime=new IntegerValueHashMap<Id>();
	private DoubleValueHashMap<Id> timeOfLastVehicleClustering=new DoubleValueHashMap<Id>();
	
	
	public InstantaniousOutflowCollector(
			Map<Id<Link>, ? extends Link> filteredEquilNetLinks, int binSizeInSeconds) { // to
																					// create
																					// the
																					// class
																					// FlowInfoCollector
																					// and
																					// give
																					// the
																					// link
																					// set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) { // call from
		// NetworkReadExample
		linkLeave(event.getLinkId(), event.getTime());
	}

	private void linkLeave(Id linkId, double time) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (lastLinkLeaveTime.get(linkId) == 0) {
			lastLinkLeaveTime.put(linkId, time);
			return;
		} else {
			if (!leaveEvents.containsKey(linkId)) {
				leaveEvents
						.put(linkId,
								new LinkedList<InstantaniousOutflowCollector.InterVehicleInterval>());
			}

			double lastLinkLeaveTimeInSec = lastLinkLeaveTime.get(linkId);
			if (time < 86400) {
				
				if(time - lastLinkLeaveTimeInSec==0){
					numberOfVehiclesLeavingLinkAtSameTime.increment(linkId);
					
					if (timeOfLastVehicleClustering.get(linkId)!=0){
						timeOfLastVehicleClustering.put(linkId, time);
					}
					
					return;
				} else {
					if (numberOfVehiclesLeavingLinkAtSameTime.get(linkId)!=0){
						
						if (linkId.toString().equalsIgnoreCase("20")){
							System.out.println(3600*numberOfVehiclesLeavingLinkAtSameTime.get(linkId) + "\t" + timeOfLastVehicleClustering.get(linkId) + "\t*");
						}
						
						leaveEvents.get(linkId).add(
								new InterVehicleInterval(3600*numberOfVehiclesLeavingLinkAtSameTime.get(linkId),
										timeOfLastVehicleClustering.get(linkId)));
						numberOfVehiclesLeavingLinkAtSameTime.set(linkId, 0);
						
						timeOfLastVehicleClustering.put(linkId, 0.0);
						
					} 
				}
				
				
				
				
				leaveEvents.get(linkId).add(
						new InterVehicleInterval(3600/(time - lastLinkLeaveTimeInSec),
								time));
				
				if (linkId.toString().equalsIgnoreCase("20")){
					System.out.println(3600/(time - lastLinkLeaveTimeInSec) + "\t" + time);
				}
				
				lastLinkLeaveTime.put(linkId, time);
			}
		}

	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		if (linkOutFlow!=null){
			return linkOutFlow;
		}
		
		linkOutFlow = new HashMap<Id, int[]>();
		HashMap<Id, int[]> numberOfSamples = new HashMap<Id, int[]>();

		for (Id linkId : leaveEvents.keySet()) {
			linkOutFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
			numberOfSamples
					.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);

			int[] bins = linkOutFlow.get(linkId);
			int[] sampleBins = numberOfSamples.get(linkId);

			for (InterVehicleInterval ivi : leaveEvents.get(linkId)) {
				int binIndex = (int) Math.round(Math
						.floor(ivi.endTimeOfInterval / binSizeInSeconds));

				if (ivi.endTimeOfInterval < 86400) {
					bins[binIndex] += ivi.flow;
					sampleBins[binIndex]++;
				}
			}
		}

		for (Id linkId : leaveEvents.keySet()) {
			int[] bins = linkOutFlow.get(linkId);
			int[] sampleBins = numberOfSamples.get(linkId);
			for (int i = 0; i < bins.length; i++) {
				if (sampleBins[i]==0){
					bins[i] =0;
				}else {
					bins[i] /= sampleBins[i];
				}
				
			}
		}

		return linkOutFlow;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		linkLeave(event.getLinkId(), event.getTime());
	}

	private class InterVehicleInterval {
		public InterVehicleInterval(double flow, double time) {
			super();
			this.flow = flow;
			this.endTimeOfInterval = time;
		}

		private double flow;
		private double endTimeOfInterval;

	}

}
