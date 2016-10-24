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
import playground.agarwalamit.utils.ListUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Created by amit on 19/09/16.
 */


public class ListUtilTest {

    @Test
    public void test(){

        List<Integer> ints = Arrays.asList(4,8,12,16);

        Assert.assertEquals("Sum is wrong",40, ListUtils.intSum(ints), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Mean is wrong",40/4, ListUtils.intMean(ints), MatsimTestUtils.EPSILON);

        List<Double> doubles = Arrays.asList(4.0,8.0,12.0,16.0);

        Assert.assertEquals("Sum is wrong",40., ListUtils.doubleSum(doubles), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Mean is wrong",40./4, ListUtils.doubleMean(doubles), MatsimTestUtils.EPSILON);
    }


}
