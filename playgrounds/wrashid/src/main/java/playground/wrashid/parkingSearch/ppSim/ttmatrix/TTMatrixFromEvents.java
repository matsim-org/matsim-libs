package playground.wrashid.parkingSearch.ppSim.ttmatrix;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

// TODO: create test based on existing events file
public class TTMatrixFromEvents implements TTMatrix {

	public static void main(String[] args) {
		String eventsFile="C:/data/parkingSearch/chessboard/output/ITERS/it.50/50.events.xml.gz";
		String networkFile="C:/data/parkingSearch/chessboard/output/output_network.xml.gz";
		TTMatrixFromEvents tTMatrixFromEvents=new TTMatrixFromEvents(900, eventsFile, networkFile);
	}
	
	private HashMap<Id, double[]> linkTravelTimes;

	private int timeBinSizeInSeconds;
	private static Network network;

	TTMatrixFromEvents(int timeBinSizeInSeconds, String eventsFile, String networkFile) {
		network = GeneralLib.readNetwork(networkFile);
		this.timeBinSizeInSeconds = timeBinSizeInSeconds;

		linkTravelTimes = new HashMap<Id, double[]>();

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		TTMatrixTimesHandler tTMatrixTimesHandler = new TTMatrixTimesHandler(timeBinSizeInSeconds);

		events.addHandler(tTMatrixTimesHandler);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		linkTravelTimes=tTMatrixTimesHandler.getLinkTravelTimes();
	}

	@Override
	public double getTravelTime(double time, Id linkId) {
		double travelTime;

		int timeBinIndex = (int) (Math.round(time) / timeBinSizeInSeconds);
		Link link = network.getLinks().get(linkId);

		if (!linkTravelTimes.containsKey(linkId) || linkTravelTimes.get(linkId)[timeBinIndex] == -1) {
			double minTravelTime = link.getLength() / link.getFreespeed();
			travelTime = minTravelTime;
		} else {
			travelTime = linkTravelTimes.get(linkId)[timeBinIndex];
		}

		return travelTime;
	}

	private static class TTMatrixTimesHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private HashMap<Id, double[]> linkTravelTimes;
		private HashMap<Id, int[]> numberOfSamples;
		private int timeBinSizeInSeconds;

		private HashMap<Id, Double> agentEnterLinkTime=new HashMap<Id, Double>();

		public TTMatrixTimesHandler(int timeBinSizeInSeconds) {
			this.timeBinSizeInSeconds = timeBinSizeInSeconds;
			linkTravelTimes=new HashMap<Id, double[]>();
			numberOfSamples=new HashMap<Id, int[]>();
		}
		
		public HashMap<Id, double[]> getLinkTravelTimes(){
			for (Id linkId:linkTravelTimes.keySet()){
				double[] ds = linkTravelTimes.get(linkId);
				
				for (int i=0;i<ds.length;i++){
					if (ds[i]==0){
						ds[i]=-1;
					} else {
						ds[i]/=numberOfSamples.get(linkId)[i];
						Link link = network.getLinks().get(linkId);
						System.out.println(link.getFreespeed() + " -> " + link.getLength()/ds[i]);
					}
				}
			}
			return linkTravelTimes;
		}

		@Override
		public void reset(int iteration) {

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (agentEnterLinkTime.containsKey(event.getPersonId())) {
				int timeBinIndex = (int) (Math.round(GeneralLib.projectTimeWithin24Hours(agentEnterLinkTime.get(event.getPersonId()))) / timeBinSizeInSeconds);

				double travelTime=event.getTime()-agentEnterLinkTime.get(event.getPersonId());
				
				if (!linkTravelTimes.containsKey(event.getLinkId())){
					int numberOfBins = 3600*24/timeBinSizeInSeconds + 1;
					linkTravelTimes.put(event.getLinkId(), new double[numberOfBins]);
					numberOfSamples.put(event.getLinkId(), new int[numberOfBins]);
				}
				double[] ds = linkTravelTimes.get(event.getLinkId());
				ds[timeBinIndex]+=travelTime;
				
				int[] ns = numberOfSamples.get(event.getLinkId());
				ns[timeBinIndex]++;
				
				agentEnterLinkTime.remove(event.getPersonId());
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			agentEnterLinkTime.put(event.getPersonId(), event.getTime());
		}

	}

}
