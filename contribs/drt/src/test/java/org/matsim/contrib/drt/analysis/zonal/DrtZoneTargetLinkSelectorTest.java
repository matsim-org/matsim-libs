/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.analysis.zonal;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtilsTest.createNetwork;
import static org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem.createFromPreparedGeometries;

public class DrtZoneTargetLinkSelectorTest {
	
	
	@Test
	public void RamdomDrtZoneTargetLinkSelectorTest(){
		DrtZonalSystem drtZonalSystem = createFromPreparedGeometries(createNetwork(),
				DrtGridUtils.createGridFromNetwork(createNetwork(), 700));

		RandomDrtZoneTargetLinkSelector selector = new RandomDrtZoneTargetLinkSelector();

		Set<Id<Link>> links = new HashSet<>();
		for(int i = 0; i < 1000; i ++){
			links.add(selector.selectTargetLink(drtZonalSystem.getZones().get("1")).getId());
		}
		//zone has 4 links, we query RandomDrtZoneTargetLinkSelector a lot of times, so every link of zone should be returned at least once...
		assertThat(links.size() == drtZonalSystem.getZones().get("1").getLinks().size());
	}
	
	@Test
	public void MostCentralDrtZoneTargetLinkSelectorTest(){
		DrtZonalSystem drtZonalSystem = createFromPreparedGeometries(createNetwork(),
				DrtGridUtils.createGridFromNetwork(createNetwork(), 700));

		MostCentralDrtZoneTargetLinkSelector selector = new MostCentralDrtZoneTargetLinkSelector(drtZonalSystem);
		
		Link link = selector.selectTargetLink(drtZonalSystem.getZones().get("3"));


		assertThat(link.getId().toString() ==  "cd");
	}


}
