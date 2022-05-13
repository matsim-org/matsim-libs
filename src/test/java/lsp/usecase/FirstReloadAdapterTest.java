/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp.usecase;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LSPCarrierResource;
import lsp.LSPResource;


public class FirstReloadAdapterTest {

	private Id<Link> reloadingLinkId;
	private ReloadingPoint reloadingPoint;
	
	@Before
	public void initialize(){
		
        
        UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> reloadingId = Id.create("ReloadingPoint1", LSPResource.class);
        reloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        
        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        reloadingPoint = reloadingPointBuilder.build();
	}
	
	@Test
	public void reloadingPointTest() {
		assertEquals(10, reloadingPoint.getCapacityNeedFixed(), 0.0);
		assertEquals(1, reloadingPoint.getCapacityNeedLinear(), 0.0);
		assertFalse(LSPCarrierResource.class.isAssignableFrom(reloadingPoint.getClass()));
		assertSame(reloadingPoint.getClassOfResource(), ReloadingPoint.class);
		assertNotNull(reloadingPoint.getClientElements());
		assertTrue(reloadingPoint.getClientElements().isEmpty());
		assertSame(reloadingPoint.getEndLinkId(), reloadingLinkId);
		assertSame(reloadingPoint.getStartLinkId(), reloadingLinkId);
		assertNotNull(reloadingPoint.getEventHandlers());
		assertFalse(reloadingPoint.getEventHandlers().isEmpty());
		assertEquals(1, reloadingPoint.getEventHandlers().size());
		assertNotNull(reloadingPoint.getInfos());
		assertTrue(reloadingPoint.getInfos().isEmpty());
	}
}
