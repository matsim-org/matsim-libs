/**
 * 
 */
package playground.qiuhan.sa;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mrieser.pt.utils.MergeNetworks;

/**
 * uses {@code playground.mrieser.pt.utils.MergeNetworks} to merge 2 networks
 * 
 * @author Q. SUN
 * 
 */
public class Merge2Networks {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String anIV_NetworkFile = "input/A_NM/network.gz"//
		, slOeV_NetworkFile = "output/matsimNetwork/networkBerlin2pt.xml"//
		, combiNetworkFile = "output/matsimNetwork/combi.xml.gz"//
		, prefixIV = "anIV"//
		, prefixOeV = "slOeV";

		Scenario scenarioIV = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenarioIV).readFile(anIV_NetworkFile);

		Scenario scenarioOeV = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenarioOeV).readFile(slOeV_NetworkFile);

		Scenario scenarioCombi = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		NetworkImpl combiNet = (NetworkImpl) scenarioCombi.getNetwork();
		MergeNetworks.merge(scenarioIV.getNetwork(), prefixIV,
				scenarioOeV.getNetwork(), prefixOeV, combiNet);

		new NetworkWriter(combiNet).write(combiNetworkFile);
	}
}
