package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.load.IntegerLoadType;

/**
 * Tests {@link InsertionGenerator.Insertion#getRequest()}, i.e. which request variant an insertion serves.
 */
public class InsertionRequestTest {
	private final IntegerLoadType loadType = new IntegerLoadType("passengers");
	private final VehicleEntry vehicleEntry = mock(VehicleEntry.class);

	@Test
	void requestIsTakenFromWaypoints() {
		var variant = mock(DrtRequest.class);
		var insertion = InsertionTestUtils.insertionFor(variant, vehicleEntry, loadType.fromInt(1));
		assertThat(insertion.getRequest()).isSameAs(variant);
	}

	@Test
	void requestIsNullWithoutPickupAndDropoffWaypoints() {
		var point = new InsertionGenerator.InsertionPoint(0, null, null, null);
		var insertion = new InsertionGenerator.Insertion(vehicleEntry, point, point, loadType.fromInt(1));
		assertThat(insertion.getRequest()).isNull();
	}

	@Test
	void pickupAndDropoffMustCarryTheSameRequest() {
		var pickup = new InsertionGenerator.InsertionPoint(0, null, new Waypoint.Pickup(mock(DrtRequest.class)), null);
		var dropoff = new InsertionGenerator.InsertionPoint(0, null, new Waypoint.Dropoff(mock(DrtRequest.class)), null);
		assertThatThrownBy(() -> new InsertionGenerator.Insertion(vehicleEntry, pickup, dropoff, loadType.fromInt(1)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
