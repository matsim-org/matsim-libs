package playground.wrashid.parkingChoice.trb2011.analysis;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.SortableMapObject;

public class LinkVolumeAnalyzer {

	public static void main(String[] args) {
		final String networkFileName = "H:/data/experiments/TRBAug2011/matsim/network.multimodal-wu.xml.gz";
		final String eventsFileName = "H:/data/experiments/TRBAug2011/runs/run1/output/ITERS/it.150/herbie.150.events.xml.gz";
		final String plansFileName = "H:/data/experiments/TRBAug2011/herbie/plans_1pct.xml.gz";
		final String facilitiesFileName = "H:/data/experiments/TRBAug2011/herbie/facilitiesWFreight.xml.gz";

		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();

		NetworkImpl network = GeneralLib.readNetwork(networkFileName);

		VolumesAnalyzer volumeAnalyzer = new VolumesAnalyzer(3600, 24 * 3600 - 1, network);
		PeakHourAgents peakHourAgents=new PeakHourAgents();
		eventsManager.addHandler(volumeAnalyzer);
		eventsManager.addHandler(peakHourAgents);
		

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);

		// peak defined as times, where there are more than 600'000 vehicles on
		// the road at the same time
		// for the given scenario. these are the intervalls between 7-9 and
		// 15-19.5

		PriorityQueue<SortableMapObject> priorityQueue = new PriorityQueue<SortableMapObject>();

		int count = 0;
		for (Id linkId : volumeAnalyzer.getLinkIds()) {
			double[] volumesPerHourForLink = volumeAnalyzer.getVolumesPerHourForLink(linkId);
			double volumeInPeakHours = getPeakHourVolums(volumesPerHourForLink);

			priorityQueue.add(new SortableMapObject<Id>(linkId, -volumeInPeakHours));
			count++;
		}

		// find out, which agents drive over the link during peak hours

		Scenario scenario = GeneralLib.readScenario(plansFileName, networkFileName, facilitiesFileName);

		int selectTop10Percent = count / 10;

		for (int i = 0; i < selectTop10Percent; i++) {
			SortableMapObject sortableMapObject = priorityQueue.poll();
		}

		// cluster them, e.g. cluster with high density, so that the method
		// works!
		// how much parking reduction needed?
	}

	private static double getPeakHourVolums(double[] volumesPerHourForLink) {
		double result = 0;
		result += volumesPerHourForLink[7];
		result += volumesPerHourForLink[8];
		result += volumesPerHourForLink[15];
		result += volumesPerHourForLink[16];
		result += volumesPerHourForLink[17];
		result += volumesPerHourForLink[18];
		result += volumesPerHourForLink[19];

		return result;
	}

	private static class PeakHourAgents implements LinkEnterEventHandler {

		private LinkedList<Id> personIds = new LinkedList<Id>();

		public PeakHourAgents() {

		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if ((event.getTime() > 7 * 3600 && event.getTime() < 8 * 3600)
					|| (event.getTime() > 15 * 3600 && event.getTime() < 19 * 3600)) {
				this.getPersonIds().add(event.getPersonId());
			}
		}

		public LinkedList<Id> getPersonIds() {
			return personIds;
		}

	}

}
