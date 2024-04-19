/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.analysis.zonal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtilsTest.createNetwork;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZonalSystemTest {

	@Test
	void test_cellSize100() {
		SquareGridZoneSystem drtZonalSystem = new SquareGridZoneSystem(createNetwork(), 100);
		Assertions.assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId("ab")).orElseThrow().getId().toString()).isEqualTo("90");
	}

	@Test
	void test_cellSize700() {
		SquareGridZoneSystem drtZonalSystem = new SquareGridZoneSystem(createNetwork(), 700);
		Assertions.assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId("ab")).orElseThrow().getId().toString()).isEqualTo("2");
	}

	@Test
	void test_gridWithinServiceArea(){
		Coordinate min = new Coordinate(-500, 500);
		Coordinate max = new Coordinate(1500, 1500);
		List<PreparedGeometry> serviceArea = createServiceArea(min,max);

		Predicate<Zone> zoneFilter = zone -> serviceArea.stream().anyMatch(area -> area.intersects(zone.getPreparedGeometry().getGeometry()));
		SquareGridZoneSystem zonalSystem = new SquareGridZoneSystem(createNetwork(), 100, zoneFilter);

		assertEquals(2, zonalSystem.getZones().size());

		//link 'da' is outside of the service area
		Id<Link> id = Id.createLinkId("da");
		Assertions.assertThat(zonalSystem.getZoneForLinkId(id)).isNotPresent();
	}

	@Test
	void test_noZonesWithoutLinks(){
		Coordinate min = new Coordinate(1500, 1500);
		Coordinate max = new Coordinate(2500, 2500);
		List<PreparedGeometry> serviceArea = createServiceArea(min,max);

		Predicate<Zone> zoneFilter = zone -> serviceArea.stream().anyMatch(area -> area.intersects(zone.getPreparedGeometry().getGeometry()));
		SquareGridZoneSystem zonalSystem = new SquareGridZoneSystem(createNetwork(), 100, zoneFilter);

		//service area is off the network - so we should have 0 zones..
		assertEquals(0, zonalSystem.getZones().size());
	}

	public List<PreparedGeometry> createServiceArea(Coordinate min, Coordinate max){
		GeometryFactory gf = new GeometryFactory();
		PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
		List<PreparedGeometry> ServiceArea =  new ArrayList<>();
		Coordinate p1 = new Coordinate(min.x, min.y);
		Coordinate p2 = new Coordinate(max.x, min.y);
		Coordinate p3 = new Coordinate(max.x, max.y);
		Coordinate p4 = new Coordinate(min.x, max.y);
		Coordinate[] ca = { p1, p2, p3, p4, p1 };
		Polygon polygon = new Polygon(gf.createLinearRing(ca), null, gf);
		ServiceArea.add(preparedGeometryFactory.create(polygon));
		return ServiceArea;
	}
}
