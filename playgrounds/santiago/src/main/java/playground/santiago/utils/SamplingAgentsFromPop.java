package playground.santiago.utils;

import java.io.File;
import java.util.SortedMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.santiago.population.ActivityClassifier;



public class SamplingAgentsFromPop {
	
	///Fields///
	
	final static String NET_FILENAME = "../../../runs-svn/santiago/casoBase10_NP/input/network_merged_cl.xml.gz";
	
	final static String PATH_FOR_SAMPLED_INPUT = "../../../runs-svn/santiago/casoBase10_NP/input/new-input/";
	final static String INPUT_POP_FILENAME = PATH_FOR_SAMPLED_INPUT + "expanded_plans_final_1.xml.gz";	
	final static String INPUT_CONFIG_FILENAME = PATH_FOR_SAMPLED_INPUT + "expanded_config_final.xml";
	final static String OUTPUT_POP_FILENAME = PATH_FOR_SAMPLED_INPUT + "sampled_plans_final_1.xml.gz";
	
	final static double ORIGINAL_PERCENTAGE = 0.1; /* This is the original sample rate from expanded_plans_final.xml.gz i.e. " x % " */
	final static double SAMPLED_PERCENTAGE = 0.1; /* This is the percentage that is desired to extract from a "x %" expanded_plans_final.xml.gz */ 

	///////////
	
	void sampling( String input_pop , String input_net , String output_pop , double sampledPercentage ) {

		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( input_net ) ;
		config.plans().setInputFile( input_pop ) ;

		Population pop = ScenarioUtils.loadScenario(config).getPopulation() ;

		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
		
		for ( Person person : pop.getPersons().values() ) {
			if ( Math.random() < sampledPercentage ) {
				System.out.println("adding person...");
				newPop.addPerson(person);
			}
		}
		

		PopulationWriter popwriter = new PopulationWriter(newPop,ScenarioUtils.loadScenario(config).getNetwork()) ;
		popwriter.write( output_pop ) ;

		System.out.println("done.");
	}

	void changeAndWriteNewConfigFile (String pathFromSampledInput,
			String configFile , double originalPercentage , double sampledPercentage , String outputPopFile){
		
		double finalSampleRate = Math.floor( originalPercentage * sampledPercentage * 1000) / 1000 ;
		
		Config oldConfig = ConfigUtils.loadConfig(configFile);		
		/*QSim stuffs*/		
		QSimConfigGroup qsim = oldConfig.qsim();
		//The capacity factor is equal to the percentage used in the clonePersons method.
		qsim.setFlowCapFactor(finalSampleRate);
		//storageCapFactor obtained by expression proposed in Nicolai and Nagel, 2013.
		double storageCapFactor = Math.ceil( ( ( 0.1 / ( Math.pow ( finalSampleRate , 0.25 ) ) ) ) *100 ) / 100;
		qsim.setStorageCapFactor(storageCapFactor);
		////////////////////////////////////////////////////////////////////////
		
		/*Path to new plans file*/		
		PlansConfigGroup plans = oldConfig.plans();
		plans.setInputFile( outputPopFile );
		////////////////////////////////////////////////////////////////////////
	
		/*Counts stuffs*/
		CountsConfigGroup counts = oldConfig.counts();
		counts.setCountsScaleFactor( Math.pow( finalSampleRate , -1 ) );
		////////////////////////////////////////////////////////////////////////
		
		
		/*Write the new config_file*/

		new ConfigWriter(oldConfig).write( pathFromSampledInput + "sampled_config_final.xml");
		////////////////////////////////////////////////////////////////////////
	}
	
	
	void run() {
		sampling( INPUT_POP_FILENAME , NET_FILENAME , OUTPUT_POP_FILENAME , SAMPLED_PERCENTAGE );
		changeAndWriteNewConfigFile ( PATH_FOR_SAMPLED_INPUT , INPUT_CONFIG_FILENAME , ORIGINAL_PERCENTAGE , SAMPLED_PERCENTAGE , OUTPUT_POP_FILENAME);
	}

	
	

	public static void main(final String[] args) {
		SamplingAgentsFromPop app = new SamplingAgentsFromPop();		
		app.run();
		
		
	}




}