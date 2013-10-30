package usecases.chessboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

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
			IdImpl homeId = new IdImpl("i("+i+",9)R");
			Carrier carrier = CarrierImpl.newInstance(new IdImpl(agentCounter));
			createFleet(homeId, carrier);
			createCustomers(carrier,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier);

			IdImpl homeIdR = new IdImpl("i("+i+",0)");
			Carrier carrier_ = CarrierImpl.newInstance(new IdImpl(agentCounter));
			createFleet(homeIdR, carrier_);
			createCustomers(carrier_,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier_);
		}
		
		for(int i=1;i<10;i++){
			IdImpl homeId = new IdImpl("j(0,"+i+")R");
			Carrier carrier = CarrierImpl.newInstance(new IdImpl(agentCounter));
			createFleet(homeId, carrier);
			createCustomers(carrier,scenario.getNetwork());
			agentCounter++;
			carriers.addCarrier(carrier);
			
			IdImpl homeIdR = new IdImpl("j(9,"+i+")");
			Carrier carrier_ = CarrierImpl.newInstance(new IdImpl(agentCounter));
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
		List<Id> innerCityLinks = createInnerCityLinks(network);
		List<Id> outerCityLinks = createOuterCityLinks(network);
		
		for(int i=0;i<50;i++){
			CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(new IdImpl(i+1), drawLocationLinkId(innerCityLinks,outerCityLinks));
			serviceBuilder.setCapacityDemand(1);
			serviceBuilder.setServiceDuration(5*60);
			serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(4*60*60, 10*60*60));
			carrier.getServices().add(serviceBuilder.build());
		}
	}

	private static Id drawLocationLinkId(List<Id> innerCityLinks, List<Id> outerCityLinks) {
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

	private static List<Id> createOuterCityLinks(Network network) {
		List<Id> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
		List<Id> outer = new ArrayList<Id>();
		for(Id id : network.getLinks().keySet()){
			if(!inner.contains(id)){
				outer.add(id);
			}
		}
		return outer;
	}

	private static List<Id> createInnerCityLinks(Network network) {
		List<Id> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
		List<Id> innerCityLinkIds = new ArrayList<Id>();
		for(Id id : inner){
			if(network.getLinks().containsKey(id)){
				innerCityLinkIds.add(id);
			}
		}
		return innerCityLinkIds;
	}

	private static void createFleet(IdImpl homeId, Carrier carrier) {
		carrier.getCarrierCapabilities().getCarrierVehicles().add(getHeavyVehicle(carrier.getId(),homeId));
		carrier.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(carrier.getId(),homeId));
		carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
	}

	private static CarrierVehicle getLightVehicle(Id id, IdImpl homeId) {
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(new IdImpl("carrier_"+id.toString()+"_lightVehicle"), homeId);
		vBuilder.setEarliestStart(6*60*60);
		vBuilder.setLatestEnd(6*60*60 + 10*60*60);
		vBuilder.setType(createLightType());
		return vBuilder.build();
	}

	private static CarrierVehicleType createLightType() {
		CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(new IdImpl("light"));
		typeBuilder.setCapacity(5);
		typeBuilder.setFixCost(84.0);
		typeBuilder.setCostPerDistanceUnit(0.00047);
		typeBuilder.setCostPerTimeUnit(0.008);
		return typeBuilder.build();
	}

	private static CarrierVehicle getHeavyVehicle(Id id, IdImpl homeId) {
		CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(new IdImpl("carrier_"+id.toString()+"_heavyVehicle"), homeId);
		vBuilder.setEarliestStart(6*60*60);
		vBuilder.setLatestEnd(6*60*60 + 10*60*60);
		vBuilder.setType(createHeavyType());
		return vBuilder.build();
	}

	private static CarrierVehicleType createHeavyType() {
		CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(new IdImpl("heavy"));
		typeBuilder.setCapacity(10);
		typeBuilder.setFixCost(130.0);
		typeBuilder.setCostPerDistanceUnit(0.00077);
		typeBuilder.setCostPerTimeUnit(0.008);
		return typeBuilder.build();
	}
	
	

}
