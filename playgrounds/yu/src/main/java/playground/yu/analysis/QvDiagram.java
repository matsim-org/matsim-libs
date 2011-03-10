/**
 * 
 */
package playground.yu.analysis;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author yu
 * 
 */
public class QvDiagram {

	public static void main(String[] args) {
		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String eventsFilename = "../runs/run628/it.500/500.events.txt.gz";
		final String picFilename = "../runs/run628/it.500/500.qv/";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		EventsManagerImpl events = new EventsManagerImpl();
		VolumesAnalyzer va = new VolumesAnalyzer(300, 24 * 3600 - 1, network);
		events.addHandler(va);
		CalcLinksAvgSpeed clas = new CalcLinksAvgSpeed(network, 300, 682845.0,
				247388.0, 2000.0);
		events.addHandler(clas);

		new MatsimEventsReader(events).readFile(eventsFilename);

		double maxQ = 0;
		Id maxQlinkId = null;
		for (Id linkId : clas.getInterestLinkIds()) {
			XYScatterChart chart = new XYScatterChart("Q-V Diagram of link "
					+ linkId, "q [veh/h]", "v [km/h]");
			int vaQ[] = va.getVolumesForLink(linkId);
			if (vaQ != null) {
				double v[] = new double[288];
				double q[] = new double[288];
				for (int i = 0; i < 288; i++) {
					v[i] = clas.getAvgSpeed(linkId, i * 300);
					q[i] = vaQ[i] * 12.0;
					if (q[i] > maxQ) {
						maxQ = q[i];
						if (!linkId.equals(maxQlinkId))
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
