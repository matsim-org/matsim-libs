package playground.mmoyo.analysis.comp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

import playground.mmoyo.analysis.counts.chen.CountsComparingGraph;
import playground.mmoyo.analysis.counts.chen.CountsComparingGraphMinMax;
import playground.mmoyo.ptRouterAdapted.AdaptedLauncher;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.yu.run.TrCtl;
import org.matsim.core.config.ConfigWriter;

public class Comparer {
	final String configFile;
	final Config config;
	final String varName;
	final String subOutputDir;
	final String configsDir;
	
	final String XML = ".xml";
	
	public Comparer(final String configFile, String varName){
		this.config = new Config();
		this.configFile = configFile;
		this.varName = varName;
		new MatsimConfigReader(config).readFile(this.configFile);
		this.subOutputDir = config.getParam("controler", "outputDirectory") + "/runs/" ;
		this.configsDir = config.getParam("controler", "outputDirectory") + "/configs/";
		initDir();
	}

	private void initDir(){
		//load config
		final Config config = new Config();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(this.configFile);

		//validate that files inside the config really exist 
		existsFile(config.getParam("controler", "outputDirectory"));
		existsFile(config.getParam("ptCounts", "inputBoardCountsFile"));
		existsFile(config.getParam("ptCounts", "inputOccupancyCountsFile"));
		existsFile(config.getParam("ptCounts", "inputAlightCountsFile"));
		existsFile("./res");
		
		//create configs and output sub dirs
		File tmpFile = new File(this.configsDir);
		if (!tmpFile.exists()){tmpFile.mkdir();}
		tmpFile = new File(this.subOutputDir);
		if (!tmpFile.exists()){tmpFile.mkdir();}
		tmpFile= null;
	}

	private void existsFile(String path){
		File tmpFile = new File(path);
		if (!tmpFile.exists()){
			try {
				throw new FileNotFoundException("Can not find: " + tmpFile.getPath());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}	
		}
	}
	
	private void routeAndSimulate(final MyTransitRouterConfig myTransitRouterConfig, String strVarValue){
		myTransitRouterConfig.scenarioName = this.varName + strVarValue;
		System.out.println ("routing " + myTransitRouterConfig.scenarioName);
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(myTransitRouterConfig);
		String routedPlan = adaptedLauncher.route(this.configFile);
		
		//create new config file
		String newConfigFile = this.configsDir + "config_" + strVarValue + XML;
		this.config.setParam("plans", "inputPlansFile", routedPlan );
		this.config.setParam("controler", "outputDirectory", this.subOutputDir + myTransitRouterConfig.scenarioName);
		ConfigWriter configWriter = new ConfigWriter(this.config);
		configWriter.write(newConfigFile);
		System.out.println("config file written: " + newConfigFile);
		
		//launch controller
		System.out.println("\n\n  simulating: " + newConfigFile);
		TrCtl.main(new String[]{newConfigFile});
	}
	
	private void generateComparingGraphs() throws IOException{
		new CountsComparingGraph().createComparingGraphs(this.subOutputDir, this.varName);
	}
	
	private void generateMinMaxGraphs() throws IOException{
		new CountsComparingGraphMinMax().createComparingGraphs(this.subOutputDir, this.varName);
	}
	
	public static void main(String[] args) throws IOException {
		String configFilePath = null;
		String varName;
		
		if (args.length==2){
			configFilePath = args[0];
			varName = args[1];
		}else{
			configFilePath = "../playgrounds/mmoyo/output/bestTest/config.xml";
			varName =  "swit"; 
		}
		Comparer comparer = new Comparer (configFilePath, varName);
		comparer.initDir();
		
		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		myTransitRouterConfig.searchRadius = 600.0;
		myTransitRouterConfig.extensionRadius = 200.0;
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;
		myTransitRouterConfig.fragmentPlans = false;
		myTransitRouterConfig.noCarPlans= true;
		myTransitRouterConfig.allowDirectWalks= true;
		myTransitRouterConfig.compressPlan = true;
		
		//best values
		myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -0.7/1000;
		myTransitRouterConfig.marginalUtilityOfTravelTimeWalk = -6.0/3600;
		myTransitRouterConfig.marginalUtilityOfTravelTimeTransit = -6.0/3600;
		myTransitRouterConfig.costLineSwitch = 240.0 * -myTransitRouterConfig.marginalUtilityOfTravelTimeTransit; 
		
		//once
		//job 16 & 17
		String strVarValue = "best";
		myTransitRouterConfig.scenarioName = "best" ;
		comparer.routeAndSimulate(myTransitRouterConfig, strVarValue);
		
		//many times. example of job15
		/*
		for ( double dVariable = 0.0; dVariable <= 1200.0 ; dVariable += 60.0 ) {	//set here
			double roundedVarValue = Math.round(dVariable*100)/100.0;
			String strVarValue = String.valueOf(roundedVarValue);			
			myTransitRouterConfig.costLineSwitch = roundedVarValue * -myTransitRouterConfig.marginalUtilityOfTravelTimeTransit; 

			System.out.println("marginalUtilityOfTravelDistanceTransit: " + myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit);
			System.out.println("marginalUtilityOfTravelTimeWalk: " + myTransitRouterConfig.marginalUtilityOfTravelTimeWalk);
			System.out.println("marginalUtilityOfTravelTimeTransit: " + myTransitRouterConfig.marginalUtilityOfTravelTimeTransit);
			System.out.println("cost Line Switch: " + myTransitRouterConfig.costLineSwitch);
			
			comparer.routeAndSimulate(myTransitRouterConfig, strVarValue);
		}
		
		//comparer.generateComparingGraphs();
		//comparer.generateMinMaxGraphs();
		 */
		
	}
}
