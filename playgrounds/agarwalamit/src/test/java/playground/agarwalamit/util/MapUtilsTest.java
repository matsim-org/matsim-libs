/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.util;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import playground.agarwalamit.utils.MapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by amit on 19/09/16.
 */


public class MapUtilsTest {

    private final MatsimTestUtils helper = new MatsimTestUtils();

    @Test
    public void test(){

        Map<String, Integer> str2Int = new HashMap<>();
        str2Int.put("A",4);
        str2Int.put("B",8);
        str2Int.put("C",12);
        str2Int.put("D",16);

        //value sum
        int sumFromUtil = MapUtils.intValueSum(str2Int);
        int sum = 40;
        Assert.assertEquals("Sum is wrong",sum,sumFromUtil,MatsimTestUtils.EPSILON);

        Map<String, Double> str2Double = new HashMap<>();
        str2Double.put("A",4.);
        str2Double.put("B",8.);
        str2Double.put("C",12.);
        str2Double.put("D",16.);

        //value sum
        Assert.assertEquals("Sum is wrong",40.0, MapUtils.doubleValueSum(str2Double),MatsimTestUtils.EPSILON);

    }

}
