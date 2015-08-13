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

package playground.johannes.synpop.sim.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author johannes
 */
public class DynamicIntArrayTest extends TestCase {

    public void test1() {
        DynamicIntArray array = new DynamicIntArray();

        array.set(0, 100);
        array.set(11, 111);
        array.set(4, 104);

        Assert.assertEquals(array.get(0), 100);
        Assert.assertEquals(array.get(4), 104);
        Assert.assertEquals(array.get(11), 111);
        Assert.assertEquals(array.get(1), array.naValue);

        array.set(23, 123);

        Assert.assertEquals(array.get(23), 123);
        Assert.assertEquals(array.get(4887), array.naValue);
    }

    public void test2() {
        DynamicIntArray array = new DynamicIntArray(100, 0);

        Assert.assertEquals(array.get(234), 0);
        Assert.assertEquals(array.get(0), 0);

        array.set(99, 2);
        Assert.assertEquals(array.get(99), 2);

        array.set(102, 4);
        Assert.assertEquals(array.get(102), 4);

        Assert.assertEquals(array.get(101), 0);
    }
}
