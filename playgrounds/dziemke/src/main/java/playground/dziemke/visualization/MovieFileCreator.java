package playground.dziemke.visualization;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFEvent2MVI;

/**
 * @author dziemke
 */
public class MovieFileCreator {
	private final static Logger log = Logger.getLogger(MovieFileCreator.class);

	public static void main(String[] args) {
		// Parameter
		String runOutputRoot = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/output/counts-stations-50";
		double snapshotPeriod = 60;
		
		// Files
		String eventFile = runOutputRoot + "/output_events.xml.gz";
		String networkFile = runOutputRoot + "/output_network.xml.gz";
		String mviFile = runOutputRoot + "/otfvis.mvi";

		// Add network to scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		
		// File conversion
		OTFEvent2MVI.convert(scenario, eventFile, mviFile, snapshotPeriod);
		log.info("Movie file " + mviFile + " created.");
	}
}