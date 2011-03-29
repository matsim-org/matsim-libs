package playground.anhorni.PLOC.analysis;

import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;


public class CompareScenarios {
	
	public String outpath = "src/main/java/playground/anhorni/output/PLOC/zh/";
	private ScenarioImpl baseScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());


	public static void main(String[] args) {
	}
	
	private void init(final String networkfilePath,	final String facilitiesfilePath) {
		new MatsimNetworkReader(baseScenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(baseScenario).readFile(facilitiesfilePath);
	}
		
	public void run(String networkfilePath, String facilitiesfilePath) {
		this.init(networkfilePath, facilitiesfilePath);
		
		CompareScores scoreComparator = new CompareScores();
		scoreComparator.handleScenario(this.baseScenario);
		scoreComparator.printStatistics(outpath);
	}
}
