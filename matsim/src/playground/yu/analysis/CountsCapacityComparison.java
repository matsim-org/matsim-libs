/**
 * 
 */
package playground.yu.analysis;

import java.io.IOException;

import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class CountsCapacityComparison {

	/**
	 * compare link capacity with counts-value, in order to check problematical
	 * link capacites
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		final String outputFilename = "../matsimTests/countsCapacityComparison/output_zurich.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		SimpleWriter sw = new SimpleWriter(outputFilename);
		sw.writeln("linkId\tx\ty\tCapacity [veh/h]\tmax Value of Counts");

		double capPeriod = ((double) network.getCapacityPeriod()) / 3600.0;
		for (Id linkId : counts.getCounts().keySet()) {
			Link link = network.getLink(linkId);
			if (link != null) {
				double capacity = link.getCapacity(0) / capPeriod;
				double maxCountsValue = counts.getCount(linkId).getMaxVolume()
						.getValue();
				if (capacity <= maxCountsValue) {
					sw.writeln(linkId.toString() + "\t"
							+ link.getCenter().getX() + "\t"
							+ link.getCenter().getY() + "\t" + capacity + "\t"
							+ maxCountsValue);
					// TODO what about the upward and downward links??
					// upward
					int smallUpwardLinks = 1;
					while (smallUpwardLinks == 1) {
						sw.writeln("upward");
						double capSum = 0.0;
						smallUpwardLinks = 0;
						for (Link inLink : link.getFromNode().getInLinks()
								.values()) {
							capSum += inLink.getCapacity(0) / capPeriod;
							smallUpwardLinks++;
						}
						if (capSum <= maxCountsValue) {
							for (Link inLink : link.getFromNode().getInLinks()
									.values()) {
								sw.writeln(inLink.getId().toString() + "\t"
										+ inLink.getCenter().getX() + "\t"
										+ inLink.getCenter().getY() + "\t"
										+ inLink.getCapacity(0) / capPeriod);
							}
						}
						link = link.getFromNode().getInLinks().values()
								.iterator().next();
					}
					// downward
					int smallDownwardLinks = 1;
					while (smallDownwardLinks == 1) {
						sw.writeln("downward");
						double capSum = 0.0;
						smallDownwardLinks = 0;
						for (Link outLink : link.getToNode().getOutLinks()
								.values()) {
							capSum += outLink.getCapacity(0) / capPeriod;
							smallDownwardLinks++;
						}
						if (capSum <= maxCountsValue) {
							for (Link outLink : link.getToNode().getOutLinks()
									.values()) {
								sw.writeln(outLink.getId().toString() + "\t"
										+ outLink.getCenter().getX() + "\t"
										+ outLink.getCenter().getY() + "\t"
										+ outLink.getCapacity(0) / capPeriod);
							}
						}
						link = link.getToNode().getOutLinks().values()
								.iterator().next();
					}

					sw.flush();
				}
			}
		}
		sw.close();
	}
}
