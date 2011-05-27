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
	
	private void init(String createConfigFile) {
		Config createConfig = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(createConfig);
    	matsimConfigReader.readFile(createConfigFile);
    	
    	int numberOfRuns = Integer.parseInt(createConfig.findParam("PLOC", "numberOfRuns"));
    	log.info("number of Runs: " + numberOfRuns);
    	
    	this.createPlansAndConfigs(createConfig);
    	
    	for (int i = 0; i < 100000; i++) {
    		this.randomNumberGenerator.nextLong();
    	}
	}
	
	private void createPlansAndConfigs(Config createConfig) {
		Config runConfig = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(runConfig);
    	String runConfigFile = createConfig.findParam("PLOC", "runConfig");
    	String inPathStub = createConfig.findParam("PLOC", "inPath");
    	matsimConfigReader.readFile(runConfigFile);
    	
    	for (int runIndex = 0; runIndex < numberOfRuns; runIndex++) {
    		long seed = randomNumberGenerator.nextLong();
    		
    		createConfig.setParam("global", "randomSeed", Long.toString(seed));
    		createConfig.setParam("locationchoiceExperimental", "randomSeed", Long.toString(seed));
    		
    		runConfig.setParam("global", "randomSeed", Long.toString(seed));
    		runConfig.setParam("locationchoiceExperimental", "randomSeed", Long.toString(seed));
        			
    		String configPath = "./input/PLOC/zh/1Pct/runs/";
        	new File(configPath).mkdirs();
        	ConfigWriter configWriter = new ConfigWriter(createConfig);
        	configWriter.write(configPath + "/createConfig.xml");	
        	this.adaptPlansAndFacilities(configPath + "/createConfig.xml");
    		
    		// now write the final config  		
        	runConfig.setParam("plans", "inputPlansFile", "./input/PLOC/zh/1Pct/runs/run" + runIndex + "/plans.xml.gz");
        	runConfig.setParam("controler", "runId", Integer.toString(runIndex));
        	runConfig.setParam("facilities", "inputFacilitiesFile", "./input/PLOC/zh/1Pct/runs/run" + runIndex + "/facilities.xml.gz");
        	String path = inPathStub + "/run" + runIndex;
        	        	
        	new File(path).mkdirs();
        	configWriter.write(path + "/config.xml");			
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
