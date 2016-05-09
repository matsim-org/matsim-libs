package saleem.stockholmscenario.teleportation;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleType;

public class StockholmScenarioSimulation {
public static void main(String[] args) {
	
/*	Before running the simulation, this function reads the Config file, gets the storage capacity factor to calculate the sample size, and then sets the sitting capacity, 
 * standing capacity and passenger car equivalents of the vehicle types based on the sample size. This is done to balance out the effect of setting storage capacity 
 * factor and flow capacity factor (in the Config file) on the PT links.
*/
//       System.out.println("STARTED ...");
	
//	String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
	String path = "/home/saleem/input/config.xml";
    Config config = ConfigUtils.loadConfig(path);
    final Scenario scenario = ScenarioUtils.loadScenario(config);
	Controler controler = new Controler(scenario);
    double samplesize = config.qsim().getStorageCapFactor();
	
	// Changing vehicle and road capacity according to sample size
	PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
	capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
	
	Network network = scenario.getNetwork();
	TransitSchedule schedule = scenario.getTransitSchedule();
	new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//	NetworkWriter networkWriter =  new NetworkWriter(network);
//	networkWriter.write("/home/saleem/input/PseudoNetwork.xml");
//	networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoNetwork.xml");
	controler.run();
	
}
public static ArrayList<VehicleType> toArrayList(Iterator<VehicleType> iter){
	ArrayList<VehicleType> arraylist = new ArrayList<VehicleType>();
	while(iter.hasNext()){
		arraylist.add(iter.next());
	}
	return arraylist;
}
}
