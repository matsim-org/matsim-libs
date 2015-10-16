package playground.wrashid.brawand.demand;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import playground.wrashid.bodenbender.StudyArea;

/**
 * write out first non-stationary position of agents after
 * 'simulationStartTimeInSeconds' and last position before
 * 'simulationEndTimeInSeconds' furthermore specify, if last position was for
 * arrival event or not.
 * 
 * @author wrashid
 * 
 */
public class TravelDemand {

	public static void main(String[] args) {

		String eventsFile = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/ITERS/it.50/50.events.xml.gz";

		EventsManager events = (EventsManager) EventsUtils
				.createEventsManager();

		CollectPositionOfVehicles collectPositionOfVehicles = new CollectPositionOfVehicles();

		events.addHandler(collectPositionOfVehicles);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		Network network = GeneralLib
				.readNetwork("H:/data/experiments/TRBAug2011/runs/ktiRun24/output/output_network.xml.gz");

		ArrayList<String> homeFileContent = new ArrayList<String>();
		ArrayList<String> workFileContent = new ArrayList<String>();

		StringBuffer tmpStringBuffer = new StringBuffer();

		homeFileContent.add("linkId\tdemandCount\tx\ty");
		workFileContent.add("linkId\tdemandCount\tx\ty");

		for (Id linkId : collectPositionOfVehicles.getNumberOfHomeActsPerLink()
				.getKeySet()) {
			tmpStringBuffer.append(linkId);
			tmpStringBuffer.append("\t");
			tmpStringBuffer.append(collectPositionOfVehicles
					.getNumberOfHomeActsPerLink().get(linkId));
			tmpStringBuffer.append("\t");
			tmpStringBuffer.append(network.getLinks().get(linkId).getCoord()
					.getX());
			tmpStringBuffer.append("\t");
			tmpStringBuffer.append(network.getLinks().get(linkId).getCoord()
					.getY());
			homeFileContent.add(tmpStringBuffer.toString());
			tmpStringBuffer = new StringBuffer();
		}

		for (Id linkId : collectPositionOfVehicles.getNumberOfWorkActsPerLink()
				.getKeySet()) {
			tmpStringBuffer.append(linkId);
			tmpStringBuffer.append("\t");
			tmpStringBuffer.append(collectPositionOfVehicles
					.getNumberOfWorkActsPerLink().get(linkId));
			tmpStringBuffer.append("\t");
			tmpStringBuffer.append(network.getLinks().get(linkId).getCoord()
					.getX());
			tmpStringBuffer.append("\t");
			tmpStringBuffer.append(network.getLinks().get(linkId).getCoord()
					.getY());
			homeFileContent.add(tmpStringBuffer.toString());
			tmpStringBuffer = new StringBuffer();
		}

		GeneralLib.writeList(homeFileContent, "c:/tmp/homeDemand.txt");
		GeneralLib.writeList(workFileContent, "c:/tmp/workDemand.txt");

	}

	private static class CollectPositionOfVehicles implements
			ActivityEndEventHandler {

		private IntegerValueHashMap<Id> numberOfHomeActsPerLink = new IntegerValueHashMap<Id>();
		private IntegerValueHashMap<Id> numberOfWorkActsPerLink = new IntegerValueHashMap<Id>();

		@Override
		public void reset(int iteration) {

		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (event.getActType().equalsIgnoreCase("home")) {
				getNumberOfHomeActsPerLink().increment(event.getLinkId());
			}

			if (event.getActType().equalsIgnoreCase("work_sector2") || event.getActType().equalsIgnoreCase("work_sector3")) {
				getNumberOfWorkActsPerLink().increment(event.getLinkId());
			}
		}

		public IntegerValueHashMap<Id> getNumberOfHomeActsPerLink() {
			return numberOfHomeActsPerLink;
		}

		public IntegerValueHashMap<Id> getNumberOfWorkActsPerLink() {
			return numberOfWorkActsPerLink;
		}

	}

}
