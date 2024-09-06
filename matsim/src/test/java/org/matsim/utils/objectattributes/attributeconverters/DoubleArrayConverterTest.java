
/* *********************************************************************** *
 * project: org.matsim.*
 * DoubleArrayConverterTest.java
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

	/**
 * @author jbischoff
 */
public class DoubleArrayConverterTest {


	 @Test
	 void testFromToString() {
        final DoubleArrayConverter converter = new DoubleArrayConverter();
        String a = "-0.1,0,0.0005,17.3,5.2E22";
        double[] array = converter.convert(a);
        Assertions.assertEquals(array.length, 5);
        Assertions.assertEquals(array[0], -0.1, 0.00005);
        Assertions.assertEquals(array[1], 0.0, 0.00005);
        Assertions.assertEquals(array[2], 0.0005, 0.00005);
        Assertions.assertEquals(array[3], 17.3, 0.00005);
        Assertions.assertEquals(array[4], 5.2E22, 0.00005);

        String b = converter.convertToString(array);
        Assertions.assertEquals("-0.1,0.0,5.0E-4,17.3,5.2E22", b);


    }

}
