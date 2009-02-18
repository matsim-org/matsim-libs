/**
 * 
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

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

		Gbl.createConfig(null);
		World world = Gbl.getWorld();

		System.out.println("  reading the network...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		System.out.println("  reading the counts...");
		final Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		System.out.println("  reading the events...");
		Events events = new Events();
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
				Link link = network.getLink(linkId);
				if (link != null) {
					Coord toCoord = link.getToNode().getCoord();
					Coord fromCoord = link.getFromNode().getCoord();
					double x = 0.7 * toCoord.getX() + 0.3 * fromCoord.getX();
					double y = 0.7 * toCoord.getY() + 0.3 * fromCoord.getY();
					if (x != 0 && y != 0) {
						if (va.getVolumesForLink(linkId.toString()) != null) {
							double countVal = 0.0;
							double simVal = 0.0;
							for (int h = 0; h < 24; h++) {
								countVal += count.getVolume(h + 1).getValue();
								simVal += va.getVolumesForLink(linkId
										.toString())[h]
										* countsScaleFactor;
							}
							writer.write(linkId + "\t" + x + "\t" + y + "\t"
									+ countVal + "\t" + simVal + "\t"
									+ (simVal - countVal) / countVal + "\n");
						}
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
