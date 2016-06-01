package playground.singapore.springcalibration.preprocess;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
//import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class HitsPlansAnanlyzer {
	
	private String outdir;
	
	public static void main(String[] args) {
		HitsPlansAnanlyzer analyzer = new HitsPlansAnanlyzer();
		analyzer.run(args[0], args[1], args[2]);
	}
	
	
	public void run(String hitsplansfile, String facilitiesfile, String outdir) {
		this.outdir = outdir;
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario).readFile(facilitiesfile);
		new MatsimPopulationReader(scenario).readFile(hitsplansfile);
		
		//new PopulationWriter(scenario.getPopulation()).writeFileV5(outdir + "/hitsPlansCompleted.xml.gz");
		
		this.analyze(scenario.getPopulation());
	}
	
	private void analyze(Population population) {
		// ########### trip length and duration
		// - per mode
		// - per act type
		
		// ########### mode shares
		// - counts
		// - distance
		// - time
		TDDistributions distributions = new TDDistributions(population, outdir);
		distributions.run();	
	}
}
