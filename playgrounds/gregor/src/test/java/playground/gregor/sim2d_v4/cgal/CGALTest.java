package playground.gregor.sim2d_v4.cgal;/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CGALTest {

    @Test
    public void testIntersectCeoff() {

        {
            //parallel not collinear
            LineSegment s1 = LineSegment.createFromCoords(0, 0, 2, 2);
            LineSegment s2 = LineSegment.createFromCoords(2, 0, 4, 2);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(-1.)));
        }

        {
            //parallel collinear no intersection
            LineSegment s1 = LineSegment.createFromCoords(0, 0, 2, 2);
            LineSegment s2 = LineSegment.createFromCoords(3, 3, 5, 5);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(-2.)));
        }

        {
            //parallel collinear intersection in one point
            LineSegment s1 = LineSegment.createFromCoords(0, 0, 2, 2);
            LineSegment s2 = LineSegment.createFromCoords(2, 2, 5, 5);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(-3.)));
        }

        {
            //parallel collinear overlap
            LineSegment s1 = LineSegment.createFromCoords(0, 0, 2, 2);
            LineSegment s2 = LineSegment.createFromCoords(1, 1, 5, 5);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(-4.)));
        }

        {
            //no intersect w/ s1
            LineSegment s1 = LineSegment.createFromCoords(0, 0, 2, 2);
            LineSegment s2 = LineSegment.createFromCoords(0, 5, 5, 0);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(-5.)));
        }

        {
            //no intersect w/ s2
            LineSegment s1 = LineSegment.createFromCoords(0, 5, 5, 0);
            LineSegment s2 = LineSegment.createFromCoords(0, 0, 2, 2);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(-6.)));
        }

        {
            //intersection center of s1
            LineSegment s1 = LineSegment.createFromCoords(0, 5, 5, 0);
            double halfLength = Math.sqrt(5 * 5 + 5 * 5) / 2;
            LineSegment s2 = LineSegment.createFromCoords(0, 0, 3, 3);
            double coeff = CGAL.intersectCoeff(s1, s2);
            assertThat(coeff, is(equalTo(halfLength)));
        }


    }
}
