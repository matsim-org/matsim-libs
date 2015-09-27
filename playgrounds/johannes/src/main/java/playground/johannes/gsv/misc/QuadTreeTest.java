/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.gsv.misc;

import org.matsim.core.utils.collections.QuadTree;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import java.util.Random;

/**
 * @author jillenberger
 */
public class QuadTreeTest {

    public static void main(String args[]) {
        double minx = 0;
        double miny = 0;
        double maxx = 100;
        double maxy = 100;

        org.matsim.core.utils.collections.QuadTree<Object> quadTree1 = new QuadTree<>(minx, miny, maxx, maxy);
        playground.johannes.gsv.misc.QuadTree quadTree2 = new playground.johannes.gsv.misc.QuadTree(minx, miny, maxx, maxy);

        Random random = new XORShiftRandom();

        System.out.println("Inserting points");
        for(int i = 0; i < 10000; i++) {
            double x = random.nextDouble() * maxx;
            double y = random.nextDouble() * maxy;

            quadTree1.put(x, y, new Object());
            quadTree2.put(x, y, new Object());
        }

        double[] xcoords = new double[1000];
        double[] ycoords = new double[1000];

        for(int i = 0; i < xcoords.length; i++) {
            xcoords[i] = random.nextDouble() * maxx;
            ycoords[i] = random.nextDouble() * maxy;
        }

        System.out.println("Reading...");

        long time = System.currentTimeMillis();
        for(int i = 0; i < xcoords.length; i++) {
            quadTree2.get(xcoords[i], ycoords[i], 90, 100);
        }
        time = System.currentTimeMillis() - time;
        System.out.println("QuadTree2 " + time);


        time = System.currentTimeMillis();
        for(int i = 0; i < xcoords.length; i++) {
            quadTree1.get(xcoords[i], ycoords[i], 100);

        }
        time = System.currentTimeMillis() - time;
        System.out.println("QuadTree1 " + time);

    }
}
