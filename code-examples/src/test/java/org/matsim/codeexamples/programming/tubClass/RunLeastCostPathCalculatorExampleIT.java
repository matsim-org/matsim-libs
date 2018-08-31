/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.codeexamples.programming.tubClass;

import java.io.File;

import org.junit.Test;
import org.matsim.codeexamples.programming.leastCostPath.RunLeastCostPathCalculatorExample;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author  jbischoff
 *
 */
public class RunLeastCostPathCalculatorExampleIT {

	@Test
	public void test() {
		try {
			IOUtils.deleteDirectoryRecursively(new File( RunLeastCostPathCalculatorExample.outputDirectory ).toPath());
		} catch ( Exception ee ) {
			// deletion may fail; is ok.
		}
			RunLeastCostPathCalculatorExample.main(null);
			IOUtils.deleteDirectoryRecursively(new File( RunLeastCostPathCalculatorExample.outputDirectory ).toPath());
			// if this fails, then it is a test failure (since the directory should have been constructed)

	}

}
