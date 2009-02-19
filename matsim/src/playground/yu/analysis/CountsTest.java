/**
 * 
 */
package playground.yu.analysis;

import java.util.Map;

import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

/**
 * @author yu
 * 
 */
public class CountsTest {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String countsFilename = "../schweiz-ivtch-SVN/baseCase/counts/countsIVTCH.xml";
		// final String outputFilename = "output/counts.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile(countsFilename);

		double countsSummeValueofTime[] = new double[24];
		for (Id linkId : counts.getCounts().keySet()) {
			Map<Integer, Volume> volumes = counts.getCount(linkId).getVolumes();
			int i = 0;
			for (Integer itg : volumes.keySet()) {
				i++;
				double value = volumes.get(itg).getValue();
				String output = "link_ID=" + linkId + "\th=" + itg
						+ "\tvolume=" + value;
				countsSummeValueofTime[itg - 1] += value;
				// if (itg.intValue() != i)
				// output += "\tfalse";
				System.out.println(output);
			}
		}
		for (int i = 0; i < 24; i++)
			System.out.println((i + 1) + "\t" + countsSummeValueofTime[i]);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
