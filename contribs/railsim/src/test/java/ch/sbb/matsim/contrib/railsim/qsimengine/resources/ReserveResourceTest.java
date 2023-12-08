package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTestUtils;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ReserveResourceTest {

	private TrainPosition d1 = Mockito.mock(TrainPosition.class, Mockito.RETURNS_DEEP_STUBS);
	private TrainPosition d2 = Mockito.mock(TrainPosition.class, Mockito.RETURNS_DEEP_STUBS);
	private TrainPosition d3 = Mockito.mock(TrainPosition.class, Mockito.RETURNS_DEEP_STUBS);

	private RailLink l1 = RailsimTestUtils.createLink(10, 2);
	private RailLink l2 = RailsimTestUtils.createLink(10, 2);

	private RailResourceInternal r;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<String> args() {
		return List.of("fixedBlock", "movingBlock");
	}

	public ReserveResourceTest(String type) {
		r = switch (type) {
			case "fixedBlock" -> new FixedBlockResource(Id.create("r", RailResource.class), List.of(l1, l2));
			case "movingBlock" -> new MovingBlockResource(Id.create("r", RailResource.class), List.of(l1, l2));
			default -> throw new IllegalArgumentException();
		};
	}

	@Test
	public void anyTrack() {

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

	@Test
	public void anyTrackNonBlocking() {

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
