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

package tutorial.programming.example21TUBclass;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;

import tutorial.programming.example21tutorialTUBclass.leastCostPath.RunLeastCostPathCalculatorExample;

/**
 * @author  jbischoff
 *
 */
public class IT {

	@Test
	@Ignore
	public void test() {
		final String pathname = "./output/example";
		try{
			IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());
		}
		catch (IllegalArgumentException e){
			
		}
		RunLeastCostPathCalculatorExample.main(null);
		IOUtils.deleteDirectoryRecursively(new File(pathname).toPath());

	}

}
