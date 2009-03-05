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

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm-opt.xml";
		final String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		final String outputFilename = "../matsimTests/countsCapacityComparison/output.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		SimpleWriter sw = new SimpleWriter(outputFilename);
		sw.writeln("linkId\tCapacity [veh/h]\tmax Value of Counts");

		for (Id linkId : counts.getCounts().keySet()) {
			Link link = network.getLink(linkId);
			if (link != null) {
				double capacity = link.getCapacity(0)
						/ (double) network.getCapacityPeriod() * 3600.0;
				double countsvalue = counts.getCount(linkId).getMaxVolume()
						.getValue();
				if (capacity <= countsvalue) {
					sw.writeln(linkId.toString() + "\t" + capacity + "\t"
							+ countsvalue);
					sw.flush();
				}
			}
		}
		sw.close();
	}
}
