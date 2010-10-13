/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.prognose2025;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.matsim.core.utils.geometry.CoordImpl;

public class VerschmiererTest {

	@Test @Ignore("uses hard coded datafile")
	public void test() {
		Verschmierer verschmierer = new Verschmierer();
		verschmierer.prepare();
		CoordImpl notInCell = new CoordImpl(3.0, 4.0);
		Assert.assertSame(notInCell, verschmierer.shootIntoSameZoneOrLeaveInPlace(notInCell));
		CoordImpl inCell = new CoordImpl(4326122, 5756578);
		Assert.assertNotSame(inCell, verschmierer.shootIntoSameZoneOrLeaveInPlace(inCell));
	}

}
