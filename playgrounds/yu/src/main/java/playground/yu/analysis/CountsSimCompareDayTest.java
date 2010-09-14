/**
 *
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

/**
 * quote from playground.balmermi.Scenario
 *
 * @author yu
 *
 */
public class CountsSimCompareDayTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		String eventsFilename = "../runs/run628/it.500/500.events.txt.gz";
		String outputPath = "../runs/run628/it.500/500.compareCountsSim.";
		double countsScaleFactor = 10.0;

		System.out.println("  reading the network...");
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		System.out.println("  reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		System.out.println("  reading the events...");
		EventsManagerImpl events = new EventsManagerImpl();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1, network);
		events.addHandler(va);
		new MatsimEventsReader(events).readFile(eventsFilename);

		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputPath
					+ "24h.txt.gz");
			writer
					.write("linkId\t"
							+ "X\tY\tcountValue\tsimValue\tdeviation((sim-count)/count)\n");
			for (Id linkId : counts.getCounts().keySet()) {
				Count count = counts.getCount(linkId);
				Link link = network.getLinks().get(linkId);
				if (link != null) {
					Coord toCoord = link.getToNode().getCoord();
					Coord fromCoord = link.getFromNode().getCoord();
					double x = 0.7 * toCoord.getX() + 0.3 * fromCoord.getX();
					double y = 0.7 * toCoord.getY() + 0.3 * fromCoord.getY();
					if ((x != 0 && y != 0)
							&& (va.getVolumesForLink(linkId) != null)) {
						double countVal = 0.0;
						double simVal = 0.0;
						for (int h = 0; h < 24; h++) {
							countVal += count.getVolume(h + 1).getValue();
							simVal += va.getVolumesForLink(linkId)[h]
									* countsScaleFactor;
						}
						writer.write(linkId + "\t" + x + "\t" + y + "\t"
								+ countVal + "\t" + simVal + "\t"
								+ (simVal - countVal) / countVal + "\n");
					}
				}
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("  done!");
	}

}
