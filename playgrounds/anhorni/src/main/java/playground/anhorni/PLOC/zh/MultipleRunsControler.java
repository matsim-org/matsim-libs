package playground.anhorni.PLOC.zh;

import java.io.File;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;

import playground.anhorni.LEGO.miniscenario.create.AdaptZHScenario;
import playground.anhorni.PLOC.SingleRunControler;

public class MultipleRunsControler {
	
	private final static Logger log = Logger.getLogger(MultipleRunsControler.class);
	private int numberOfRuns = 10;
	private Random randomNumberGenerator = new Random(834273782);

	public static void main(String[] args) {
		MultipleRunsControler runControler = new MultipleRunsControler();
		runControler.init(args[0]);
    	runControler.run();
	}
	
	private void init(String configFile) {
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(configFile);
    	
    	int numberOfRuns = Integer.parseInt(config.findParam("PLOC", "numberOfRuns"));
    	log.info("number of Runs: " + numberOfRuns);
    	
    	this.createPlansAndConfigs(config);
    	
    	for (int i = 0; i < 100000; i++) {
    		this.randomNumberGenerator.nextLong();
    	}
	}
	
	private void createPlansAndConfigs(Config config) {    	
    	for (int runIndex = 0; runIndex < numberOfRuns; runIndex++) {
    		long seed = randomNumberGenerator.nextLong();
    		// first adapt plans and facilities
    		String path = "./input/PLOC/zh/1Pct/";
			new File(path).mkdirs();
			config.setParam("controler", "outputDirectory", path);
			config.setParam("global", "randomSeed", Long.toString(seed));
			config.setParam("locationchoiceExperimental", "randomSeed", Long.toString(seed));
    		String configPath = "./input/PLOC/zh/1Pct/runs/";
        	new File(configPath).mkdirs();
        	ConfigWriter configWriter = new ConfigWriter(config);
        	configWriter.write(configPath + "/configTmp.xml");	
        	this.adaptPlansAndFacilities(configPath + "/configTmp.xml");
    		
    		// now write the final config  		
			config.setParam("plans", "inputPlansFile", "./input/PLOC/zh/1Pct/runs/run" + runIndex + "/plans.xml.gz");
        	config.setParam("controler", "runId", Integer.toString(runIndex));
        	config.setParam("facilities", "inputFacilitiesFile", "./input/PLOC/zh/1Pct/runs/run" + runIndex + "/facilities.xml.gz");
        	path = "./output/PLOC/zh/run" + runIndex;
        	config.setParam("controler", "outputDirectory", path);
        	config.setParam("counts", "countsScaleFactor", "100");
        	
        	configPath = "./input/PLOC/zh/1Pct/runs/run" + runIndex;
        	new File(configPath).mkdirs();
        	configWriter.write(configPath + "/config.xml");			
    	}	
	}
	
	private void adaptPlansAndFacilities(String configFile) {
		AdaptZHScenario adapter = new AdaptZHScenario();
		adapter.run(configFile);
	}
	
	public void run() {
		for (int runIndex = 0; runIndex < numberOfRuns; runIndex++) {
			String configFile = "./input/PLOC/zh/1Pct/runs/run" + runIndex + "/config.xml";
			String config[] = {configFile};
			SingleRunControler controler;
    		controler = new SingleRunControler(config);	 
        	controler.run();
		}
	}
}
