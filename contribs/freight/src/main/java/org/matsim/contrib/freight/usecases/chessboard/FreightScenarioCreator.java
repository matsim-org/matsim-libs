package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates chessboard freight scenario.
 *
 * @author stefan
 *
 */
final class FreightScenarioCreator {

    static int agentCounter = 1;
    static Random random = new Random(Long.MAX_VALUE);

    public static void main(String[] args) {

        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input/usecases/chessboard/network/grid9x9.xml");



        //carriers
        Carriers carriers = new Carriers();

        for(int i=1;i<10;i++){
            Id<Link> homeId = Id.createLinkId("i("+i+",9)R");
            Carrier carrier = CarrierImpl.newInstance(Id.create(agentCounter,Carrier.class));
            createFleet(homeId, carrier);
            createCustomers(carrier,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier);

            Id<Link> homeIdR = Id.createLinkId("i("+i+",0)");
            Carrier carrier_ = CarrierImpl.newInstance(Id.create(agentCounter,Carrier.class));
            createFleet(homeIdR, carrier_);
            createCustomers(carrier_,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier_);
        }

        for(int i=1;i<10;i++){
            Id<Link> homeId = Id.createLinkId("j(0,"+i+")R");
            Carrier carrier = CarrierImpl.newInstance(Id.create(agentCounter,Carrier.class));
            createFleet(homeId, carrier);
            createCustomers(carrier,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier);

            Id<Link> homeIdR = Id.createLinkId("j(9,"+i+")");
            Carrier carrier_ = CarrierImpl.newInstance(Id.create(agentCounter,Carrier.class));
            createFleet(homeIdR, carrier_);
            createCustomers(carrier_,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier_);
        }

//        CarrierVehicleTypes types = CarrierVehicleTypes.getVehicleTypes(carriers);
//        new CarrierVehicleTypeWriter(types).write("input/usecases/chessboard/freight/vehicleTypes.xml");
        
        new CarrierPlanXmlWriterV2(carriers).write("input/usecases/chessboard/freight/multipleCarriers_withoutTW_withDepots_withoutPlan.xml");
    }

    private static void createCustomers(Carrier carrier, Network network) {
        List<Id<Link>> innerCityLinks = createInnerCityLinks(network);
        List<Id<Link>> outerCityLinks = createOuterCityLinks(network);

        for(int i=0;i<20;i++){
            CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create((i + 1),CarrierService.class), drawLocationLinkId(innerCityLinks, outerCityLinks));
            serviceBuilder.setCapacityDemand(1);
            serviceBuilder.setServiceDuration(5*60);
            serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(6*60*60, 15*60*60));
            carrier.getServices().add(serviceBuilder.build());
        }
    }

    private static Id<Link> drawLocationLinkId(List<Id<Link>> innerCityLinks, List<Id<Link>> outerCityLinks) {
        double probInner = 0.5;
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
        List<Id<Link>> outer = new ArrayList<Id<Link>>();
        for(Id<Link> id : network.getLinks().keySet()){
            if(!inner.contains(id)){
                outer.add(id);
            }
        }
        return outer;
    }

    private static List<Id<Link>> createInnerCityLinks(Network network) {
        List<Id<Link>> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
        List<Id<Link>> innerCityLinkIds = new ArrayList<Id<Link>>();
        for(Id<Link> id : inner){
            if(network.getLinks().containsKey(id)){
                innerCityLinkIds.add(id);
            }
        }
        return innerCityLinkIds;
    }

    private static void createFleet(Id<Link> homeId, Carrier carrier) {
        Id<Link> oppositeId = getOpposite(homeId);

        //light
        carrier.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(carrier.getId(), homeId, "a"));
        carrier.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(carrier.getId(), oppositeId, "b"));

        //heavy
        carrier.getCarrierCapabilities().getCarrierVehicles().add(getHeavyVehicle(carrier.getId(), homeId, "a"));
        carrier.getCarrierCapabilities().getCarrierVehicles().add(getHeavyVehicle(carrier.getId(), oppositeId, "b"));

        carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
    }

    public static Id<Link> getOpposite(Id<Link> homeId) {
        if(homeId.toString().startsWith("i")){
            String opposite = "i(" + homeId.toString().substring(2,4);
            if(homeId.toString().substring(4,5).equals("0")) opposite += "9)R";
            else opposite += "0)";
            return Id.createLinkId(opposite);
        }
        else{
            String opposite = "j(";
            if(homeId.toString().substring(2,3).equals("0")) {
                opposite += "9," + homeId.toString().substring(4,6);
            }
            else opposite += "0," + homeId.toString().substring(4,6) + "R";
            return Id.createLinkId(opposite);
        }
    }

    private static CarrierVehicle getLightVehicle(Id<?> id, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_"+id.toString()+"_lightVehicle_" + depot) ,Vehicle.class), homeId);
        vBuilder.setEarliestStart(6*60*60);
        vBuilder.setLatestEnd(16*60*60);
        vBuilder.setType(createLightType());
        return vBuilder.build();
    }

    private static VehicleType createLightType() {
        CarrierUtils.Builder typeBuilder = CarrierUtils.Builder.newInstance(Id.create("small", org.matsim.vehicles.VehicleType.class ) );
        typeBuilder.setCapacityWeightInTons(6 );
        typeBuilder.setFixCost(80.0);
        typeBuilder.setCostPerDistanceUnit(0.00047);
        typeBuilder.setCostPerTimeUnit(0.008);
        return typeBuilder.build();
    }

    private static CarrierVehicle getHeavyVehicle(Id<?> id, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create("carrier_" + id.toString() + "_heavyVehicle_" + depot, Vehicle.class), homeId);
        vBuilder.setEarliestStart(6*60*60);
        vBuilder.setLatestEnd(16*60*60);
        vBuilder.setType(createHeavyType());
        return vBuilder.build();
    }

    private static VehicleType createHeavyType() {
        CarrierUtils.Builder typeBuilder = CarrierUtils.Builder.newInstance(Id.create("heavy", org.matsim.vehicles.VehicleType.class ) );
        typeBuilder.setCapacityWeightInTons(25 );
        typeBuilder.setFixCost(130.0);
        typeBuilder.setCostPerDistanceUnit(0.00077);
        typeBuilder.setCostPerTimeUnit(0.008);
        return typeBuilder.build();
    }



}
