/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.fixedTimeSignals;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.signals.run.RunSignalSystemsExample;

/**
 * @author nagel
 *
 */
public class RunSignalSystemsExampleTest {

	@Test
	public final void testExampleWithHoles() {
		boolean usingOTFVis = false ;
		try {
			RunSignalSystemsExampleWithHoles.run(usingOTFVis);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong") ;
		}
	}
	
	@Test
	public final void testMinimalExample() {
		try {
			String[] args = {"../examples/tutorial/example90TrafficLights/useSignalInput/withLanes/config.xml"};
			RunSignalSystemsExample.main(args);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong") ;
		}
	}

}
