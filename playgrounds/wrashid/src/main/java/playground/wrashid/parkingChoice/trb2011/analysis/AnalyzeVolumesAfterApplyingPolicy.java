package playground.wrashid.parkingChoice.trb2011.analysis;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.SortableMapObject;
import playground.wrashid.parkingChoice.trb2011.analysis.LinkVolumeAnalyzer.PeakHourAgents;

public class AnalyzeVolumesAfterApplyingPolicy {

	public static void main(String[] args) {
		String outputFolder="H:/data/experiments/TRBAug2011/runs/ktiRun31/output/";
		final String networkFileName = outputFolder + "output_network.xml.gz";
		final String eventsFileName = outputFolder + "ITERS/it.50/50.events.xml.gz";
		
		NetworkImpl network = GeneralLib.readNetwork(networkFileName);
		
		VolumesAnalyzer volumeAnalyzerBaseScenario = loadVolumeOfBaseScenario(network, eventsFileName);
		
		String outputFolderAfterApplyingPolicy="H:/data/experiments/TRBAug2011/runs/ktiRun35/output/";
		final String eventsFileNameAfterApplyingPolicy = outputFolderAfterApplyingPolicy + "ITERS/it.50/50.events.xml.gz";
		
		VolumesAnalyzer volumeAnalyzerAfterApplyingPolicy = loadVolumeOfBaseScenario(network, eventsFileNameAfterApplyingPolicy);

		LinkedList<Id> peakHourLinkIds = getPeakHourLinkIds(volumeAnalyzerBaseScenario);
		
		printDifferenceVolumeSumOfPeakHourLinks(peakHourLinkIds,volumeAnalyzerBaseScenario,volumeAnalyzerAfterApplyingPolicy);
	
	}
	
	

	private static void printDifferenceVolumeSumOfPeakHourLinks(LinkedList<Id> peakHourLinkIds,
			VolumesAnalyzer volumeAnalyzerBaseScenario, VolumesAnalyzer volumeAnalyzerAfterApplyingPolicy) {
		
		int[] sumVolumeBaseCase=new int[24];
		int[] sumVolumeWithPolicy=new int[24];
		
		for (Id linkId:peakHourLinkIds){
			int[] volumesForLinkBaseCase = volumeAnalyzerBaseScenario.getVolumesForLink(linkId);
			int[] volumesForLinkWithPolicy = volumeAnalyzerAfterApplyingPolicy.getVolumesForLink(linkId);
			
			for (int i=0;i<24;i++){
				sumVolumeBaseCase[i]+=volumesForLinkBaseCase[i];
				sumVolumeWithPolicy[i]+=volumesForLinkWithPolicy[i];
			}
		}
		
		System.out.println("hour\tbase case\treduced public parkings");
		for (int i=0;i<24;i++){
			System.out.println(i + "\t" + sumVolumeBaseCase[i] + "\t" + sumVolumeWithPolicy[i]);
		}
		
		
	}



	private static LinkedList<Id> getPeakHourLinkIds(VolumesAnalyzer volumeAnalyzer) {
		PriorityQueue<SortableMapObject<Id>> priorityQueue = new PriorityQueue<SortableMapObject<Id>>();

		int count = 0;
		for (Id linkId : volumeAnalyzer.getLinkIds()) {
			double[] volumesPerHourForLink = volumeAnalyzer.getVolumesPerHourForLink(linkId);
			double volumeInPeakHours = LinkVolumeAnalyzer.getPeakHourVolums(volumesPerHourForLink);

			priorityQueue.add(new SortableMapObject<Id>(linkId, -volumeInPeakHours));
			count++;
		}

		// find out, which agents drive over the link during peak hours

		int selectTop10Percent = count / 10;
		LinkedList<Id> peakHourLinkIds = new LinkedList<Id>();

		for (int i = 0; i < selectTop10Percent; i++) {
			SortableMapObject<Id> sortableMapObject = priorityQueue.poll();
			peakHourLinkIds.add(sortableMapObject.getKey());
		}
		return peakHourLinkIds;
	}

	private static VolumesAnalyzer loadVolumeOfBaseScenario(final NetworkImpl network, final String eventsFileName) {
		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();

		VolumesAnalyzer volumeAnalyzer = new VolumesAnalyzer(3600, 24 * 3600 - 1, network);
		eventsManager.addHandler(volumeAnalyzer);

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);
		return volumeAnalyzer;
	}
	
}
