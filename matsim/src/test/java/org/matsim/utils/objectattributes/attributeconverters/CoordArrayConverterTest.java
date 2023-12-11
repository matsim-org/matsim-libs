/* *********************************************************************** *
 * project: org.matsim.*
 * CoordArrayConverterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.utils.objectattributes.attributeconverters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

public class CoordArrayConverterTest {

	@Test
	void testFromToString() {
        final CoordArrayConverter converter = new CoordArrayConverter();
        String a = "[(223380.4988791829;6758072.4280857295),(223404.67545027257;6758049.17275259),(223417.0127605943;6758038.021038004),(223450.67625251273;6757924.791645723),(223456.13332351885;6757906.359813054)]";
        Coord[] coords = converter.convert(a);
        Assertions.assertEquals(coords.length, 5);
        Assertions.assertEquals(coords[0].hasZ(), false);
        Assertions.assertEquals(coords[0].getX(), 223380.4988791829, 0.00005);
        Assertions.assertEquals(coords[0].getY(), 6758072.4280857295, 0.00005);
        Assertions.assertEquals(coords[1].hasZ(), false);
        Assertions.assertEquals(coords[1].getX(), 223404.67545027257, 0.00005);
        Assertions.assertEquals(coords[1].getY(), 6758049.17275259, 0.00005);
        Assertions.assertEquals(coords[2].hasZ(), false);
        Assertions.assertEquals(coords[2].getX(), 223417.0127605943, 0.00005);
        Assertions.assertEquals(coords[2].getY(), 6758038.021038004, 0.00005);
        Assertions.assertEquals(coords[3].hasZ(), false);
        Assertions.assertEquals(coords[3].getX(), 223450.67625251273, 0.00005);
        Assertions.assertEquals(coords[3].getY(), 6757924.791645723, 0.00005);
        Assertions.assertEquals(coords[4].hasZ(), false);
        Assertions.assertEquals(coords[4].getX(), 223456.13332351885, 0.00005);
        Assertions.assertEquals(coords[4].getY(), 6757906.359813054, 0.00005);

        String b = converter.convertToString(coords);
        Assertions.assertEquals(a, b);
    }

}
