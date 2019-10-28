
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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.utils.objectattributes.DoubleArrayConverter;

/**
 * @author jbischoff
 */
public class DoubleArrayConverterTest {


    @Test
    public void testFromToString() {
        final DoubleArrayConverter converter = new DoubleArrayConverter();
        String a = "-0.1,0,0.0005,17.3,5.2E22";
        double[] array = converter.convert(a);
        Assert.assertEquals(array.length, 5);
        Assert.assertEquals(array[0], -0.1, 0.00005);
        Assert.assertEquals(array[1], 0.0, 0.00005);
        Assert.assertEquals(array[2], 0.0005, 0.00005);
        Assert.assertEquals(array[3], 17.3, 0.00005);
        Assert.assertEquals(array[4], 5.2E22, 0.00005);

        String b = converter.convertToString(array);
        Assert.assertEquals("-0.1,0.0,5.0E-4,17.3,5.2E22", b);


    }

}
