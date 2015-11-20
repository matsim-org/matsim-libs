package playground.wrashid.parkingSearch.ppSim.ttmatrix;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

// TODO: create test based on existing events file

// TODO: this approach could be improved: if too short bin size is used, it will allow cars to move with max. speed of road,
//which is not always correct, because if no car entered the road in a time bin, this does not mean the road itself is free. 

// also could create problems with long inflow roads (where tt longer than bin size, e.g. 1min)

// but probably 1min bin size should produce quite good results (which has been confirmed from berlin scenario)
public class TTMatrixFromEvents extends TTMatrix {

	public static void main(String[] args) {
	//	String eventsFile="C:/data/parkingSearch/psim/berlin/ITERS/it.50/50.events.xml.gz";
//		String eventsFile="C:/data/parkingSearch/psim/output/events.xml";
//		String networkFile="C:/data/parkingSearch/psim/berlin/output_network.xml.gz";
		String eventsFile="H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";
		//String eventsFile="C:/data/parkingSearch/psim/zurich/output/basic output with 300 sec bins/events.xml.gz";
		String networkFile="H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		TTMatrixFromEvents tTMatrixFromEvents=new TTMatrixFromEvents(24*3600,3600, eventsFile, networkFile);
//		tTMatrixFromEvents.writeTTMatrixToFile("C:/data/parkingSearch/psim/output/50.60secBin.ttMatrix_fromPSim.txt");
//		tTMatrixFromEvents.writeTTMatrixToFile("C:/data/parkingSearch/psim/berlin/ITERS/it.50/50.60secBin.ttMatrix.txt");
		tTMatrixFromEvents.writeTTMatrixToFile("C:/data/parkingSearch/psim/zurich/experiment/zurich eperiment 1/it.50.3600secBin.ttMatrix_fromPSim.txt");
	}
	
	TTMatrixFromEvents(int simulatedTimePeriod,int timeBinSizeInSeconds, String eventsFile, String networkFile) {
		this.simulatedTimePeriod = simulatedTimePeriod;
		network = GeneralLib.readNetwork(networkFile);
		this.timeBinSizeInSeconds = timeBinSizeInSeconds;

		linkTravelTimes = new HashMap<>();

		EventsManager events = EventsUtils.createEventsManager();

		TTMatrixTimesHandler tTMatrixTimesHandler = new TTMatrixTimesHandler(simulatedTimePeriod,timeBinSizeInSeconds,this);

		events.addHandler(tTMatrixTimesHandler);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		linkTravelTimes=tTMatrixTimesHandler.getLinkTravelTimes();
	}
	


	

	private class TTMatrixTimesHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, Wait2LinkEventHandler {

		private HashMap<Id<Link>, double[]> linkTravelTimes;
		private HashMap<Id, int[]> numberOfSamples;

		private HashMap<Id, Double> agentEnterLinkTime=new HashMap<Id, Double>();
		private TTMatrix ttMatrix;

		public TTMatrixTimesHandler( int simulatedTimePeriod, int timeBinSizeInSecond, TTMatrix ttMatrix) {
			this.ttMatrix = ttMatrix;
			linkTravelTimes=new HashMap<>();
			numberOfSamples=new HashMap<Id, int[]>();
		}
		
		public HashMap<Id<Link>, double[]> getLinkTravelTimes(){
			for (Id<Link> linkId:linkTravelTimes.keySet()){
				double[] ds = linkTravelTimes.get(linkId);
				Link link = network.getLinks().get(linkId);
				
				// reason for backward iteration is that in this case 
				// one can look at the value in past (if empty)
				// this is indeed an improvement for the quality (traffic peaks matching better)
				for (int i=ds.length-1;i>=0;i--){
					if (ds[i]==0){
						if (i>0 && i<ds.length-1){
							if (ds[i-1]==0){
								// proability increases that road speed should be close to freespeed
								double freeSpeedTravelTime=link.getLength() / link.getFreespeed();
								ds[i]=(ds[i+1]+freeSpeedTravelTime)/2;
							} else {
								ds[i]=(ds[i-1]+ds[i+1])/2;;
							}
						} else {
							ds[i]=link.getLength() / link.getFreespeed();
						}
					} else {
						ds[i]/=numberOfSamples.get(linkId)[i];
					}
					//System.out.println(link.getFreespeed() + " -> " + link.getLength()/ds[i]);
				}
			}
			return linkTravelTimes;
		}

		@Override
		public void reset(int iteration) {

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (agentEnterLinkTime.containsKey(event.getDriverId())) {
				int timeBinIndex = (int) (Math.round(GeneralLib.projectTimeWithin24Hours(agentEnterLinkTime.get(event.getDriverId()))) / ttMatrix.timeBinSizeInSeconds);

				double travelTime=event.getTime()-agentEnterLinkTime.get(event.getDriverId());
				
				if (!linkTravelTimes.containsKey(event.getLinkId())){
					int numberOfBins = ttMatrix.getNumberOfBins();
					linkTravelTimes.put(event.getLinkId(), new double[numberOfBins]);
					numberOfSamples.put(event.getLinkId(), new int[numberOfBins]);
				}
				double[] ds = linkTravelTimes.get(event.getLinkId());
				ds[timeBinIndex]+=travelTime;
				
				int[] ns = numberOfSamples.get(event.getLinkId());
				ns[timeBinIndex]++;
				
				agentEnterLinkTime.remove(event.getDriverId());
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			agentEnterLinkTime.put(event.getDriverId(), event.getTime());
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			agentEnterLinkTime.put(event.getPersonId(), event.getTime());
		}

	}

}
