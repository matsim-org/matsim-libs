/**
 *
 */
package playground.yu.counts;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

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
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario)
				.readFile("../berlin data/old_wip/wip_net.xml");
		// ------------READ .ATT COUNTSFILE----------------
		SimpleReader sr = new SimpleReader(
				"../berlin data/old_wip/link_counts_PKW_hrs0-24.att");
		List<Tuple<String, String>> NO_TONODENOs = new ArrayList<Tuple<String, String>>();
		List<List<Double>> NO_CVs = new ArrayList<List<Double>>();
		// filehead
		String line = sr.readLine();
		// after filehead
		while (line != null) {
			line = sr.readLine();
			if (line != null) {
				String[] cells = line.split(";");
				NO_TONODENOs.add(new Tuple<String, String>(cells[0], cells[1]));
				List<Double> cvs = new ArrayList<Double>(24);
				for (int i = 2; i < 26; i++) {
					cvs.add(Double.parseDouble(cells[i]));
				}
				NO_CVs.add(cvs);
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
		counts.setLayer("0");
		counts
				.setDescription("extracted from vsp-cvs/studies/berlin-wip/external-data/counts/senstadt-hand/link_counts_PKW_hrs0-24.att");
		for (Link link : network.getLinks().values()) {
			String origLinkId = ((LinkImpl) link).getOrigId();
			for (int i = 0; i < NO_TONODENOs.size(); i++) {
				Tuple<String, String> tuple = NO_TONODENOs.get(i);
				if (tuple.getFirst().equals(origLinkId)) {

					System.out.println("A:\torigId:\t" + origLinkId
							+ "\tlinkId:\t" + link.getId().toString());
					System.out.println("B:\tlinkToNodeId:\t"
							+ link.getToNode().getId().toString()
							+ "\tTONODENO:\t" + tuple.getSecond());

					if (link.getToNode().getId().toString().equals(
							tuple.getSecond())) {

						System.out.println("C:\tlinkToNodeId:\t"
								+ link.getToNode().getId().toString()
								+ "\tTONODENO:\t" + tuple.getSecond());

						List<Double> cvs = NO_CVs.get(i);
						boolean hasValidVolume = false;
						for (Double value : cvs)
							if (value.doubleValue() >= 0) {
								hasValidVolume = true;
								break;
							}

						if (hasValidVolume) {
							Count count = counts
									.createCount(link.getId(), null);
							count.setCoord(link.getCoord());
							for (int j = 0; j < cvs.size(); j++) {
								double volume = cvs.get(j).doubleValue();
								if (volume >= 0)
									count.createVolume(j + 1, volume);
							}
						}
					}
					System.out.println("-------------------------");
				}
			}
		}
		// ------------------WRITE COUNTSFILE----------------
		new CountsWriter(counts)
				.write("../berlin data/link_counts_PKW_hrs0-24.xml");
	}
}
