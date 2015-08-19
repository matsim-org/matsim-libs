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

package playground.johannes.synpop.sim.data;

import junit.framework.Assert;
import junit.framework.TestCase;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.PlainSegment;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class CachedElementTest extends TestCase {

    public void testSynchronization() {
        Segment e = new PlainSegment();

        String plainKey = "key";
        String plainValue = "1.2";
        e.setAttribute(plainKey, plainValue);

        CachedElement cache = new CachedSegment(e);
        Object objKey = new Object();
        Converters.register(plainKey, objKey, DoubleConverter.getInstance());

        Assert.assertEquals(cache.getAttribute(plainKey), plainValue);
        Assert.assertEquals(cache.getData(objKey), 1.2);
        Assert.assertEquals(cache.getAttribute("nonExistingKey"), null);
        Assert.assertEquals(cache.getData(new Object()), null);

        cache.setData(objKey, 1.3);

        Assert.assertEquals(cache.getData(objKey), 1.3);
        Assert.assertEquals(cache.getAttribute(plainKey), "1.3");

        cache.setAttribute(plainKey, "1.4");

        Assert.assertEquals(cache.getData(objKey), 1.4);

        cache.setAttribute(plainKey, null);

        Assert.assertEquals(cache.getData(objKey), null);

        cache.setAttribute(plainKey, "1.6");

        Assert.assertEquals(cache.getData(objKey), 1.6);

        cache.setData(objKey, null);

        Assert.assertEquals(cache.getAttribute(plainKey), "1.6");
    }
}
