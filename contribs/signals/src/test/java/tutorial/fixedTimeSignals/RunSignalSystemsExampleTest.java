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

/**
 * @author nagel
 *
 */
public class RunSignalSystemsExampleTest {

	@Test
	public final void test() {
		boolean usingOTFVis = false ;
		try {
			RunSignalSystemsExample.run(usingOTFVis);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong") ;
		}
	}

}
