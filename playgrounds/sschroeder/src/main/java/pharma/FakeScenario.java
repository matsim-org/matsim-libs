package pharma;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * Created by schroeder on 02/11/15.
 */
public class FakeScenario {
    public static void main(String[] args) {
//        ActivityFacilities dcs =
        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimFacilitiesReader(scenario).readFile("output/dcs.xml");
        new MatsimFacilitiesReader(scenario).readFile("output/pharmacies.xml");
        new NetworkReaderMatsimV1(scenario).parse("/Users/schroeder/DLR/Pharma/data/network.xml");
        ActivityFacilities facilities = scenario.getActivityFacilities();

        new WorldConnectLocations(config).connectFacilitiesWithLinks(facilities, (NetworkImpl) scenario.getNetwork());

        ActivityFacility dlr = facilities.getFacilitiesForActivityType("pickup").values().iterator().next();

        System.out.println("id: " + dlr.getId());
        System.out.println("coord: " + dlr.getCoord());
        System.out.println("linkId: " + dlr.getLinkId());

        Carriers carriers = new Carriers();
        Carrier carrier = CarrierImpl.newInstance(Id.create("dlr_carrier",Carrier.class));
        carrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        carrier.getCarrierCapabilities().getCarrierVehicles().add(createVehicle(Id.create(("vehicle"), Vehicle.class),dlr.getLinkId()));

//        carrier.
    }

    private static CarrierVehicle createVehicle(Id<Vehicle> vehicleId, Id<Link> startLocationId) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(vehicleId, startLocationId);
        vBuilder.setEarliestStart(8*60*60);
        vBuilder.setLatestEnd(11*60*60);
        vBuilder.setType(createType());
        return vBuilder.build();
    }

    private static CarrierVehicleType createType() {
        CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("small", VehicleType.class));
        typeBuilder.setCapacity(6);
        typeBuilder.setFixCost(80.0);
        typeBuilder.setCostPerDistanceUnit(0.00047);
        typeBuilder.setCostPerTimeUnit(0.008);
        return typeBuilder.build();
    }
}
