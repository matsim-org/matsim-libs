package org.matsim.contrib.common.zones.systems.h3;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.h3.HierarchicalH3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.HierarchicalH3ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.h3.NodeCountSubdivisionCriterion;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nkuehnel / MOIA
 */
public class HierarchicalH3ZoneSystemTest {

	private static final String CRS = TransformationFactory.DHDN_GK4;

	@Test
	void testNodeCountSubdivision() {
		Network network = H3DrtZonalSystemTest.getNetwork();
		int minRes = 3;
		int maxRes = 7;
		int maxNodes = 100;

		HierarchicalH3ZoneSystem zoneSystem = HierarchicalH3ZoneSystem.withNodeCountCriterion(
				CRS, minRes, maxRes, maxNodes, network);

		Map<Id<Zone>, Zone> zones = zoneSystem.getZones();
		assertThat(zones).isNotEmpty();

		// should produce more zones than flat resolution 3
		ZoneSystem flatMin = new H3ZoneSystem(CRS, minRes, network, z -> true);
		assertThat(zones.size()).isGreaterThan(flatMin.getZones().size());

		// should produce fewer zones than flat resolution 7
		ZoneSystem flatMax = new H3ZoneSystem(CRS, maxRes, network, z -> true);
		assertThat(zones.size()).isLessThan(flatMax.getZones().size());

		// all links should be mappable
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()).orElse(null),
					"Link " + link.getId() + " should be mapped to a zone");
		}
	}

	@Test
	void testNoSubdivisionWhenThresholdHigh() {
		Network network = H3DrtZonalSystemTest.getNetwork();
		int minRes = 3;
		int maxRes = 7;

		HierarchicalH3ZoneSystem zoneSystem = HierarchicalH3ZoneSystem.withNodeCountCriterion(
				CRS, minRes, maxRes, Integer.MAX_VALUE, network);

		// equivalent to flat at minRes since no cell exceeds threshold
		ZoneSystem flatMin = new H3ZoneSystem(CRS, minRes, network, z -> true);
		assertThat(zoneSystem.getZones().size()).isEqualTo(flatMin.getZones().size());
	}

	@Test
	void testCustomCriterionNeverSubdivides() {
		Network network = H3DrtZonalSystemTest.getNetwork();
		int minRes = 5;
		int maxRes = 10;

		HierarchicalH3ZoneSystem zoneSystem = new HierarchicalH3ZoneSystem(
				CRS, minRes, maxRes, network,
				(h3Cell, nodes, currentRes, maxResolution) -> false);

		// should be equivalent to flat at minRes
		ZoneSystem flatMin = new H3ZoneSystem(CRS, minRes, network, z -> true);
		assertThat(zoneSystem.getZones().size()).isEqualTo(flatMin.getZones().size());
	}

	@Test
	void testZoneFilter() {
		Network network = H3DrtZonalSystemTest.getNetwork();

		HierarchicalH3ZoneSystem unfiltered = HierarchicalH3ZoneSystem.withNodeCountCriterion(
				CRS, 3, 7, 100, network);

		// filter that rejects half the zones
		HierarchicalH3ZoneSystem filtered = new HierarchicalH3ZoneSystem(
				CRS, 3, 7, network,
				new NodeCountSubdivisionCriterion(100),
				zone -> zone.getId().index() % 2 == 0);

		assertThat(filtered.getZones().size()).isLessThan(unfiltered.getZones().size());
	}

	@Test
	void testCoordLookupAcrossResolutions() {
		Network network = H3DrtZonalSystemTest.getNetwork();
		int maxNodes = 50;

		HierarchicalH3ZoneSystem zoneSystem = HierarchicalH3ZoneSystem.withNodeCountCriterion(
				CRS, 3, 8, maxNodes, network);

		// pick some known links and verify they resolve to a zone
		assertTrue(zoneSystem.getZoneForLinkId(Id.createLinkId(358598)).isPresent());
		assertTrue(zoneSystem.getZoneForLinkId(Id.createLinkId(78976)).isPresent());
		assertTrue(zoneSystem.getZoneForLinkId(Id.createLinkId(59914)).isPresent());
	}

	@Test
	void testDeterminism() {
		Network network = H3DrtZonalSystemTest.getNetwork();

		HierarchicalH3ZoneSystem run1 = HierarchicalH3ZoneSystem.withNodeCountCriterion(
				CRS, 3, 7, 100, network);
		HierarchicalH3ZoneSystem run2 = HierarchicalH3ZoneSystem.withNodeCountCriterion(
				CRS, 3, 7, 100, network);

		assertThat(run1.getZones().keySet()).isEqualTo(run2.getZones().keySet());
	}

	@Test
	void testFromConfig() {
		Network network = H3DrtZonalSystemTest.getNetwork();

		HierarchicalH3GridZoneSystemParams params = new HierarchicalH3GridZoneSystemParams();
		params.setH3MinResolution(3);
		params.setH3MaxResolution(7);
		params.setMaxNodesPerZone(100);

		ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(null, network, params, CRS, z -> true);

		assertThat(zoneSystem).isInstanceOf(HierarchicalH3ZoneSystem.class);
		assertThat(zoneSystem.getZones()).isNotEmpty();

		// all links should be mappable
		for (Link link : network.getLinks().values()) {
			assertNotNull(zoneSystem.getZoneForLinkId(link.getId()).orElse(null));
		}
	}
}