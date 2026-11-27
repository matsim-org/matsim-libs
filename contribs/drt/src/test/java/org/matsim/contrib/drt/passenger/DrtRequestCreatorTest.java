package org.matsim.contrib.drt.passenger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent.StopCandidate;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;

public class DrtRequestCreatorTest {
	private static final IntegerLoadType LOAD_TYPE = new IntegerLoadType("passengers");

	private final Network network = NetworkUtils.createNetwork();
	private final Link from = link("from");
	private final Link to = link("to");
	private final Link alternativePickup = link("alternativePickup");

	private final List<Event> events = new ArrayList<>();
	private final DrtRequestCreator creator = new DrtRequestCreator("drt", eventsManager(), LOAD_TYPE);

	@Test
	void requestWithoutStopCandidates() {
		DrtRequest request = creator.createRequest(Id.create("r", Request.class), List.of(Id.createPersonId("p")),
				List.of(route()), from, to, 100, 50);

		assertThat(request.getStopCandidates()).isEmpty();
		assertThat(submittedEvent().getStopCandidates()).isEmpty();
	}

	@Test
	void stopCandidatesArePartOfTheRequestAndItsSubmittedEvent() {
		DrtStopCandidates candidates = new DrtStopCandidates(10, 20,
				List.of(new DrtStopCandidate(alternativePickup, 140)), List.of());

		DrtRequest request = creator.createRequest(Id.create("r", Request.class), List.of(Id.createPersonId("p")),
				List.of(route()), from, to, 100, 50, candidates);

		assertThat(request.getStopCandidates()).contains(candidates);
		assertThat(submittedEvent().getStopCandidates()).hasValueSatisfying(reported -> {
			assertThat(reported.accessWalkDuration()).isEqualTo(10);
			assertThat(reported.egressWalkDuration()).isEqualTo(20);
			assertThat(reported.alternativeAccess()).containsExactly(new StopCandidate(alternativePickup.getId(), 140));
			assertThat(reported.alternativeEgress()).isEmpty();
		});
	}

	private DrtRoute route() {
		DrtRoute route = new DrtRoute(from.getId(), to.getId());
		route.setConstraints(new DrtRouteConstraints(600, 900, 300, Double.POSITIVE_INFINITY, 0, true));
		route.setDirectRideTime(300);
		route.setDistance(3000);
		route.setLoad(LOAD_TYPE.fromInt(1), LOAD_TYPE);
		return route;
	}

	private DrtRequestSubmittedEvent submittedEvent() {
		assertThat(events).hasSize(1);
		return (DrtRequestSubmittedEvent)events.getFirst();
	}

	private EventsManager eventsManager() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler((BasicEventHandler)events::add);
		eventsManager.initProcessing();
		return eventsManager;
	}

	private Link link(String id) {
		Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_from"), new Coord(0, 0));
		Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_to"), new Coord(100, 0));
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, 100, 10, 1000, 1);
	}
}
