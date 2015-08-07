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

package playground.johannes.synpop.data;


import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author johannes
 */
public class PlainEpisodeTest extends TestCase {

    public void testNavigation() {
        Person person = new PlainPerson("1");
        Episode episode = new PlainEpisode();

        person.addEpisode(episode);

        Segment act0 = new PlainSegment();
        Segment leg0 = new PlainSegment();
        Segment act1 = new PlainSegment();
        Segment leg1 = new PlainSegment();
        Segment act2 = new PlainSegment();

        episode.addActivity(act0);
        episode.addActivity(act1);
        episode.addActivity(act2);

        episode.addLeg(leg0);
        episode.addLeg(leg1);

        Assert.assertEquals(episode.getPerson(), person);


        Assert.assertEquals(act0.previous(), null);
        Assert.assertEquals(act0.next(), leg0);
        Assert.assertEquals(leg0.previous(), act0);
        Assert.assertEquals(leg0.next(), act1);
        Assert.assertEquals(act1.previous(), leg0);
        Assert.assertEquals(act1.next(), leg1);
        Assert.assertEquals(leg1.previous(), act1);
        Assert.assertEquals(leg1.next(), act2);
        Assert.assertEquals(act2.previous(), leg1);
        Assert.assertEquals(act2.next(), null);
    }

//    @Rule
//    private final ExpectedException exception = ExpectedException.none();
//
//    @Test
//    public void testDuplicateEntries() {
//        Episode episode = new PlainEpisode();
//
//        Segment act = new PlainSegment();
//
//        episode.addActivity(act);
//        exception.expect(IllegalArgumentException.class);
//        episode.addActivity(act);
//    }
}
