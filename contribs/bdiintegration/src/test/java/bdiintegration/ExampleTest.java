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
package bdiintegration;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class ExampleTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void test() throws IOException {
		
		// you can have files at the class level (i.e. <packageName>.<className>):
		final File file1 = new File( utils.getClassInputDirectory() + "ExampleTest.txt");
		Assert.assertTrue( file1.exists() ) ;

		// you can have files at the method level (i.e. <packageName>.<className>.<methodName>):
		final File file2 = new File( utils.getInputDirectory() + "test.txt");
		Assert.assertTrue( file2.exists() ) ;
		
	}

}
