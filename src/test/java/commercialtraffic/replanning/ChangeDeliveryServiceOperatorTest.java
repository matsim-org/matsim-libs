package commercialtraffic.replanning;

import commercialtraffic.deliveryGeneration.DeliveryGeneratorTest;
import commercialtraffic.deliveryGeneration.PersonDelivery;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

public class ChangeDeliveryServiceOperatorTest {

    @Test
    public void getPlanAlgoInstance() {
        Plan testPlan = createPlan();
        Carriers carriers = DeliveryGeneratorTest.generateCarriers();
        ChangeDeliveryServiceOperator changeDeliveryServiceOperator = new ChangeDeliveryServiceOperator(ConfigUtils.createConfig().global(), carriers);

        changeDeliveryServiceOperator.getPlanAlgoInstance().run(testPlan);

        Activity work = (Activity) testPlan.getPlanElements().get(2);
        String op = PersonDelivery.getServiceOperator(work);
        Assert.assertEquals(op, "2");

        changeDeliveryServiceOperator.getPlanAlgoInstance().run(testPlan);

        op = PersonDelivery.getServiceOperator(work);
        Assert.assertEquals(op, "1");

    }

    private Plan createPlan() {
        Person p = PopulationUtils.createPopulation(ConfigUtils.createConfig()).getFactory().createPerson(Id.createPersonId(1));
        Plan plan = PopulationUtils.createPlan();
        p.addPlan(plan);

        Activity home = PopulationUtils.createActivityFromCoord("home", new Coord(-200, 800));
        home.setLinkId(Id.createLinkId(116));
        home.setEndTime(8 * 3600);

        plan.addActivity(home);
        plan.addLeg(PopulationUtils.createLeg(TransportMode.car));

        Activity work = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
        work.setLinkId(Id.createLinkId(259));
        work.setEndTime(16 * 3600);

        work.getAttributes().putAttribute(PersonDelivery.JOB_TYPE, "pizza");
        work.getAttributes().putAttribute(PersonDelivery.JOB_DURATION, 180);
        work.getAttributes().putAttribute(PersonDelivery.JOB_EARLIEST_START, 12 * 3600);
        work.getAttributes().putAttribute(PersonDelivery.JOB_TIME_END, 13 * 3600);
        work.getAttributes().putAttribute(PersonDelivery.JOB_OPERATOR, 1);
        work.getAttributes().putAttribute(PersonDelivery.JOB_SIZE, 1);

        plan.addActivity(work);


        plan.addLeg(PopulationUtils.createLeg(TransportMode.car));


        Activity home2 = PopulationUtils.createActivityFromCoord("home", new Coord(-200, 800));
        home2.setLinkId(Id.createLinkId(116));
        plan.addActivity(home2);
        return plan;
    }

}