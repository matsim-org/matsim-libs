package lcrociani.TestMatsimConnector;

import matsimConnector.network.HybridNetworkBuilder;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;

import org.junit.Ignore;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import pedCA.output.Log;

public class NetworkBuildTest {
	
	@Ignore
	public void TestNetworkBuild(){
		try{
			String PATH = Constants.INPUT_PATH;
			Config c = ConfigUtils.loadConfig(PATH+"/config.xml");
			Scenario scenario = ScenarioUtils.loadScenario(c);
			CAScenario scenarioCA = new CAScenario(PATH+"/CAScenario");
			HybridNetworkBuilder.buildNetwork(scenarioCA.getCAEnvironment(Id.create("0", CAEnvironment.class)), scenarioCA);
			scenarioCA.connect(scenario);
		}catch(UncheckedIOException e){
			Log.error("Path not found!");
			return;
		}
	}
}
