package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTestUtils;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Id;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReserveResourceTest {

	private TrainPosition d1 = Mockito.mock(TrainPosition.class, Mockito.RETURNS_DEEP_STUBS);
	private TrainPosition d2 = Mockito.mock(TrainPosition.class, Mockito.RETURNS_DEEP_STUBS);
	private TrainPosition d3 = Mockito.mock(TrainPosition.class, Mockito.RETURNS_DEEP_STUBS);

	static List<Arguments> params() {
		RailLink l1 = RailsimTestUtils.createLink(10, 2);
		RailLink l2 = RailsimTestUtils.createLink(10, 2);
		return List.of(
			Arguments.of(l1, l2, new FixedBlockResource(Id.create("r", RailResource.class), List.of(l1, l2))),
			Arguments.of(l1, l2, new MovingBlockResource(Id.create("r", RailResource.class), List.of(l1, l2)))
		);
	}

	@ParameterizedTest
	@MethodSource("params")
	public void anyTrack(RailLink l1, RailLink l2, RailResourceInternal r) {

		assertThat(r.hasCapacity(0, l1, RailResourceManager.ANY_TRACK, d1))
			.isTrue();

		double reserved = r.reserve(0, l1, RailResourceManager.ANY_TRACK, d1);
		assertThat(reserved).isEqualTo(10);

		reserved = r.reserve(0, l1, RailResourceManager.ANY_TRACK, d2);
		assertThat(reserved).isEqualTo(10);

		assertThat(r.hasCapacity(0, l1, RailResourceManager.ANY_TRACK, d3))
			.isFalse();

		r.release(l1, d1.getDriver());

		assertThat(r.hasCapacity(0, l1, RailResourceManager.ANY_TRACK, d3))
			.isTrue();

	}

	@ParameterizedTest
	@MethodSource("params")
	public void anyTrackNonBlocking(RailLink l1, RailLink l2, RailResourceInternal r) {

		assertThat(r.hasCapacity(0, l1, RailResourceManager.ANY_TRACK_NON_BLOCKING, d1))
			.isTrue();

		double reserved = r.reserve(0, l1, RailResourceManager.ANY_TRACK_NON_BLOCKING, d1);
		assertThat(reserved).isEqualTo(10);

		assertThat(r.hasCapacity(0, l1, RailResourceManager.ANY_TRACK_NON_BLOCKING, d2))
			.isFalse();

		assertThat(r.hasCapacity(0, l2, RailResourceManager.ANY_TRACK_NON_BLOCKING, d2))
			.isTrue();

		assertThat(r.reserve(0, l2, RailResourceManager.ANY_TRACK_NON_BLOCKING, d2))
			.isEqualTo(10);

		r.release(l1, d1.getDriver());

		assertThat(r.hasCapacity(0, l1, RailResourceManager.ANY_TRACK_NON_BLOCKING, d3))
			.isTrue();

		assertThat(r.hasCapacity(0, l2, RailResourceManager.ANY_TRACK_NON_BLOCKING, d1))
			.isFalse();

	}
}
