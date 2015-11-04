package pharma;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.Jsprit;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.util.Solutions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 * Created by schroeder on 02/11/15.
 */
public class FakeScenario {
    public static void main(String[] args) {
//        ActivityFacilities dcs =
        Random r = new Random(4711);

        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimFacilitiesReader(scenario).readFile("out/dcs.xml");
        new MatsimFacilitiesReader(scenario).readFile("out/pharmacies.xml");
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
        CarrierVehicle vehicle = createVehicle(Id.create(("vehicle"), Vehicle.class), dlr.getLinkId());
        carrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
        carrier.getCarrierCapabilities().getVehicleTypes().add(vehicle.getVehicleType());

        ActivityFacilities customers = FacilitiesUtils.createActivityFacilities("customers");
        for(int i=0;i<100;i++){
            ActivityFacility facility = drawFacility(facilities.getFacilitiesForActivityType("delivery"), r);
            if(!customers.getFacilities().containsKey(facility.getId())){
                customers.addActivityFacility(facility);
            }

            CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create((i + 1),CarrierService.class),
                    facility.getLinkId());
            serviceBuilder.setCapacityDemand(1);
            serviceBuilder.setServiceDuration(5*60);
            carrier.getServices().add(serviceBuilder.build());
        }
        carriers.addCarrier(carrier);

        createPlans(carriers,scenario.getNetwork());

        new CarrierPlanXmlWriterV2(carriers).write("output/carriers.xml");
        new FacilitiesWriter(customers).write("out/customers.xml");

//        carrier.
    }

    private static void createPlans(Carriers carriers, Network network) {
        for(Carrier c : carriers.getCarriers().values()){
            CarrierPlan p = createPlan(c,network);
            c.setSelectedPlan(p);
        }
    }

    private static CarrierPlan createPlan(Carrier c, Network network) {
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(c, network);
        NetworkBasedTransportCosts.Builder tpcostsBuilder = NetworkBasedTransportCosts.Builder.newInstance(network, c.getCarrierCapabilities().getVehicleTypes());
        NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
        vrpBuilder.setRoutingCost(netbasedTransportcosts);
        VehicleRoutingProblem vrp = vrpBuilder.build();

        VehicleRoutingAlgorithm vra = Jsprit.createAlgorithm(vrp);
        VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());

        CarrierPlan plan = MatsimJspritFactory.createPlan(c, solution);
        NetworkRouter.routePlan(plan, netbasedTransportcosts);
        return plan;
    }

    private static ActivityFacility drawFacility(TreeMap<Id<ActivityFacility>, ActivityFacility> pickups, Random random) {
        List<ActivityFacility> acts = new ArrayList<>(pickups.values());
        int randNumber = random.nextInt(pickups.size());
        return acts.get(randNumber);
    }

    private static CarrierVehicle createVehicle(Id<Vehicle> vehicleId, Id<Link> startLocationId) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(vehicleId, startLocationId);
        vBuilder.setEarliestStart(8*60*60);
        vBuilder.setLatestEnd(11*60*60);
        vBuilder.setType(createType());
        return vBuilder.build();
    }

    private static CarrierVehicleType createType() {
        CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("light", VehicleType.class));
        typeBuilder.setCapacity(5);
        typeBuilder.setFixCost(80.0);
        typeBuilder.setCostPerDistanceUnit(0.00047);
        typeBuilder.setCostPerTimeUnit(0.008);
        return typeBuilder.build();
    }
}
