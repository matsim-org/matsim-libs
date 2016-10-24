package playground.santiago.utils;

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
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;



public class SamplingAgentsFromPop {
	


	final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	final String runsSampledDir = "../../../runs-svn/santiago/TMP/";

	final String expandedPlansFolder = svnWorkingDir + "inputForMATSim/plans/2_10pct/";
	final String expandedPlansFile  = expandedPlansFolder + "randomized_expanded_plans.xml.gz";
	
	
	final String sampledPlansFolder = svnWorkingDir + "inputForMATSim/plans/3_1pct/";
	final String sampledPlansFile   = sampledPlansFolder + "randomized_sampled_plans.xml.gz";
		
	final String configFolder = svnWorkingDir + "inputForMATSim/";	
	final String expandedConfigFile = configFolder + "randomized_expanded_config.xml";
	final String sampledConfigFile  = configFolder + "randomized_sampled_config.xml";
	
			
	
	
	
	final static double ORIGINAL_PERCENTAGE = 0.1; /* This is the original sample rate from expanded_plans_final.xml.gz*/
	final static double SAMPLED_PERCENTAGE = 0.1; /* This is the percentage that is desired to extract*/ 

	

	
	void sampling( double sampledPercentage ) {

		


		Scenario scenarioTmp = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioTmp).readFile(expandedPlansFile);
		
		Population pop = scenarioTmp.getPopulation();

		Population newPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation() ;
		
		for ( Person person : pop.getPersons().values() ) {
			if ( Math.random() < sampledPercentage ) {
				System.out.println("adding person...");
				newPop.addPerson(person);
			}
		}
		
	
		PopulationWriter popwriter = new PopulationWriter(newPop) ;
		popwriter.write( sampledPlansFile );

		System.out.println("done.");
	}

	void changeAndWriteNewConfigFile () {
		
		double finalSampleRate = Math.floor( ORIGINAL_PERCENTAGE * SAMPLED_PERCENTAGE * 1000) / 1000 ;
		Config config = ConfigUtils.loadConfig( expandedConfigFile );		

		QSimConfigGroup qsim = config.qsim();
		qsim.setFlowCapFactor(finalSampleRate);
		double storageCapFactor = Math.ceil( ( ( finalSampleRate / ( Math.pow ( finalSampleRate , 0.25 ) ) ) ) * 1000 ) / 1000;
		qsim.setStorageCapFactor(storageCapFactor);

	

		CountsConfigGroup counts = config.counts();
		counts.setCountsScaleFactor( Math.pow( finalSampleRate , -1 ) );

		
		
		ControlerConfigGroup cc = config.controler();
		cc.setOutputDirectory(runsSampledDir + "output/" );


		counts.setInputFile(runsSampledDir + "input/counts_merged_VEH_C01.xml" );

		NetworkConfigGroup net = config.network();
		net.setInputFile(runsSampledDir + "input/network_merged_cl.xml.gz" );
		
		PlansConfigGroup plans = config.plans();

		plans.setInputFile( runsSampledDir + "input/randomized_sampled_plans.xml.gz" );
		plans.setInputPersonAttributeFile( runsSampledDir + "input/sampledAgentAttributes.xml");
		
		TransitConfigGroup transit = config.transit();
		transit.setTransitScheduleFile(runsSampledDir + "input/transitschedule_simplified.xml" );
		transit.setVehiclesFile(runsSampledDir + "input/transitvehicles.xml" );
		
	
		

		
		new ConfigWriter(config).write( sampledConfigFile );
		
		

	}
	

	void run() {
		
		sampling( SAMPLED_PERCENTAGE );
		changeAndWriteNewConfigFile ( );
		
	}


	public static void main(final String[] args) {
		SamplingAgentsFromPop app = new SamplingAgentsFromPop();		
		app.run();
		
		
	}


}