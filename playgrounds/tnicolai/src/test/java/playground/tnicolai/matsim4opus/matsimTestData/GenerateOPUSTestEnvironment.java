package playground.tnicolai.matsim4opus.matsimTestData;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.io.FileCopy;
import playground.tnicolai.matsim4opus.utils.io.TempDirectoryUtil;

public class GenerateOPUSTestEnvironment {
	
	// Logger
	private static final Logger log = Logger.getLogger(GenerateOPUSTestEnvironment.class);
	
	private boolean isTestRun;
	
	/**
	 * Constructor
	 * @param isTestRun
	 */
	public GenerateOPUSTestEnvironment(boolean isTestRun){
		this.isTestRun = isTestRun;
	}
	
	/**
	 * Constructor
	 */
	public GenerateOPUSTestEnvironment(){
		this.isTestRun = Boolean.FALSE;
	}
	
	/**
	 * creates a default opus environment for testing purposes 
	 */
	public String createOPUSTestEnvironment(){
		
		// creates temporary opus directories
		TempDirectoryUtil.createOPUSDirectories();
		
		// copying matsim input data into fresh created opus directories
		String urbanSimOutputDir = UtilityCollection.getTestUrbanSimInputDataDir( GenerateOPUSTestEnvironment.class );
		String matsimNetwork = UtilityCollection.getWarmStartNetwork( GenerateOPUSTestEnvironment.class );

		// copy UrbanSim data to temp dir
		copyTestData(urbanSimOutputDir, Constants.MATSIM_4_OPUS_TEMP);
		// copy MATSim network to temp dir
		copyTestData(matsimNetwork, Constants.MATSIM_4_OPUS_TEMP);
		
		return generateMATSimConfig ( matsimNetwork, "", MATSimRunMode.coldStart );
	}
	
	/**
	 * creates a opus environment for warm start testing
	 */
	public String createWarmStartOPUSTestEnvironment(String warmStartPlansFile, byte runMode){
		
		// creates temporary opus directories
		TempDirectoryUtil.createOPUSDirectories();
		
		// copying matsim input data into fresh created opus directories
		String urbanSimOutputDir = UtilityCollection.getWarmStartUrbanSimInputData( GenerateOPUSTestEnvironment.class );
		String plansFileDir = UtilityCollection.getWarmStartInputPlansFile( GenerateOPUSTestEnvironment.class );
		String matsimNetwork = UtilityCollection.getWarmStartNetwork( GenerateOPUSTestEnvironment.class );

		// copy UrbanSim data to temp dir
		copyTestData(urbanSimOutputDir, Constants.MATSIM_4_OPUS_TEMP);
		// copy plans files to temp dir
		copyTestData(plansFileDir, Constants.MATSIM_4_OPUS_TEMP);
		// copy MATSim network to temp dir
		copyTestData(matsimNetwork, Constants.MATSIM_4_OPUS_TEMP);
		
		return generateMATSimConfig ( matsimNetwork, plansFileDir + warmStartPlansFile, runMode);
	}

	/**
	 * @param matsimNetwork
	 * @return path to generated MATSim config
	 */
	private String generateMATSimConfig(String matsimNetwork, String plansFile, byte runMode) {
		// generate MATSim config
		GenerateMATSimConfig gmc;

		if(runMode == MATSimRunMode.warmStart)
			gmc = new GenerateMATSimConfig(this.isTestRun, matsimNetwork + "/psrc.xml.gz", plansFile);
		else if(runMode ==MATSimRunMode.hotStart)
			gmc = new GenerateMATSimConfig(this.isTestRun, matsimNetwork + "/psrc.xml.gz", "", plansFile);
		else // coldStart
			gmc = new GenerateMATSimConfig(isTestRun, matsimNetwork + "/psrc.xml.gz");
		
		gmc.generate();
		
		log.info("MATSim config file is located at: " + gmc.getMATSimConfigPath());
		
		return gmc.getMATSimConfigPath(); // returns the path to MATSim config
	}

	/**
	 * @param source
	 */
	private void copyTestData(String source, String destination) {
		
		log.info("Copying UrbanSim data from " + source + " to " + destination);
		if( !FileCopy.copyTree(source, destination) ){
			log.error("Error while copying matsim4opus test data.");
			System.exit(-1);
		}
	}
	
	/**
	 * clean up temporary test files 
	 */
	public void cleanOPUSTestEnvironment(){
		TempDirectoryUtil.cleaningUpOPUSDirectories();
	}
		
	/**
	 * Test GenerateOPUSTestEnvironment
	 * @param args
	 */
	public static void main (String args[]){
		log.info("Starting test ...");
		GenerateOPUSTestEnvironment gote = new GenerateOPUSTestEnvironment();
		gote.createOPUSTestEnvironment();
		gote.cleanOPUSTestEnvironment();
		log.info("... done!");
	}

}
