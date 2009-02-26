/**
 * 
 */
package playground.yu.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

import playground.yu.utils.io.SimpleReader;

/**
 * @author yu
 * 
 */
public class CountsConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// ------------READ MATSIM NETWORK-----------------
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("../berlin data/wip_net.xml");
		// ------------READ .ATT COUNTSFILE----------------
		SimpleReader sr = new SimpleReader(
				"../berlin data/link_counts_PKW_hrs0-24.att");
		Map<String, String> NO_TONODENOs = new HashMap<String, String>();
		Map<String, List<Double>> NO_CVs = new HashMap<String, List<Double>>();
		// filehead
		String line = sr.readLine();
		// after filehead
		while (line != null) {
			line = sr.readLine();
			if (line != null) {
				String[] cells = line.split(";");
				NO_TONODENOs.put(cells[0], cells[1]);
				List<Double> cvs = new ArrayList<Double>(24);
				for (int i = 2; i < 26; i++) {
					cvs.add(Double.parseDouble(cells[i]));
				}
				NO_CVs.put(cells[0], cvs);
			}
		}
		try {
			sr.close();
		} catch (Exception e) {
			System.err.println(e);
		}
		// ---------------PREPARE COUNTS------------
		Counts counts = new Counts();
		counts.setYear(2000);
		counts.setName("berlin counts");
		counts
				.setDescription("extracted from vsp-cvs/studies/berlin-wip/external-data/counts/senstadt-hand/link_counts_PKW_hrs0-24.att");
		for (Link link : network.getLinks().values()) {
			String origLinkId = link.getOrigId();
			if (NO_TONODENOs.keySet().contains(origLinkId)) {
				System.out.println("A:\torigId:\t" + origLinkId + "\tlinkId:\t"
						+ link.getId().toString());
				System.out.println("B:\tlinkToNodeId:\t"
						+ link.getToNode().getId().toString() + "\tTONODENO:\t"
						+ NO_TONODENOs.get(origLinkId));
				if (link.getToNode().getId().toString().equals(
						NO_TONODENOs.get(origLinkId))) {
					System.out.println("C:\tlinkToNodeId:\t"
							+ link.getToNode().getId().toString()
							+ "\tTONODENO:\t" + NO_TONODENOs.get(origLinkId));
					Count count = counts.createCount(link.getId(), null);
					List<Double> cvs = NO_CVs.get(origLinkId);
					for (int i = 0; i < cvs.size(); i++) {
						double volume = cvs.get(i).doubleValue();
						if (volume >= 0)
							count.createVolume(i + 1, volume);
					}
				}
				System.out.println("-------------------------");
			}
		}
		// ------------------WRITE COUNTSFILE----------------
		new CountsWriter(counts, "../berlin data/link_counts_PKW_hrs0-24.xml")
				.write();
	}
}
