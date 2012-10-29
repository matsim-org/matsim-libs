package freight;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.algorithms.CarrierAlgorithm;
import org.matsim.contrib.freight.algorithms.VehicleRouter;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class ExampleFreight {
	
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("input/grid10.xml");
		Network network = scenario.getNetwork();
		
		//create customer locations
		Id depot = getLinkIdFromCoord(5000,5000,network);
		Id customer1 = getLinkIdFromCoord(2000,2500,network);
		Id customer2 = getLinkIdFromCoord(2000,2000,network);
		Id customer3 = getLinkIdFromCoord(1500,5000,network);
		Id customer4 = getLinkIdFromCoord(9000,7000,network);
		Id customer5 = getLinkIdFromCoord(9000,8000,network);
		
		//create carrier and shipments
		CarrierFactory carrierFactory = new CarrierFactory();
		
		Carrier myCarrier = carrierFactory.createCarrier("myCarrier", depot.toString());
		myCarrier.setCarrierCapabilities(carrierFactory.createCapabilities());
		
		//create 5 vehicles
		for(int i=0;i<5;i++){
			/*
			 * do vehicles really need a type necessarily?
			 */
			CarrierVehicle vehicle = carrierFactory.createVehicle("vehicle_"+i, depot.toString(), 30, carrierFactory.createDefaultVehicleType());
			myCarrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
		}
		
		//create shipments
		CarrierShipment s1 = carrierFactory.createShipment(depot, customer1, 5, carrierFactory.createTimeWindow(0.0, 24*3600), carrierFactory.createTimeWindow(0.0, 24*3600));
		myCarrier.getShipments().add(s1);
		
		CarrierShipment s2 = carrierFactory.createShipment(depot, customer2, 5, carrierFactory.createTimeWindow(0.0, 24*3600), carrierFactory.createTimeWindow(0.0, 24*3600));
		myCarrier.getShipments().add(s2);
		
		CarrierShipment s3 = carrierFactory.createShipment(depot, customer3, 10, carrierFactory.createTimeWindow(0.0, 24*3600), carrierFactory.createTimeWindow(0.0, 24*3600));
		myCarrier.getShipments().add(s3);
		
		CarrierShipment s4 = carrierFactory.createShipment(depot, customer4, 10, carrierFactory.createTimeWindow(0.0, 24*3600), carrierFactory.createTimeWindow(0.0, 24*3600));
		myCarrier.getShipments().add(s4);
		
		CarrierShipment s5 = carrierFactory.createShipment(depot, customer5, 5, carrierFactory.createTimeWindow(0.0, 24*3600), carrierFactory.createTimeWindow(0.0, 24*3600));
		myCarrier.getShipments().add(s5);

		
		//setup carrierAlgorithm - vehicle routing distribution problem solver
		TravelDisutility onlyDistanceMatters = getTravelDisutility();
		TravelTime timeWithFreeSpeed = getTravelTime();
		
		CarrierAlgorithm vehicleRouter = new VehicleRouter(network, onlyDistanceMatters, timeWithFreeSpeed);
		vehicleRouter.run(myCarrier);

		//write plans
		Carriers carriers = carrierFactory.createCarriers();
		carriers.getCarriers().put(myCarrier.getId(), myCarrier);
		new CarrierPlanWriter(carriers.getCarriers().values()).write("output/myCarrierPlans.xml");

	}

	private static TravelTime getTravelTime() {
		return new TravelTime() {
			
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
				return link.getLength()/link.getFreespeed();
			}
		};
	}

	private static TravelDisutility getTravelDisutility() {
		return new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, org.matsim.vehicles.Vehicle vehicle) {
				return link.getLength();
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
	}

	

	private static Id getLinkIdFromCoord(int i, int j, Network network) {
		Coord coord = new CoordImpl(i,j);
		return ((NetworkImpl) network).getNearestLink(coord).getId();
	}

}
