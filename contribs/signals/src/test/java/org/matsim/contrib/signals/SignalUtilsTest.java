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
package org.matsim.contrib.signals;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsDataImpl;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;


public class SignalUtilsTest {

	@Test
	final void testCreateAndAddSignalGroups4Signals() {
		Id<SignalSystem> id1 = Id.create("1", SignalSystem.class);
		Id<SignalGroup> idSg1 = Id.create("1", SignalGroup.class);
		Id<SignalGroup> idSg3 = Id.create("3", SignalGroup.class);
		Id<Signal> idS1 = Id.create("1", Signal.class);
		Id<Signal> idS3 = Id.create("3", Signal.class);
		SignalsData signals = new SignalsDataImpl(ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class));
		SignalSystemsDataFactory fac = signals.getSignalSystemsData().getFactory();
		SignalSystemData system = fac.createSignalSystemData(id1);
		SignalData signal = fac.createSignalData(Id.create(id1, Signal.class));
		system.addSignalData(signal);
		signal = fac.createSignalData(idS3);
		system.addSignalData(signal);
		
		
		SignalGroupsData groups = signals.getSignalGroupsData();
		SignalUtils.createAndAddSignalGroups4Signals(groups, system);
		
		Map<Id<SignalGroup>, SignalGroupData> system1Groups = groups.getSignalGroupDataBySignalSystemId().get(id1);
		Assertions.assertNotNull(system1Groups);
		Assertions.assertEquals(2, system1Groups.size());
		
		Assertions.assertTrue(system1Groups.containsKey(idSg1));
		SignalGroupData group4sys = system1Groups.get(idSg1);
		Assertions.assertNotNull(group4sys);
		Assertions.assertEquals(idSg1, group4sys.getId());
		Assertions.assertNotNull(group4sys.getSignalIds());
		Assertions.assertEquals(idS1, group4sys.getSignalIds().iterator().next());
		
		group4sys = system1Groups.get(idSg3);
		Assertions.assertNotNull(group4sys);
		Assertions.assertEquals(idSg3, group4sys.getId());
		Assertions.assertNotNull(group4sys.getSignalIds());
		Assertions.assertEquals(idS3, group4sys.getSignalIds().iterator().next());
		
		
	}

}
