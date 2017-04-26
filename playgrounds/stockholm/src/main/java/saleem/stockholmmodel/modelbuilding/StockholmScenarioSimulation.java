package saleem.stockholmmodel.modelbuilding;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleType;

import saleem.ptoptimisation.decisionvariables.TransitScheduleAdapter;

/**
 * Execution class for StockholmPTCar model. Before running the simulation, the main function reads the Config file, 
 * gets the storage capacity factor to calculate the sample size, and then sets the sitting capacity, 
 * standing capacity and passenger car equivalents of the vehicle types based on the sample size. 
 * This is neccessary if a smaller than 100% demand sample is executed, in the same way as setting storage capacity and 
 * flow capacity (in the Config file) of the network links.
 * 
 * @author Mohammad Saleem
 */
public class StockholmScenarioSimulation {
public static void main(String[] args) {
	
	String path = "./ihop2/matsim-input/config.xml";
	
    Config config = ConfigUtils.loadConfig(path);
    final Scenario scenario = ScenarioUtils.loadScenario(config);
	Controler controler = new Controler(scenario);
	//If due to network cleanup, the network has to be scaled up, then set sample size here manually, according to population sample size
    double samplesize = config.qsim().getStorageCapFactor();	// Changing vehicle and road capacity according to sample size
	PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
	capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);

	Network network = scenario.getNetwork();
	TransitSchedule schedule = scenario.getTransitSchedule();
	new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
	controler.run();
	
	}
}
