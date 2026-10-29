package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopCandidate;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.testcases.fakes.FakeLink;

/**
 * Tests how the stop candidates of a request are turned into request variants.
 */
public class RequestVariantsTest {
	private static final double MAX_WAIT = 600;
	private static final double MAX_TRAVEL = 1800;
	private static final double EARLIEST = 1000; // arrival at the request's own pickup stop

	private final Link from = link("from");
	private final Link to = link("to");
	private final Link accessFar = link("accessFar");
	private final Link accessNear = link("accessNear");
	private final Link egressFar = link("egressFar");

	private final DrtRouteConstraints constraints = new DrtRouteConstraints(MAX_TRAVEL, 3600, MAX_WAIT, 300, 0, true);

	@Test
	void withoutCandidates_onlyTheRequestItself() {
		DrtRequest request = builder().build();
		var variants = RequestVariants.of(request);
		assertThat(variants).hasSize(1);
		assertThat(variants.getFirst().request()).isSameAs(request);
		assertThat(variants.getFirst().index()).isZero();
		assertThat(variants.getFirst().walkDuration()).isZero();
	}

	@Test
	void firstVariantIsTheRequestWithItsWalkingDurations() {
		DrtRequest request = builder().stopCandidates(new DrtStopCandidates(60, 30,
				List.of(new DrtStopCandidate(accessFar, 180)), List.of())).build();

		var variants = RequestVariants.of(request);
		assertThat(variants).hasSize(2);
		assertThat(variants.getFirst().request()).isSameAs(request);
		assertThat(variants.getFirst().accessWalkDuration()).isEqualTo(60);
		assertThat(variants.getFirst().egressWalkDuration()).isEqualTo(30);
		assertThat(variants.get(1).index()).isEqualTo(1);
	}

	@Test
	void fartherPickupStop_laterEarliestStart_sameLatestArrival() {
		DrtRequest request = builder().stopCandidates(new DrtStopCandidates(60, 30,
				List.of(new DrtStopCandidate(accessFar, 180)), List.of())).build();

		var variant = RequestVariants.of(request).get(1);
		DrtRequest v = variant.request();
		assertThat(v.getId()).isEqualTo(request.getId());
		assertThat(v.getPassengerIds()).isEqualTo(request.getPassengerIds());
		assertThat(v.getFromLink()).isSameAs(accessFar);
		assertThat(v.getToLink()).isSameAs(to);
		assertThat(v.getStopCandidates()).isEmpty();
		assertThat(variant.accessWalkDuration()).isEqualTo(180);
		assertThat(variant.egressWalkDuration()).isEqualTo(30);

		// departure at the origin: 1000 - 60 = 940; arrival at the farther stop: 940 + 180 = 1120
		assertThat(v.getEarliestStartTime()).isEqualTo(1120);
		// waiting is counted from the arrival at the stop
		assertThat(v.getLatestStartTime()).isEqualTo(1120 + MAX_WAIT);
		// latest arrival at the destination unchanged
		assertThat(v.getLatestArrivalTime()).isEqualTo(request.getLatestArrivalTime());
		assertThat(v.getConstraints().maxRideDuration()).isEqualTo(constraints.maxRideDuration());
	}

	@Test
	void nearerPickupStop_earlierEarliestStart() {
		DrtRequest request = builder().stopCandidates(new DrtStopCandidates(60, 0,
				List.of(new DrtStopCandidate(accessNear, 20)), List.of())).build();

		DrtRequest v = RequestVariants.of(request).get(1).request();
		assertThat(v.getEarliestStartTime()).isEqualTo(EARLIEST - 40);
		assertThat(v.getLatestArrivalTime()).isEqualTo(request.getLatestArrivalTime());
	}

	@Test
	void fartherDropoffStop_earlierLatestArrival() {
		DrtRequest request = builder().stopCandidates(new DrtStopCandidates(0, 30, List.of(),
				List.of(new DrtStopCandidate(egressFar, 130)))).build();

		DrtRequest v = RequestVariants.of(request).get(1).request();
		assertThat(v.getToLink()).isSameAs(egressFar);
		assertThat(v.getEarliestStartTime()).isEqualTo(EARLIEST);
		// 100 s more walking after the dropoff, so the vehicle must arrive 100 s earlier
		assertThat(v.getLatestArrivalTime()).isEqualTo(request.getLatestArrivalTime() - 100);
	}

	@Test
	void allCombinations_sameLinkSkipped() {
		DrtRequest request = builder().stopCandidates(new DrtStopCandidates(0, 0,
				List.of(new DrtStopCandidate(accessFar, 100)), List.of(new DrtStopCandidate(accessFar, 100)))).build();

		// (from,to)=request, (from,accessFar), (accessFar,to); (accessFar,accessFar) is skipped
		var variants = RequestVariants.of(request);
		assertThat(variants).hasSize(3);
		assertThat(variants).noneMatch(v -> v.request().getFromLink() == v.request().getToLink());
		assertThat(variants).extracting(RequestVariant::index).containsExactly(0, 1, 2);
	}

	@Test
	void copyOfRequestKeepsCandidates() {
		// as in DrtRequestInsertionRetryQueue
		DrtRequest request = builder().stopCandidates(new DrtStopCandidates(60, 30,
				List.of(new DrtStopCandidate(accessFar, 180)), List.of())).build();
		DrtRequest copy = DrtRequest.newBuilder(request).build();
		assertThat(copy.getStopCandidates()).isEqualTo(request.getStopCandidates());
		assertThat(RequestVariants.of(copy)).hasSize(2);
	}

	@Test
	void negativeWalkDurationIsRejected() {
		assertThatThrownBy(() -> new DrtStopCandidates(-1, 0, List.of(), List.of()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DrtStopCandidate(accessFar, -1)).isInstanceOf(IllegalArgumentException.class);
	}

	private DrtRequest.Builder builder() {
		return DrtRequest.newBuilder()
				.id(Id.create("r", Request.class))
				.passengerIds(List.of(Id.createPersonId("p")))
				.mode("drt")
				.fromLink(from)
				.toLink(to)
				.earliestDepartureTime(EARLIEST)
				.constraints(constraints);
	}

	private static Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}
}
