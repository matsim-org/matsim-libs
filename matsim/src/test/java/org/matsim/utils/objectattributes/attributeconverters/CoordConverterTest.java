/* *********************************************************************** *
 * project: org.matsim.*
 * CoordConverterTest.java
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

public class CoordConverterTest {


	@Test
	void testFromToString() {
        final CoordConverter converter = new CoordConverter();
        String a = "(224489.3667496938;6757449.720111595)";
        Coord coord = converter.convert(a);
        Assertions.assertEquals(coord.hasZ(), false);
        Assertions.assertEquals(coord.getX(), 224489.3667496938, 0.00005);
        Assertions.assertEquals(coord.getY(), 6757449.720111595, 0.00005);

        String b = converter.convertToString(coord);
        Assertions.assertEquals(a, b);
    }

}
