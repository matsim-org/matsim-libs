/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimFileTypeGuesserSignalsTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.signals.data.MatsimSignalSystemsReader;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;

/**
 * @author dgrether
 *
 */
public class MatsimFileTypeGuesserSignalsTest {

	@Test
	void testSignalSystemsV20XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/contrib/signals/data/signalsystems/v20/testSignalSystems_v2.0.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.SignalSystems, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals(MatsimSignalSystemsReader.SIGNALSYSTEMS20, g.getSystemId());
	}

	@Test
	void testSignalGroupsV20XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/contrib/signals/data/signalgroups/v20/testSignalGroups_v2.0.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.SignalGroups, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals(MatsimSignalSystemsReader.SIGNALGROUPS20, g.getSystemId());
	}

	@Test
	void testSignalControlV20XML() throws IOException {
		MatsimFileTypeGuesser g = new MatsimFileTypeGuesser("test/input/org/matsim/contrib/signals/data/signalcontrol/v20/testSignalControl_v2.0.xml");
		assertEquals(MatsimFileTypeGuesser.FileType.SignalControl, g.getGuessedFileType());
		assertNull(g.getPublicId());
		assertNotNull(g.getSystemId());
		assertEquals(MatsimSignalSystemsReader.SIGNALCONTROL20, g.getSystemId());
	}
}
