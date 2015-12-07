package saleem.stockholmscenario.teleportation;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import saleem.stockholmscenario.teleportation.ptoptimisation.PTControlListener;

public class StockholmScenarioSimulation {
public static void main(String[] args) {
	
/*	Before running the simulation, this function reads the Config file, gets the storage capacity factor to calculate the sample size, and then sets the sitting capacity, 
 * standing capacity and passenger car equivalents of the vehicle types based on the sample size. This is done to balance out the effect of setting storage capacity 
 * factor and flow capacity factor (in the Config file) on the PT links.
*/
//        String path = "/home/saleem/input/config.xml";
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\config.xml";
        Config config = ConfigUtils.loadConfig(path);
		double samplesize = config.qsim().getStorageCapFactor();
//        double samplesize = 0.1;
        Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();
		controler.addControlerListener(new PTControlListener(scenario));
		Network network = scenario.getNetwork();
		Vehicles vehicles = scenario.getTransitVehicles();
		ArrayList<VehicleType> vehcilestypes = toArrayList(vehicles.getVehicleTypes().values().iterator());
		Iterator vehtypes = vehcilestypes.iterator();
		while(vehtypes.hasNext()){
			VehicleType vt = (VehicleType)vehtypes.next();
			VehicleCapacity cap = vt.getCapacity();
			cap.setSeats((int)Math.ceil(cap.getSeats()*samplesize));
			cap.setStandingRoom((int)Math.ceil(cap.getStandingRoom()*samplesize));
			vt.setCapacity(cap);
			vt.setPcuEquivalents(vt.getPcuEquivalents()*samplesize);
			System.out.println("Sample Size is: " + samplesize);
		}
		TransitSchedule schedule = scenario.getTransitSchedule();
		//network.getLinks().clear();
		//network.getNodes().clear();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//		Iterator iter = network.getLinks().values().iterator();
//		while(iter.hasNext()){
//			Link link = (Link)iter.next();
//			if(link.getId().toString().startsWith("tr")){
////				link.setCapacity(link.getCapacity()/config.qsim().getFlowCapFactor());
//			}
//		}
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoNetwork.xml");
//		TransitScheduleWriter tw = new TransitScheduleWriter(schedule);
//		tw.writeFile("H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\PseudoSchedule.xml");

		controler.run();
		/*Config config = ConfigUtils.loadConfig(path, new MatrixBasedPtRouterConfigGroup());

        //fetching relevant groups ot of the config
        MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);
        PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();


        //setting up scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Bounding Box for PT Matrix - may be scaled down to a smaller area
        BoundingBox nbb = BoundingBox.createBoundingBox(scenario.getNetwork());
        // setting up PT Matrix
        PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, nbb, mbpcg);

        //and finally setting up the controler
        Controler controler = new Controler(config);
        // setting up routing 
        controler.setTripRouterFactory( new MatrixBasedPtRouterFactoryImpl(controler.getScenario(), ptMatrix) ); // the car and pt router

        controler.run();*/
	}
public static ArrayList<VehicleType> toArrayList(Iterator<VehicleType> iter){
	ArrayList<VehicleType> arraylist = new ArrayList<VehicleType>();
	while(iter.hasNext()){
		arraylist.add(iter.next());
	}
	return arraylist;
}
}
