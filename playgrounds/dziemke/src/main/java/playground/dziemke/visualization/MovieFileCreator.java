package playground.dziemke.visualization;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFEvent2MVI;

/**
 * @author dziemke
 */
public class MovieFileCreator {
	private final static Logger log = Logger.getLogger(MovieFileCreator.class);

	public static void main(String[] args) {
		// Parameters
		String inputOutputRoot = "D:/Workspace/container/stockholm/";
		String runId = "02";
		int iteration = 100;
		
		// Input and output files				
		String eventFile = inputOutputRoot + "output/" + runId + "/ITERS/it." + iteration + "/" + runId + "." + iteration + ".events.xml.gz";
		String networkFile = "../../shared-svn/projects/stockholm/network_cleaned_simplified.xml";
		String mviFile = inputOutputRoot + "output/" + runId + "/ITERS/it." + iteration + "/" + runId + "." + iteration + ".otfvis.mvi";
		double snapshotPeriod = 60;
		
		// Initiating conversion				
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		OTFEvent2MVI.convert(scenario, eventFile, mviFile, snapshotPeriod);
		
		log.info("Movie file " + mviFile + " created.");
	}
}
