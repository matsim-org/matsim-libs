package playground.santiago.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;



public class SamplingAgentsFromPop {
	
	///Fields///
	
	final static String INPUT_POP_FILENAME = "../../../runs-svn/santiago/casoBase5_NP/input/new-input/expanded_plans_final.xml.gz";
	final static String OUTPUT_POP_FILENAME = "../../../runs-svn/santiago/casoBase5_NP/input/new-input/sampled_plans_final.xml.gz";
	final static String NET_FILENAME = "../../../runs-svn/santiago/casoBase5_NP/input/network_merged_cl.xml.gz";
	
	void run( String input_pop , String input_net , String output_pop ) {

		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( input_net ) ;
		config.plans().setInputFile( input_pop ) ;

		Population pop = ScenarioUtils.loadScenario(config).getPopulation() ;

		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
		
		for ( Person person : pop.getPersons().values() ) {
			if ( Math.random() < 0.1 ) {
				System.out.println("adding person...");
				newPop.addPerson(person);
			}
		}
		

		PopulationWriter popwriter = new PopulationWriter(newPop,ScenarioUtils.loadScenario(config).getNetwork()) ;
		popwriter.write( output_pop ) ;

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		SamplingAgentsFromPop app = new SamplingAgentsFromPop();
		app.run( INPUT_POP_FILENAME , NET_FILENAME , OUTPUT_POP_FILENAME );
	}




}