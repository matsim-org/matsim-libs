package freight.deliveryGeneration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class DeliveryGeneratorTest {


    @org.junit.Test
    public void notifyBeforeMobsim() {
        Carriers carriers = generateCarriers();
        Config config = createConfig();
        Scenario scenario = createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("grid_network.xml");
        addPopulation(scenario);
        DeliveryGenerator generator = new DeliveryGenerator(scenario, carriers);

        generator.notifyBeforeMobsim(new BeforeMobsimEvent(null, 0));
    }

    private Carriers generateCarriers() {
        Carriers carriers = new Carriers();
        Carrier pizza_1 = CarrierImpl.newInstance(Id.create("pizza_1", Carrier.class));
        Carrier shopping_1 = CarrierImpl.newInstance(Id.create("shopping_1", Carrier.class));
        carriers.addCarrier(pizza_1);
        carriers.addCarrier(shopping_1);
        pizza_1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        pizza_1.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1), Id.createLinkId(111), "one"));
        pizza_1.getCarrierCapabilities().getVehicleTypes().add(createLightType());

        shopping_1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        shopping_1.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(2), Id.createLinkId(111), "one"));
        shopping_1.getCarrierCapabilities().getVehicleTypes().add(createLightType());


        return carriers;
    }


    private void addPopulation(Scenario scenario) {
        Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(1));
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        p.addPlan(plan);

        Activity home = PopulationUtils.createActivityFromCoord("home", new Coord(-200, 800));
        home.setLinkId(Id.createLinkId(116));
        home.setEndTime(8 * 3600);

        plan.addActivity(home);
        plan.addLeg(PopulationUtils.createLeg(TransportMode.car));

        Activity work = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
        work.setLinkId(Id.createLinkId(259));
        work.setEndTime(16 * 3600);

        work.getAttributes().putAttribute(PersonDelivery.DELIEVERY_TYPE, "pizza");
        work.getAttributes().putAttribute(PersonDelivery.DELIEVERY_DURATION, 180);
        work.getAttributes().putAttribute(PersonDelivery.DELIEVERY_TIME_START, 12 * 3600);
        work.getAttributes().putAttribute(PersonDelivery.DELIEVERY_TIME_END, 13 * 3600);
        work.getAttributes().putAttribute(PersonDelivery.SERVICE_OPERATOR, 1);
        work.getAttributes().putAttribute(PersonDelivery.DELIEVERY_SIZE, 1);

        plan.addActivity(work);


        plan.addLeg(PopulationUtils.createLeg(TransportMode.car));


        Activity home2 = PopulationUtils.createActivityFromCoord("home", new Coord(-200, 800));
        home2.setLinkId(Id.createLinkId(116));
        plan.addActivity(home2);
        scenario.getPopulation().addPerson(p);

    }


    private static CarrierVehicle getLightVehicle(Id<?> id, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_" + id.toString() + "_lightVehicle_" + depot), Vehicle.class), homeId);
        vBuilder.setEarliestStart(6 * 60 * 60);
        vBuilder.setLatestEnd(16 * 60 * 60);
        vBuilder.setType(createLightType());
        return vBuilder.build();
    }

    private static CarrierVehicleType createLightType() {
        CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("small", VehicleType.class));
        typeBuilder.setCapacity(6);
        typeBuilder.setFixCost(80.0);
        typeBuilder.setCostPerDistanceUnit(0.00047);
        typeBuilder.setCostPerTimeUnit(0.008);
        return typeBuilder.build();
    }
}