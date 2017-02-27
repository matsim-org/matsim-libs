package saleem.gaming.scenariobuilding;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;
/**
 * A class used to execute different gaming scenarios,
 * the code below focusses on all scenarios for Farsta Centrum area.
 * Change the corresponding config files for other areas.
 * 
 * @author Mohammad Saleem
 */
public class GamingScenariosRunner {
	public static void main(String[] args) {
		{
			/* Farsta Centrum area, Max allowed people moved into the area, 
			 * All Employed, 10 PC demand simulated, Max PT measure
			 */
			String path = "./FarstaCentrum/configFarstaCentrumEmployed10pcMax.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;//10 %

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		{
			/* Farsta Centrum area, Max allowed people moved into the area, 
			 * All Employed, 10 PC demand simulated, Min PT measure
			 */
			String path = "./FarstaCentrum/configFarstaCentrumEmployed10pcMin.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		/* Farsta Centrum area, Max allowed people moved into the area, 
		 * All UnEmployed, 10 PC demand simulated, Max PT measure
		 */
		{
			String path = "./FarstaCentrum/configFarstaCentrumUnEmployed10pcMax.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		/* Farsta Centrum area, Max allowed people moved into the area, 
		 * All UnEmployed, 10 PC demand simulated, Min PT measure
		 */
		{
			String path = "./FarstaCentrum/configFarstaCentrumUnEmployed10pcMin.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		/* Farsta Centrum area, Zero people moved into the area, 
		 * 10 PC demand simulated, Max PT measure
		 */
		{
			String path = "./FarstaCentrum/configFarstaCentrumPlain10pcMax.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		/* Farsta Centrum area, Zero people moved into the area, 
		 * 10 PC demand simulated, Min PT measure
		 */
		{
			String path = "./FarstaCentrum/configFarstaCentrumPlain10pcMin.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		/* Farsta Centrum area, Max allowed people moved into the area, 
		 * Mixed employment, 10 PC demand simulated, Max PT measure
		 */
		{
			String path = "./FarstaCentrum/configFarstaCentrumMixed10pcMax.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		/* Farsta Centrum area, Max allowed people moved into the area, 
		 * Mixed employment, 10 PC demand simulated, Min PT measure
		 */
		{
			String path = "./FarstaCentrum/configFarstaCentrumMixed10pcMin.xml";
		    Config config = ConfigUtils.loadConfig(path);
		    final Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
		    double samplesize = 0.1;

		    // Changing vehicle and road capacity according to sample size
			PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
			capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
			
			Network network = scenario.getNetwork();
			TransitSchedule schedule = scenario.getTransitSchedule();
			new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}		
		
	}
}
