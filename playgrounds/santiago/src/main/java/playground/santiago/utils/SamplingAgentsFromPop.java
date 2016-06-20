package playground.santiago.utils;

import java.io.File;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;

import playground.santiago.population.SantiagoScenarioBuilder;



public class SamplingAgentsFromPop {
	
	///Fields///
	private static final Logger log = Logger.getLogger(SantiagoScenarioBuilder.class);

	
	final static String ORIGINAL_FILES_PATH = "../../../runs-svn/santiago/basecase1/";
	final static String POLICY_FILES_PATH	= "../../../runs-svn/santiago/policy1/";
			
	final static String ORIGINAL_PLANS  = ORIGINAL_FILES_PATH + "input/new-input/randomized_expanded_plans_final.xml.gz";
	final static String ORIGINAL_CONFIG = ORIGINAL_FILES_PATH + "input/new-input/randomized_expanded_config_final.xml";
			
	final static String NETWORK_FILE    = POLICY_FILES_PATH + "input/network_merged_cl.xml.gz";
	final static String SAMPLED_PLANS   = POLICY_FILES_PATH + "input/new-input/randomized_sampled_plans_final.xml.gz";
	final static String SAMPLED_CONFIG  = POLICY_FILES_PATH + "input/new-input/randomized_sampled_config_final.xml";
	
	final static double ORIGINAL_PERCENTAGE = 0.1; /* This is the original sample rate from expanded_plans_final.xml.gz i.e. " x % " */
	final static double SAMPLED_PERCENTAGE = 0.1; /* This is the percentage that is desired to extract from a "x %" expanded_plans_final.xml.gz */ 

	
	///////////
	
	void sampling( double sampledPercentage ) {

		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( NETWORK_FILE ) ;
		config.plans().setInputFile( ORIGINAL_PLANS ) ;

		Population pop = ScenarioUtils.loadScenario(config).getPopulation() ;

		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
		
		for ( Person person : pop.getPersons().values() ) {
			if ( Math.random() < sampledPercentage ) {
				System.out.println("adding person...");
				newPop.addPerson(person);
			}
		}
		
		File newInput = new File( POLICY_FILES_PATH + "new-input/" );
		if(!newInput.exists()) newInput.mkdirs();	
		
		String tmp = POLICY_FILES_PATH + "new-input/";
		PopulationWriter popwriter = new PopulationWriter(newPop,ScenarioUtils.loadScenario(config).getNetwork()) ;
		popwriter.write( tmp + "randomized_sampled_plans_final.xml.gz" );

		System.out.println("done.");
	}

	void changeAndWriteNewConfigFile ( double originalPercentage , double sampledPercentage ){
		
		double finalSampleRate = Math.floor( originalPercentage * sampledPercentage * 1000) / 1000 ;	
		
		Config config = ConfigUtils.loadConfig( ORIGINAL_CONFIG );		

		QSimConfigGroup qsim = config.qsim();
		qsim.setFlowCapFactor(finalSampleRate);
		double storageCapFactor = Math.ceil( ( ( finalSampleRate / ( Math.pow ( finalSampleRate , 0.25 ) ) ) ) * 1000 ) / 1000;
		qsim.setStorageCapFactor(storageCapFactor);

	

		CountsConfigGroup counts = config.counts();
		counts.setCountsScaleFactor( Math.pow( finalSampleRate , -1 ) );

		
		
		ControlerConfigGroup cc = config.controler();
		cc.setOutputDirectory(POLICY_FILES_PATH + "output/" );


		counts.setCountsFileName(POLICY_FILES_PATH + "input/counts_merged_VEH_C01.xml" );

		NetworkConfigGroup net = config.network();
		net.setInputFile(POLICY_FILES_PATH + "input/network_merged_cl.xml.gz" );
		
		PlansConfigGroup plans = config.plans();
		plans.setInputPersonAttributeFile(POLICY_FILES_PATH + "input/agentAttributes.xml" );
		plans.setInputFile( SAMPLED_PLANS );
		
		TransitConfigGroup transit = config.transit();
		transit.setTransitScheduleFile(POLICY_FILES_PATH + "input/transitschedule_simplified.xml" );
		transit.setVehiclesFile(POLICY_FILES_PATH + "input/transitvehicles.xml" );
		
		File newInput = new File( POLICY_FILES_PATH + "new-input/" );
		if(!newInput.exists()) newInput.mkdirs();	
		
		String tmp = POLICY_FILES_PATH + "new-input/" ;
		
		new ConfigWriter(config).write( tmp +"randomized_sampled_config_final.xml" );
		
		

	}
	

	void run() {
		
		sampling( SAMPLED_PERCENTAGE );
		changeAndWriteNewConfigFile ( ORIGINAL_PERCENTAGE , SAMPLED_PERCENTAGE );
		
	}


	public static void main(final String[] args) {
		SamplingAgentsFromPop app = new SamplingAgentsFromPop();		
		app.run();
		
		
	}




}