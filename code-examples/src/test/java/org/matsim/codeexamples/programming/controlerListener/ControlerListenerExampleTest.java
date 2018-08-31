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
package org.matsim.codeexamples.programming.controlerListener;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.matsim.codeexamples.programming.controlerListener.RunControlerListenerExample;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class ControlerListenerExampleTest {
	
	/**
	 * Test method for {@link RunControlerListenerExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		
		final String pathname = "./output/example/";
		try {
			IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		} catch ( UncheckedIOException ee ) {
			// (normally, the directory should NOT be there initially.  It might, however, be there if someone ran the main class in some other way,
			// and did not remove the directory afterwards.)
		}
		
		try {
			RunControlerListenerExample.main(null);
		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running subpopulation example: "+ee ) ;
		}

		IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
	}
}
