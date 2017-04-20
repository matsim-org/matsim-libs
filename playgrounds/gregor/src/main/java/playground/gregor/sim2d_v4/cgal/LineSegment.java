/* *********************************************************************** *
 * project: org.matsim.*
 * Segment.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.cgal;

import org.matsim.api.core.v01.network.Link;

public class LineSegment {
    public double x0;
    public double x1;
    public double y0;
    public double y1;
    public double dx;//normalized!!
    public double dy;//normalized!!
    public double length;
    public double width;

    public static LineSegment createFromLink(Link link) {
        double fromX = link.getFromNode().getCoord().getX();
        double toX = link.getToNode().getCoord().getX();
        double fromY = link.getFromNode().getCoord().getY();
        double toY = link.getToNode().getCoord().getY();

        //this requires a planar coordinate system
        double dx = toX - fromX;
        double dy = toY - fromY;
        double projLength = Math.sqrt(dx * dx + dy * dy);
        if (projLength == 0) {
            dx = 1;
            dy = 1;
            projLength = 1;
        }
        dx /= projLength;
        dy /= projLength;
        LineSegment s = new LineSegment();
        s.x0 = fromX;
        s.x1 = toX;
        s.y0 = fromY;
        s.y1 = toY;
        s.dx = dx;
        s.dy = dy;
        s.length = projLength;
        s.width = link.getCapacity() / 1.33;
        return s;
    }

    public static LineSegment createFromCoords(double fromX, double fromY, double toX, double toY) {
        //this requires a planar coordinate system
        double dx = toX - fromX;
        double dy = toY - fromY;
        double projLength = Math.sqrt(dx * dx + dy * dy);
        if (projLength == 0) {
            dx = 1;
            dy = 1;
            projLength = 1;
        }
        dx /= projLength;
        dy /= projLength;
        LineSegment s = new LineSegment();
        s.x0 = fromX;
        s.x1 = toX;
        s.y0 = fromY;
        s.y1 = toY;
        s.dx = dx;
        s.dy = dy;
        s.length = projLength;
        s.width = 0;
        return s;
    }

    public LineSegment getInverse() {
        LineSegment s = new LineSegment();
        s.x0 = x1;
        s.y0 = y1;
        s.x1 = x0;
        s.y1 = y0;
        s.dx = -dx;
        s.dy = -dy;
        s.length = length;
        s.width = width;
        return s;
    }

    public boolean equalInverse(LineSegment other) {
        return Math.abs(this.x0 - other.x1) < CGAL.EPSILON && Math.abs(this.x1 - other.x0) < CGAL.EPSILON && Math.abs(this.y0 - other.y1) < CGAL.EPSILON && Math.abs(this.y1 - other.y0) < CGAL.EPSILON;
    }

    // pseudo angle, e.g. for sorting by angle
    // cf. http://stackoverflow.com/questions/16542042/fastest-way-to-sort-vectors-by-angle-without-actually-computing-that-angle
    public double getPseudoAngle() {
//        return Math.atan2(dx,dy);
        double p = dx / (Math.abs(dx) + Math.abs(dy));
        if (dy < 0) {
            return p - 1;
        }
        return 1 - p;
    }

    @Override
    public String toString() {
        return this.x0 + ":" + this.y0 + "  " + this.x1 + ":" + this.y1;
    }
}
