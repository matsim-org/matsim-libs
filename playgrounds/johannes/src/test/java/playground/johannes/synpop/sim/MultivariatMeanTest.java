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

package playground.johannes.synpop.sim;

import junit.framework.Assert;
import junit.framework.TestCase;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class MultivariatMeanTest extends TestCase {

    public void test() {
        Person refPerson1 = new PlainPerson("1");
        refPerson1.setAttribute("attribute1", "1");
        refPerson1.setAttribute("attribute2", "1");

        Person refPerson2 = new PlainPerson("2");
        refPerson2.setAttribute("attribute1", "3");
        refPerson2.setAttribute("attribute2", "1");

        Set<Person> refPersons = new HashSet<>();
        refPersons.add(refPerson1);
        refPersons.add(refPerson2);

        Person simPerson1 = new PlainPerson("1");
        simPerson1.setAttribute("attribute1", "1");
        simPerson1.setAttribute("attribute2", "0");

        Person simPerson2 = new PlainPerson("2");
        simPerson2.setAttribute("attribute1", "3");
        simPerson2.setAttribute("attribute2", "5");

        CachedPerson cachedPerson1 = new CachedPerson(simPerson1);
        CachedPerson cachedPerson2 = new CachedPerson(simPerson2);

        Set<CachedPerson> simPersons = new HashSet<>();
        simPersons.add(cachedPerson1);
        simPersons.add(cachedPerson2);

        Object dataKey1 = new Object();
        Object dataKey2 = new Object();
        Converters.register("attribute1", dataKey1, DoubleConverter.getInstance());
        Converters.register("attribute2", dataKey2, DoubleConverter.getInstance());

        MultivariatMean mm = new MultivariatMean(refPersons, simPersons, "attribute1", "attribute2", new
                LinearDiscretizer(1));

        Assert.assertEquals(5.0, mm.evaluate(null));

        cachedPerson1.setData(dataKey2, 1.0);
        mm.onChange(dataKey2, 0, 1, cachedPerson1);
        Assert.assertEquals(4.0, mm.evaluate(null));

        cachedPerson1.setData(dataKey1, 0.0);
        mm.onChange(dataKey1, 1, 0, cachedPerson1);
        Assert.assertEquals(4.0, mm.evaluate(null));

        cachedPerson1.setData(dataKey2, 0.0);
        mm.onChange(dataKey2, 1, 0, cachedPerson1);
        Assert.assertEquals(4.0, mm.evaluate(null));

        cachedPerson1.setData(dataKey1, 3.0);
        mm.onChange(dataKey1, 0.0, 3.0, cachedPerson1);
        Assert.assertEquals(1.5, mm.evaluate(null));
    }


}
