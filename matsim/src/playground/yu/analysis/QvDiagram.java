/**
 * 
 */
package playground.yu.analysis;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.charts.XYScatterChart;
import org.matsim.world.World;

/**
 * @author yu
 * 
 */
public class QvDiagram {

	public static void main(String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../runs/run628/it.500/500.events.txt.gz";
		final String picFilename = "../runs/run628/it.500/500.qv/";

		Gbl.createConfig(null// new String[] { "./test/yu/test/configTest.xml" }
				);
		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Events events = new Events();
		VolumesAnalyzer va = new VolumesAnalyzer(300, 24 * 3600 - 1, network);
		events.addHandler(va);
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(network, 300, 682845.0,
				247388.0, 2000.0);
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		double maxQ = 0;
		String maxQlinkId = "";
		for (String linkId : clas.getInterestLinkIds()) {
			XYScatterChart chart = new XYScatterChart("Q-V Diagram of link "
					+ linkId, "q [veh/h]", "v [km/h]");
			int vaQ[] = va.getVolumesForLink(linkId);
			if (vaQ != null) {
				double v[] = new double[288];
				double q[] = new double[288];
				for (int i = 0; i < 288; i++) {
					v[i] = clas.getAvgSpeed(new IdImpl(linkId), i * 300);
					q[i] = vaQ[i] * 12.0;
					if (q[i] > maxQ) {
						maxQ = q[i];
						if (!maxQlinkId.equals(linkId))
							maxQlinkId = linkId;
					}
				}
				chart.addSeries("q-v", q, v);
			}
			chart.saveAsPng(picFilename + linkId + ".png", 800, 600);
		}
		System.out.println("max value of q in city-centre:\t" + maxQ
				+ " [veh/h];\nlinkId:\t" + maxQlinkId);
		System.out.println("-> Done!");
		System.exit(0);
	}
}
