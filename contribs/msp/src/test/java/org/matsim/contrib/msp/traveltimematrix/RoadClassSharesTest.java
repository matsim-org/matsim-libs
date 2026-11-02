package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the storage of the road class shares: the rounding into one byte per class, and that a lookup
 * returns shares that sum to 1 so that a query does not have to normalise.
 */
class RoadClassSharesTest {

	/**
	 * The resolution of one stored byte, i.e. how far a share may be off.
	 */
	private static final double TOLERANCE = 1.0 / RoadClassShares.SCALE;

	@Test
	void theSharesOfAPairSumToOne() {

		Fixture fixture = new Fixture();
		fixture.shares.set(fixture.first, fixture.second, new double[]{202, 0, 21});

		double[] shares = fixture.shares.get(fixture.first, fixture.second);

		assertEquals(1.0, shares[0] + shares[1] + shares[2], 1e-12,
			"the bytes of a pair are normalised to sum to the full scale, so a lookup needs no normalisation");
		assertEquals(202.0 / 223, shares[0], TOLERANCE);
		assertEquals(0.0, shares[1]);
		assertEquals(21.0 / 223, shares[2], TOLERANCE);
	}

	@Test
	void absoluteTravelTimesAreTurnedIntoShares() {

		Fixture fixture = new Fixture();
		// the same split, expressed in seconds and in hours: the shares cannot tell the two apart
		fixture.shares.set(fixture.first, fixture.second, new double[]{300, 100, 0});
		double[] fromSeconds = fixture.shares.get(fixture.first, fixture.second);

		fixture.shares.set(fixture.first, fixture.second, new double[]{3, 1, 0});
		double[] fromHours = fixture.shares.get(fixture.first, fixture.second);

		assertEquals(0.75, fromSeconds[0], TOLERANCE);
		assertEquals(0.25, fromSeconds[1], TOLERANCE);
		assertEquals(fromSeconds[0], fromHours[0], 1e-12);
		assertEquals(fromSeconds[1], fromHours[1], 1e-12);
	}

	@Test
	void aPairWithoutAPathPutsEverythingOnTheFirstClass() {

		Fixture fixture = new Fixture();
		fixture.shares.set(fixture.first, fixture.second, new double[]{0, 0, 0});

		double[] shares = fixture.shares.get(fixture.first, fixture.second);

		// there is no path to split, and the travel time these shares multiply is 0 either way. What
		// matters is that the row is not left uninitialised and still sums to 1.
		assertEquals(1.0, shares[0]);
		assertEquals(1.0, shares[0] + shares[1] + shares[2], 1e-12);
	}

	@Test
	void weighingIsTheSameAsLookingUpAndMultiplying() {

		Fixture fixture = new Fixture();
		fixture.shares.set(fixture.first, fixture.second, new double[]{202, 0, 21});
		double[] index = {0.1, 0.5, 0.9};

		double[] shares = fixture.shares.get(fixture.firstNode(), fixture.secondNode());
		double expected = shares[0] * index[0] + shares[1] * index[1] + shares[2] * index[2];

		assertEquals(expected, fixture.shares.weigh(fixture.firstNode(), fixture.secondNode(), index), 1e-12);
	}

	@Test
	void theWrongNumberOfClassesIsRefused() {
		Fixture fixture = new Fixture();
		assertThrows(IllegalArgumentException.class,
			() -> fixture.shares.set(fixture.first, fixture.second, new double[]{1, 2}));
	}

	/**
	 * A share store over the corridor network, with three classes and the two outer zones at hand.
	 */
	private static final class Fixture {

		private final Network network = TestNetworks.corridor();
		private final ZoneSystem zoneSystem = TestNetworks.oneZonePerNode(this.network);
		private final RoadClassShares shares = new RoadClassShares(this.zoneSystem,
			List.of("motorway", "primary", "residential"));
		private final Zone first;
		private final Zone second;

		private Fixture() {
			List<Zone> zones = List.copyOf(this.zoneSystem.getZones().values());
			this.first = zones.get(0);
			this.second = zones.get(zones.size() - 1);
		}

		private Node firstNode() {
			return nodeIn(this.first);
		}

		private Node secondNode() {
			return nodeIn(this.second);
		}

		/**
		 * A node the zone system puts into the given zone - which is what a query addresses a pair by,
		 * while {@code set} addresses it by zone.
		 */
		private Node nodeIn(Zone zone) {
			return this.network.getNodes().values().stream()
				.filter(node -> this.zoneSystem.getZoneForNodeId(node.getId())
					.map(found -> found.getId().equals(zone.getId()))
					.orElse(false))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No node of the network is in zone " + zone.getId() + "."));
		}
	}
}
