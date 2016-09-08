package saleem.stockholmscenario.teleportation.gaming;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import saleem.stockholmscenario.teleportation.PTCapacityAdjusmentPerSample;

public class GamingScenariosRunner {
	public static void main(String[] args) {
		{
			String path = "./FarstaCentrum/configFarstaCentrumEmployed10pcMax.xml";
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
		{
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}
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
			
//			NetworkWriter networkWriter =  new NetworkWriter(network);
//			networkWriter.write("./ihop2/Plain/PlainNetwork.xml");
			
			controler.addControlerListener(new FareControlListener());
			controler.run();
		}		
		
	}
}
