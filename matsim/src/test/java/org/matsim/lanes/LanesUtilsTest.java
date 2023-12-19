package org.matsim.lanes;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LanesUtilsTest {

    private LanesToLinkAssignment createLaneAssignment(Scenario scenario, double inLength, double leftLength, double straightLength) {

        // construct a link with effectively two lanes and one ingoing lane of zero length
        // the ingoing lane is required for qsim to work at all with the lanes feature
        Id<Link> linkIdBeforeIntersection = Id.createLinkId("1325764790002f");
        Id<Link> nextLinkIdLeftTurn = Id.createLinkId("3624560720000f");
        Id<Link> nextLinkIdStraight = Id.createLinkId("1325764790003f");

        Id<Lane> inLaneId = Id.create("1325764790002f_in", Lane.class);
        Id<Lane> leftTurnLaneId = Id.create("1325764790002f_left", Lane.class);
        Id<Lane> straightLaneId = Id.create("1325764790002f_straight", Lane.class);

        LanesFactory factory = scenario.getLanes().getFactory();
        // add lanes for link "1325764790002f"
        LanesToLinkAssignment laneLinkAssignment = factory.createLanesToLinkAssignment(linkIdBeforeIntersection);

        Lane laneIn = factory.createLane(inLaneId);
        laneIn.addToLaneId(leftTurnLaneId);
        laneIn.addToLaneId(straightLaneId);
        laneIn.setStartsAtMeterFromLinkEnd(inLength);
        laneIn.setCapacityVehiclesPerHour(720. * 4);
        laneIn.setNumberOfRepresentedLanes(4.0);
        laneLinkAssignment.addLane(laneIn);

        Lane lane0 = factory.createLane(leftTurnLaneId);
        lane0.addToLinkId(nextLinkIdLeftTurn); // turn left towards check-in link
        lane0.setStartsAtMeterFromLinkEnd(leftLength);
        lane0.setCapacityVehiclesPerHour(720.);
        laneLinkAssignment.addLane(lane0);

        Lane lane1 = factory.createLane(straightLaneId);
        lane1.addToLinkId(nextLinkIdStraight); // straight!
        lane1.setStartsAtMeterFromLinkEnd(straightLength);
        lane1.setCapacityVehiclesPerHour(720. * 3.0);
        lane1.setNumberOfRepresentedLanes(3.0);
        laneLinkAssignment.addLane(lane1);

        return laneLinkAssignment;
    }

	@Test
	void correctOrder() {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        NetworkFactory f = scenario.getNetwork().getFactory();

        // left and straight lane start after the ingoing
        LanesToLinkAssignment l2l = createLaneAssignment(scenario, 10, 8, 8);
        Link link = f.createLink(
                l2l.getLinkId(),
                f.createNode(Id.createNodeId(1), new Coord(1,1)),
                f.createNode(Id.createNodeId(2), new Coord(2,2))
        );
        link.setLength(10);


        List<ModelLane> lanes = LanesUtils.createLanes(link, l2l);
        assertThat(lanes).hasSize(3);

        // lanes start at the beginning
        l2l = createLaneAssignment(scenario, 10, 10, 10);
        lanes = LanesUtils.createLanes(link, l2l);

        assertThat(lanes).hasSize(3);

    }
}