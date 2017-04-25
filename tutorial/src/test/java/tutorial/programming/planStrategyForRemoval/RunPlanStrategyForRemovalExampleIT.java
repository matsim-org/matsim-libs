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

package tutorial.programming.planStrategyForRemoval;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
* @author ikaddoura
*/

public class RunPlanStrategyForRemovalExampleIT {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void testMain() {
		
		try {
			RunPlanSelectorForRemovalExample.main(null);
		} catch(Exception e) {
			Assert.fail(e.toString());
		}
	}

}

