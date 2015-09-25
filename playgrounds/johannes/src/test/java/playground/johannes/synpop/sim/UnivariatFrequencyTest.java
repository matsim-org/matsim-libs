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

import junit.framework.TestCase;
import org.junit.Assert;
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
public class UnivariatFrequencyTest extends TestCase {

    public void test() {
        Person ref1 = new PlainPerson("1");
        ref1.setAttribute("attribute", "1");

        Person ref2 = new PlainPerson("2");
        ref2.setAttribute("attribute", "1");

        Person ref3 = new PlainPerson("3");
        ref3.setAttribute("attribute", "2");

        Set<Person> refPersons = new HashSet<>();
        refPersons.add(ref1);
        refPersons.add(ref2);
        refPersons.add(ref3);

        Person sim1 = new PlainPerson("1");
        sim1.setAttribute("attribute", "2");
        CachedPerson c1 = new CachedPerson(sim1);

        Person sim2 = new PlainPerson("2");
        sim2.setAttribute("attribute", "2");
        CachedPerson c2 = new CachedPerson(sim2);

        Person sim3 = new PlainPerson("3");
        sim3.setAttribute("attribute", "3");
        CachedPerson c3 = new CachedPerson(sim3);

        Set<CachedPerson> cachedPersons = new HashSet<>();
        cachedPersons.add(c1);
        cachedPersons.add(c2);
        cachedPersons.add(c3);

        Object dataKey = Converters.register("attribute", DoubleConverter.getInstance());

        UnivariatFrequency uf = new UnivariatFrequency(refPersons, cachedPersons, "attribute", new LinearDiscretizer
                (1.0));

        Assert.assertEquals(3.0, uf.evaluate(null), 0.0);

        c1.setData(dataKey, 1.0);
        uf.onChange(dataKey, 2.0, 1.0, c1);
        Assert.assertEquals(1.5, uf.evaluate(null), 0.0);

        c3.setData(dataKey, 1.0);
        uf.onChange(dataKey, 3.0, 1.0, c3);
        Assert.assertEquals(0.0, uf.evaluate(null), 0.0);

    }
}
