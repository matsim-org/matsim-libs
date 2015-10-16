package playground.wrashid.jin;

import java.util.ArrayList;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class NumberOfVehiclesEnteringAndExitingArea {

	public static void main(String[] args) {
		String networkFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz";
		String eventsFile =  "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";
		EventsManager events = EventsUtils.createEventsManager();
		
		Network network = GeneralLib.readNetwork(networkFile);
		
		AreaHandler handler = new AreaHandler(network);
		events.addHandler(handler);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);

		reader.parse(eventsFile);
		
		handler.printOutput("C:\\tmp\\New folder\\Area1_60SecBin.txt");
	}
	
	
	private static class AreaHandler implements ActivityEndEventHandler, LinkEnterEventHandler {

		int binSizeInSeconds=60;
		int arraySize=200*60*60/binSizeInSeconds;
		double[] inFlow=new double[arraySize];
		double[] outFlow=new double[arraySize];
		
		private Network network;
		HashSet<Id> vehicleInArea=new HashSet<Id>();
		ArrayList<String> output=new ArrayList<String>();

		public AreaHandler(Network network){
			this.network=network;
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
		public void printOutput(String fileName){
			int maxInFlowIndex=inFlow.length-1;
			int maxOutFlowIndex=outFlow.length-1;

			while (inFlow[maxInFlowIndex]!=0){
				maxInFlowIndex--;
			}
			
			while (outFlow[maxOutFlowIndex]!=0){
				maxOutFlowIndex--;
			}
			
			output.add("timeBin\tInfow\tOutflow");
			
			for (int i=0;i<=Math.max(maxInFlowIndex, maxOutFlowIndex);i++){
				output.add(i + "\t" + inFlow[i] + "\t" + outFlow[i]);
			}
			
			GeneralLib.writeList(output, fileName);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Coord linkCoord=network.getLinks().get(event.getLinkId()).getCoord();
			
			// leaving area
			if (vehicleInArea.contains(event.getDriverId()) && !isInArea(linkCoord)){
				outFlow[(int)Math.round(event.getTime()/binSizeInSeconds)]++;
				//output.add(Math.round(event.getTime()) + "\t" + "-1");
			}
			
			// entering area
			if (!vehicleInArea.contains(event.getDriverId()) && isInArea(linkCoord)){
				inFlow[(int)Math.round(event.getTime()/binSizeInSeconds)]++;
				//output.add(Math.round(event.getTime()) + "\t" + "+1");
			}
			
			// update vehicleInArea
			if (isInArea(linkCoord)){
				vehicleInArea.add(event.getDriverId());
			} else {
				vehicleInArea.remove(event.getDriverId());
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			Coord linkCoord=network.getLinks().get(event.getLinkId()).getCoord();
			if (isInArea(linkCoord)){
				vehicleInArea.add(event.getPersonId());
			} else {
				vehicleInArea.remove(event.getPersonId());
			}
		}
	}
	
	
	public static boolean isInArea(Coord coord){
		Coord circleCenter= new Coord(683243.7, 247459.2);
		double radius=700;
		
//		Coord circleCenter=new CoordImpl(682922.588,247474.957);
//		double radius=298;
		
		if (GeneralLib.getDistance(coord, circleCenter)<radius){
			return true;
		}
		
		return false;
	}
	
}
