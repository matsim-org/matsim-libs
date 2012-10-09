/**
 * 
 */
package playground.qiuhan.sa;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * org.matsim.core.network.algorithms.TransportModeNetworkFilter is used to
 * create some special subnetwork containing only some transport modes.
 * 
 * @author Q. SUN
 * 
 */
public class TransportModesSubNetworkCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		run(args);
	}

	/**
	 * @param args
	 */
	public static void run(String[] args) {
		String fullNetworkFile = "output/matsimNetwork/networkBerlin2.xml";
		String subCarNetworkFile = "output/matsimNetwork/networkBerlin2car.xml";
		String subPtNetworkFile = "output/matsimNetwork/networkBerlin2pt.xml";

		Scenario scenarioWithFullNetwork = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenarioWithFullNetwork)
				.readFile(fullNetworkFile);
		Network fullNetwork = scenarioWithFullNetwork.getNetwork();

		// create car-network
		Scenario scenarioWithCarSubNetwork = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		Network carSubNetwork = scenarioWithCarSubNetwork.getNetwork();

		Set<String> extractModes4carNet = new HashSet<String>();
		extractModes4carNet.add(TransportMode.car);

		Logger.getLogger(TransportModesSubNetworkCreator.class.getName()).info(
				"Filtering carSubNetwork");
		new TransportModeNetworkFilter(fullNetwork).filter(carSubNetwork,
				extractModes4carNet);

		/*----------IMPORTANT, only 4 car-subnetwork--------------------*/
		Logger.getLogger(TransportModesSubNetworkCreator.class.getName()).info(
				"Cleaning carSubNetwork");
		new NetworkCleaner().run(carSubNetwork);
		/*-------------------------------------------------*/
		Logger.getLogger(TransportModesSubNetworkCreator.class.getName()).info(
				"Writing carSubNetwork");
		new NetworkWriter(carSubNetwork).write(subCarNetworkFile);

		// create "pt"-network
		Scenario scenarioWithPtSubNetwork = ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		Network ptSubNetwork = scenarioWithPtSubNetwork.getNetwork();

		Set<String> extractModes4ptNet = new HashSet<String>();
		extractModes4ptNet.add("bus");
		extractModes4ptNet.add(TransportMode.walk);
		extractModes4ptNet.add("other");
		extractModes4ptNet.add(TransportMode.bike);
		extractModes4ptNet.add("tram");
		extractModes4ptNet.add("train");

		Logger.getLogger(TransportModesSubNetworkCreator.class.getName()).info(
				"Filtering ptSubNetwork");
		new TransportModeNetworkFilter(fullNetwork).filter(ptSubNetwork,
				extractModes4ptNet);

		Logger.getLogger(TransportModesSubNetworkCreator.class.getName()).info(
				"Writing ptSubNetwork");
		new NetworkWriter(ptSubNetwork).write(subPtNetworkFile);
	}
}
