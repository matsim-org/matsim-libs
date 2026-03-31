package org.matsim.contrib.drt.prebooking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.IntegerLoad;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests that prebooking requests have the correct constraints. In particular, submission time and earliest start time
 * should be different. Latest Arrival time should be relative to earliest start and not to submission time.
 */
public class PrebookingConstraintsTest {


    @Test
    public void test() {


        EventsManagerImpl eventsManager = new EventsManagerImpl();
        IntegerLoadType loadType = new IntegerLoadType("passengers");
        DrtRequestCreator drtRequestCreator = new DrtRequestCreator("drt", eventsManager, loadType);

        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createNode(Id.createNodeId("dummy1"));
        network.addNode(node1);
        Node node2 = NetworkUtils.createNode(Id.createNodeId("dummy2"));
        network.addNode(node2);
        Node node3 = NetworkUtils.createNode(Id.createNodeId("dummy3"));
        network.addNode(node3);

        network.addLink(NetworkUtils.createLink(Id.createLinkId("dummy1"), node1, node2, network, 0, 0, 0, 0));
        network.addLink(NetworkUtils.createLink(Id.createLinkId("dummy2"), node2, node3, network, 0, 0, 0, 0));

        MobsimTimer mobsimTimer = new MobsimTimer(1);

        Plan plan = PopulationUtils.createPlan();
        PopulationUtils.getFactory().createPerson(Id.createPersonId("dummy")).addPlan(plan);

        plan.addActivity(PopulationUtils.getFactory().createActivityFromLinkId("dummy", Id.createLinkId("dummy")));
        Leg leg = PopulationUtils.getFactory().createLeg("drt");
        plan.addLeg(leg);

        DrtRoute route = (DrtRoute) new DrtRouteFactory().createRoute(Id.createLinkId("dummy1"), Id.createLinkId("dummy2"));
        route.setDistance(100);
        route.setDirectRideTime(100);
        DrtRouteConstraints drtRouteConstraints = new DrtRouteConstraints(
                200,
                50,
                100,
                10,
                0,
                true
        );
        route.setConstraints(drtRouteConstraints);
        route.setLoad(IntegerLoad.fromValue(1), loadType);
        leg.setRoute(route);

        BasicPlanAgentImpl agent = new BasicPlanAgentImpl(plan, ScenarioUtils.createScenario(ConfigUtils.createConfig()),
                eventsManager, mobsimTimer, TimeInterpretation.create(ConfigUtils.createConfig()));
        plan.setPerson(agent.getPerson());

        PrebookingManager prebookingManager = new PrebookingManager("drt", network, drtRequestCreator,
                new VrpOptimizer() {
                    @Override
                    public void requestSubmitted(Request request) {
                        DrtRequest drtRequest = (DrtRequest) request;
                        Assertions.assertEquals(0, drtRequest.getSubmissionTime(), MatsimTestUtils.EPSILON);
                        Assertions.assertEquals(50, drtRequest.getEarliestStartTime(), MatsimTestUtils.EPSILON);
                        Assertions.assertEquals(150, drtRequest.getLatestStartTime(), MatsimTestUtils.EPSILON);
                        Assertions.assertEquals(250, drtRequest.getLatestArrivalTime(), MatsimTestUtils.EPSILON);
                        Assertions.assertEquals(drtRouteConstraints, drtRequest.getConstraints());
                    }

                    @Override
                    public void nextTask(DvrpVehicle vehicle) {

                    }
                }, mobsimTimer, new DefaultPassengerRequestValidator(), eventsManager, null, false);

        prebookingManager.prebook(agent, (Leg) WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements().get(1), 50);
        prebookingManager.notifyMobsimAfterSimStep(new MobsimAfterSimStepEvent(null, 51));

    }
}
