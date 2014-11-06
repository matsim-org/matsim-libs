package usecases.chessboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * Creates chessboard freight scenario.
 * 
 * @author stefan
 *
 */
public class FreightScenarioCreator {
	
	static int agentCounter = 1;
	static Random random = new Random(Long.MAX_VALUE);
	
	public static void main(String[] args) {
		
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("input/usecases/chessboard/network/grid9x9.xml");
		
		
		
		//carriers
		Carriers carriers = new Carriers();
		
		for(int i=1;i<10;i++){
			Id<Link> homeId = Id.create("i("+i+",9)R", Link.class);
			Carrier carrier = CarrierImpl.newInstance(Id.create(agentCounter, Carrier.class));
			createFleet(homeId, carrier);
			createCustomers(carrier,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier);

			Id<Link> homeIdR = Id.create("i("+i+",0)", Link.class);
			Carrier carrier_ = CarrierImpl.newInstance(Id.create(agentCounter, Carrier.class));
			createFleet(homeIdR, carrier_);
			createCustomers(carrier_,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier_);
		}
		
		for(int i=1;i<10;i++){
			Id<Link> homeId = Id.create("j(0,"+i+")R", Link.class);
			Carrier carrier = CarrierImpl.newInstance(Id.create(agentCounter, Carrier.class));
			createFleet(homeId, carrier);
			createCustomers(carrier,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier);
			
			Id<Link> homeIdR = Id.create("j(9,"+i+")", Link.class);
			Carrier carrier_ = CarrierImpl.newInstance(Id.create(agentCounter, Carrier.class));
			createFleet(homeIdR, carrier_);
			createCustomers(carrier_,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier_);
		}
		
		CarrierVehicleTypes types = CarrierVehicleTypes.getVehicleTypes(carriers);
		
		new CarrierVehicleTypeWriter(types).write("input/usecases/chessboard/freight/vehicleTypes.xml");
		new CarrierPlanXmlWriterV2(carriers).write("input/usecases/chessboard/freight/carrierPlansWithoutRoutes.xml");
	}

	private static void createCustomers(Carrier carrier, Network network) {
		List<Id<Link>> innerCityLinks = createInnerCityLinks(network);
		List<Id<Link>> outerCityLinks = createOuterCityLinks(network);
		
		for(int i=0;i<50;i++){
			CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create(i+1, CarrierService.class), drawLocationLinkId(innerCityLinks,outerCityLinks));
			serviceBuilder.setCapacityDemand(1);
			serviceBuilder.setServiceDuration(5*60);
			serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(4*60*60, 10*60*60));
			carrier.getServices().add(serviceBuilder.build());
		}
	}

	private static Id<Link> drawLocationLinkId(List<Id<Link>> innerCityLinks, List<Id<Link>> outerCityLinks) {
		double probInner = 2.0/3.0;
		double randomFigure = random.nextDouble();
		if(randomFigure <= probInner){
			int randomIndex = random.nextInt(innerCityLinks.size());
			return innerCityLinks.get(randomIndex);
		}
		else{
			int randomIndex = random.nextInt(outerCityLinks.size());
			return outerCityLinks.get(randomIndex);
		}
	}

	private static List<Id<Link>> createOuterCityLinks(Network network) {
		List<Id<Link>> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
		List<Id<Link>> outer = new ArrayList<>();
		for(Id<Link> id : network.getLinks().keySet()){
			if(!inner.contains(id)){
				outer.add(id);
			}
		}
		return outer;
	}

	private static List<Id<Link>> createInnerCityLinks(Network network) {
		List<Id<Link>> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
		List<Id<Link>> innerCityLinkIds = new ArrayList<>();
		for(Id<Link> id : inner){
			if(network.getLinks().containsKey(id)){
				innerCityLinkIds.add(id);
			}
		}
		return innerCityLinkIds;
	}

	private static void createFleet(Id<Link> homeId, Carrier carrier) {
		carrier.getCarrierCapabilities().getCarrierVehicles().add(getHeavyVehicle(carrier.getId(),homeId));
		carrier.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(carrier.getId(),homeId));
		carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
	}

	private static CarrierVehicle getLightVehicle(Id id, Id<Link> homeId) {
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create("carrier_"+id.toString()+"_lightVehicle", Vehicle.class), homeId);
		vBuilder.setEarliestStart(6*60*60);
		vBuilder.setLatestEnd(6*60*60 + 10*60*60);
		vBuilder.setType(createLightType());
		return vBuilder.build();
	}

	private static CarrierVehicleType createLightType() {
		CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("light", VehicleType.class));
		typeBuilder.setCapacity(5);
		typeBuilder.setFixCost(84.0);
		typeBuilder.setCostPerDistanceUnit(0.00047);
		typeBuilder.setCostPerTimeUnit(0.008);
		return typeBuilder.build();
	}

	private static CarrierVehicle getHeavyVehicle(Id id, Id<Link> homeId) {
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create("carrier_"+id.toString()+"_heavyVehicle", Vehicle.class), homeId);
		vBuilder.setEarliestStart(6*60*60);
		vBuilder.setLatestEnd(6*60*60 + 10*60*60);
		vBuilder.setType(createHeavyType());
		return vBuilder.build();
	}

	private static CarrierVehicleType createHeavyType() {
		CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("heavy", VehicleType.class));
		typeBuilder.setCapacity(10);
		typeBuilder.setFixCost(130.0);
		typeBuilder.setCostPerDistanceUnit(0.00077);
		typeBuilder.setCostPerTimeUnit(0.008);
		return typeBuilder.build();
	}
	
	

}
