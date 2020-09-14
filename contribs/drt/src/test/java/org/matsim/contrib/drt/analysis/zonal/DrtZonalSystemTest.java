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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtilsTest.createNetwork;
import static org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem.createFromPreparedGeometries;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtZonalSystemTest {

	@Test
	public void test_cellSize100() {
		DrtZonalSystem drtZonalSystem = createFromPreparedGeometries(createNetwork(),
				DrtGridUtils.createGridFromNetwork(createNetwork(), 100));
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId("ab")).getId()).isEqualTo("5");
	}

	@Test
	public void test_cellSize700() {
		DrtZonalSystem drtZonalSystem = createFromPreparedGeometries(createNetwork(),
				DrtGridUtils.createGridFromNetwork(createNetwork(), 700));
		assertThat(drtZonalSystem.getZoneForLinkId(Id.createLinkId("ab")).getId()).isEqualTo("1");
	}

	@Test
	public void test_gridWithinServiceArea(){
		// abgucken von createGridFromNetwork
		Coordinate min = new Coordinate(-500, 500);
		Coordinate max = new Coordinate(1500, 1500);
		List<PreparedGeometry> serviceArea = createServiceArea(min,max);
		DrtZonalSystem zonalSystem = createFromPreparedGeometries(createNetwork(),
				DrtGridUtils.createGridFromNetworkWithinServiceArea(createNetwork(), 100, serviceArea));
		
		//teste anzahl zonen
		assertEquals(4, zonalSystem.getZones().size());
		
		
		//gib mir zone für einen link außerhalb und prüfe ob null
		Id<Link> id = Id.createLinkId("da");
		assertThat(zonalSystem.getZoneForLinkId(id).equals(null));
	}

	@Test
	public void test_noZonesWithoutLinks(){
		//baue eine serviceArea, die ganz woanders als das Netzwerk liegt

		//teste, dass drtZonalSystem keine Zone hat
	}
	
	public List<PreparedGeometry> createServiceArea(Coordinate min, Coordinate max){
		GeometryFactory gf = new GeometryFactory();
		PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
		List<PreparedGeometry> ServiceArea =  new ArrayList<>();
		Coordinate p1 = new Coordinate(min.x, min.y);
		Coordinate p2 = new Coordinate(min.x, max.y);
		Coordinate p3 = new Coordinate(max.x, max.y);
		Coordinate p4 = new Coordinate(max.x, min.y);
		Coordinate[] ca = { p1, p2, p3, p4, p1 };
		Polygon polygon = new Polygon(gf.createLinearRing(ca), null, gf);
		ServiceArea.add(preparedGeometryFactory.create(polygon));
		return ServiceArea;
	}



}
