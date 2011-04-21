/**
 *
 */
package playground.yu.counts;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

/**
 * quote from playground.balmermi.Scenario
 *
 * @author yu
 *
 */
public class CountsSimCompareTest {
	private static boolean isInRange(final Id linkid, final NetworkImpl net) {
		Link l = net.getLinks().get(linkid);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkid.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(net.getNodes().get(new IdImpl("2531")).getCoord()) < 30000;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		String eventsFilename = "../runs-svn/run669/it.500/500.events.txt.gz";
		String outputPath = "../matsimTests/compareCountsSim/";
		double countsScaleFactor = 10.0;

		System.out.println("  reading the network...");
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		System.out.println("  reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		System.out.println("  reading the events...");
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 10 * 3600 - 1, network);
		events.addHandler(va);
		new MatsimEventsReader(events).readFile(eventsFilename);

		List<Double> diffs = new ArrayList<Double>();
		Map<Integer, Integer> plusDiffSet = new HashMap<Integer, Integer>();
		Map<Integer, Integer> minusDiffSet = new HashMap<Integer, Integer>();
		try {
			for (int h = 8; h < 9; h++) {
				BufferedWriter writer = IOUtils.getBufferedWriter(outputPath
						+ h + "-" + (h + 1) + ".txt");
				// writer.write("linkId\t" + "fromHour\ttoHour\t"
				// + "X\tY\tcountValue\tsimValue\tdeviation\n");
				writer.write("simuliert-gemessen\tAnzahl\n");

				for (Id linkId : counts.getCounts().keySet()) {
					if (isInRange(linkId, network)) {
						Count count = counts.getCount(linkId);
						Link link = network.getLinks().get(linkId);
						if (link != null) {
							// Coord toCoord = link.getToNode().getCoord();
							// Coord fromCoord = link.getFromNode().getCoord();
							// double x = 0.7 * toCoord.getX() + 0.3
							// * fromCoord.getX();
							// double y = 0.7 * toCoord.getY() + 0.3
							// * fromCoord.getY();
							// if (x != 0 && y != 0)

							if (va.getVolumesForLink(linkId) != null) {
								double countVal = count.getVolume(h + 1)
										.getValue();
								double simVal = va.getVolumesForLink(linkId)[h]
										* countsScaleFactor;
								diffs.add(simVal - countVal);
								// writer
								// .write(linkId + "\t" + h + "\t"
								// + (h + 1) + "\t" + x + "\t" + y
								// + "\t" + countVal + "\t"
								// + simVal + "\t"
								// + (simVal - countVal)
								// / countVal + "\n");
							}
						}
					}
				}
				for (Double d : diffs) {
					int key = ((int) d.doubleValue()) / 25 * 25;
					if (key == 0 && d > 0)
						key = 10000;
					if (key == 0 && d < 0)
						key = -10000;
					if (key >= 0) {
						Integer num = plusDiffSet.get(key);
						if (num == null)
							num = Integer.valueOf(0);
						plusDiffSet.put(key, num + 1);
					} else {
						Integer num = minusDiffSet.get(key);
						if (num == null)
							num = Integer.valueOf(0);
						minusDiffSet.put(key, num + 1);
					}
				}
				for (Entry<Integer, Integer> itgEntry : plusDiffSet.entrySet()) {
					writer.write(itgEntry.getKey() + "\t" + itgEntry.getValue()
							+ "\n");
				}
				for (Entry<Integer, Integer> itgEntry : minusDiffSet.entrySet()) {
					writer.write(itgEntry.getKey() + "\t" + itgEntry.getValue()
							+ "\n");
				}
				writer.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("  done!");
	}
}
