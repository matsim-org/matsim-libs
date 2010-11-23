/* *********************************************************************** *
 * project: org.matsim.*
 * SignalUtilsTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.signalsystems;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsDataImpl;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsDataFactory;


public class SignalUtilsTest {

	@Test
	public final void testCreateAndAddSignalGroups4Signals() {
		Id id1 = new IdImpl("1");
		Id id3 = new IdImpl("3");
		SignalsData signals = new SignalsDataImpl();
		SignalSystemsDataFactory fac = signals.getSignalSystemsData().getFactory();
		SignalSystemData system = fac.createSignalSystemData(id1);
		SignalData signal = fac.createSignalData(id1);
		system.addSignalData(signal);
		signal = fac.createSignalData(id3);
		system.addSignalData(signal);
		
		
		SignalGroupsData groups = signals.getSignalGroupsData();
		SignalUtils.createAndAddSignalGroups4Signals(groups, system);
		
		Map<Id, SignalGroupData> system1Groups = groups.getSignalGroupDataBySignalSystemId().get(id1);
		Assert.assertNotNull(system1Groups);
		Assert.assertEquals(2, system1Groups.size());
		
		Assert.assertTrue(system1Groups.containsKey(id1));
		SignalGroupData group4sys = system1Groups.get(id1);
		Assert.assertNotNull(group4sys);
		Assert.assertEquals(id1, group4sys.getId());
		Assert.assertNotNull(group4sys.getSignalIds());
		Assert.assertEquals(id1, group4sys.getSignalIds().iterator().next());
		
		group4sys = system1Groups.get(id3);
		Assert.assertNotNull(group4sys);
		Assert.assertEquals(id3, group4sys.getId());
		Assert.assertNotNull(group4sys.getSignalIds());
		Assert.assertEquals(id3, group4sys.getSignalIds().iterator().next());
		
		
	}

}
