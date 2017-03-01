/* *********************************************************************** *
 * project: org.matsim.*
 * CGAL.java
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

/**
 * This class provides basic computational geometry algorithms
 *
 * @author laemmel
 */
public abstract class CGAL {

    public static final double EPSILON = .0001;


    /**
     * tests whether coordinate x0,y0 is located left of the infinite vector that runs from x1,y1  to x2,y2
     *
     * @param x0 the x-coordinate to test
     * @param y0 the y-coordinate to test
     * @param x1 first x-coordinate of the vector
     * @param y1 first y-coordinate of the vector
     * @param x2 second x-coordinate of the vector
     * @param y2 second y-coordinate of the vector
     * @return >0 if coordinate is left of the vector
     * ==0 if coordinate is on the vector
     * <0 if coordinate is right of the vector
     */
    public static double isLeftOfLine(double x0, double y0, double x1, double y1, double x2, double y2) {
        return (x2 - x1) * (y0 - y1) - (x0 - x1) * (y2 - y1);
    }


    /**
     * tests whether coordinate x0,y0 is located on the infinite vector defined by coordinates x1,y1 and x2,y2
     *
     * @param x0 the x-coordinate to test
     * @param y0 the y-coordinate to test
     * @param x1 first x-coordinate of the vector
     * @param y1 first y-coordinate of the vector
     * @param x2 second x-coordinate of the vector
     * @param y2 second y-coordinate of the vector
     * @return true if coordinate is on the vector
     */
    public static boolean isOnVector(double x0, double y0, double x1, double y1, double x2, double y2) {
        double left = isLeftOfLine(x0, y0, x1, y1, x2, y2);
        return left * left < EPSILON;
    }

    /**
     * calculates the signed distance of a point to a line (given by a vector)
     * a negative value indicates that the point is on the left side of the defining vector
     * and a positive value indicates that the point is on right side of the defining vector
     *
     * @param px  point's x-coordinate
     * @param py  point's y-coordinate
     * @param lx0 x-coordinate of the vector's origin
     * @param ly0 y-coordinate of the vector's origin
     * @param dxl normalized vector's x-direction
     * @param dyl normalized vector's y-direction
     * @return signed distance
     */
    public static double signDistPointLine(double px, double py, double lx0, double ly0, double dxl, double dyl) {
        //		double r = (px - lx0) * dxl + (py - ly0) * dyl;
        double s = ((ly0 - py) * dxl - (lx0 - px) * dyl);

        return s;
    }

    /**
     * calculates the coefficient r by which a given vector has to be multiplied to get the perpendicular projection on the line
     * defined by the vector for a given point
     *
     * @param x   x-coordinate of the point
     * @param y   y-coordinate of the point
     * @param v0x first x-coordinate of the vector
     * @param v0y first y-coordinate of the vector
     * @param v1x second x-coordinate of the vector
     * @param v1y second y-coordinate of the vector
     * @return the coefficient by which the vector has to be multiplied
     */
    public static double vectorCoefOfPerpendicularProjection(double x, double y, double v0x, double v0y, double v1x, double v1y) {
        double vdx = v1x - v0x;
        double vdy = v1y - v0y;
        double numerator = (x - v0x) * vdx + (y - v0y) * vdy;
        double denomenator = vdx * vdx + vdy * vdy; //TODO in most sim2d cases this value can be precalculated [gl Jan'13]
        double r = numerator / denomenator;

        return r;

    }

    /**
     * calculates the coefficient r by which a given normalized vector has to be multiplied to get the perpendicular projection on the line
     * defined by the vector for a given point
     *
     * @param x   x-coordinate of the point
     * @param y   y-coordinate of the point
     * @param v0x first x-coordinate of the vector
     * @param v0y first y-coordinate of the vector
     * @param vdx x-direction of the vector
     * @param vdy y-direction of the vector
     * @return the coefficient by which the vector has to be multiplied
     */
    public static double normVectorCoefOfPerpendicularProjection(double x, double y, double v0x, double v0y, double vdx, double vdy) {
        double numerator = (x - v0x) * vdx + (y - v0y) * vdy;
        double denomenator = vdx * vdx + vdy * vdy; //TODO in most sim2d cases this value can be precalculated [gl Jan'13]
        double r = numerator / denomenator;

        return r;

    }

    /**
     * dot product of two 2d vectors
     *
     * @param x0 the x-coordinate of the first vector
     * @param y0 the y-coordinate of the first vector
     * @param x1 the x-coordinate of the second vector
     * @param y1 the y-coordinate of the second vector
     * @return the dot product
     */
    public static double dot(double x0, double y0, double x1, double y1) {
        return x0 * x1 + y0 * y1;
    }

    /**
     * dot product of two 2d vectors
     *
     * @param p0 vector 1
     * @param p1 vector 2
     * @return the dot product
     */
    public static double dot(Vector p0, Vector p1) {
        return dot(p0.x, p0.y, p1.x, p1.y);
    }

    /**
     * perp dot product of two 2d vectors
     *
     * @param x0 the x-coordinate of the first vector
     * @param y0 the y-coordinate of the first vector
     * @param x1 the x-coordinate of the second vector
     * @param y1 the y-coordinate of the second vector
     * @return the perp dot product
     */
    public static double perpDot(double x0, double y0, double x1, double y1) {
        return x0 * y1 - y0 * x1;
    }

    /**
     * perp dot product of two 2d vectors
     *
     * @param u vector 1
     * @param v vector 2
     * @return the perp dot product
     */
    public static double perpDot(Vector u, Vector v) {
        return perpDot(u.x, u.y, v.x, v.y);
    }


    /**
     * determinant of 2x2 square matrix
     *
     * @param x0
     * @param x1
     * @param y0
     * @param y1
     * @return determinant
     */
    public static double det(double x0, double x1,
                             double y0, double y1) {

        return x0 * y1 - y0 * x1;
    }


    /**
     * 2D intersection of 2 line segments
     * cf. http://geomalgorithms.com/a05-_intersect-1.html
     *
     * @param s1 the line segment for which the intersection coefficient is calculated
     * @param s2 the intersecting line segment
     * @return the intersection coefficient
     */
    public static double intersectCoeff(LineSegment s1, LineSegment s2) {
        Vector u = new Vector(s1.x1 - s1.x0, s1.y1 - s1.y0);
        Vector v = new Vector(s2.x1 - s2.x0, s2.y1 - s2.y0);
        Vector w = new Vector(s1.x0 - s2.x0, s1.y0 - s2.y0);
        double d = perpDot(u, v);

        if (Math.abs(d) < EPSILON) { //s1 and s2 are parallel
            if (perpDot(u, w) != 0 || perpDot(v, w) != 0) {
                return -1; // not collinear, i.e. no intersection
            }
            double du = dot(u, v);
            double dv = dot(v, v);
            if (du == 0 || dv == 0) { //one ore both segs are points
                return 0;
            }
            double t0, t1;
            Vector w2 = new Vector(s1.x1 - s2.x0, s1.y1 - s2.y0);
            if (v.x != 0) {
                t0 = w.x / v.x;
                t1 = w2.x / v.x;
            } else {
                t0 = w.x / v.y;
                t1 = w2.y / v.y;
            }
            if (t0 > t1) { //swap
                double t = t0;
                t0 = t1;
                t1 = t;
            }
            if (t0 > 1 || t1 < 0) {
                return -2; //no overlap
            }
            t0 = t0 < 0 ? 0 : t0;
            t1 = t1 > 1 ? 1 : t1;
            if (t0 == t1) { //intersect is a point
//                Vector i0 = new Vector(s2.x0+t0*v.x, s2.y0+t0*v.y);
                return -3;
            }
            //overlap is a segment
//            Vector i0 = new Vector(s2.x0+t0*v.x, s2.y0+t0*v.y);
//            Vector i1 = new Vector(s2.x0+t1*v.x, s2.y0+t1*v.y);
            return -4;
        }
        //not parallel
        double sI = perpDot(v, w) / d;
        if (sI < 0 || sI > 1) {
            return -5; //no intersect w/ s1
        }

        double tI = perpDot(u, w) / d;
        if (tI < 0 || tI > 1) {
            return -6; //no intersect w/ s2;
        }

//        Vector i0 = new Vector(s1.x0 + sI * u.x, s1.y0 + sI * u.y);

        return sI * s1.length;
    }

    /**
     * If ls1 and ls2 prune ls1 so it starts at the intersection point and leave ls1 untouched otherwise.
     *
     * @param ls1 the line segment to prune
     * @param ls2 the intersecting line segment
     */
    public static void pruneIntersectingLineSegments(LineSegment ls1, LineSegment ls2) {
        LineSegment ls1RB = LineSegment.createFromCoords(ls1.x0 + ls1.dy * ls1.width / 2, ls1.y0 - ls1.dx * ls1.width / 2, ls1.x1 + ls1.dy * ls1.width / 2, ls1.y1 - ls1.dx * ls1.width / 2);
        LineSegment ls1LB = LineSegment.createFromCoords(ls1.x0 - ls1.dy * ls1.width / 2, ls1.y0 + ls1.dx * ls1.width / 2, ls1.x1 - ls1.dy * ls1.width / 2, ls1.y1 + ls1.dx * ls1.width / 2);

        LineSegment ls2RB = LineSegment.createFromCoords(ls2.x0 + ls2.dy * ls2.width / 2, ls2.y0 - ls2.dx * ls2.width / 2, ls2.x1 + ls2.dy * ls2.width / 2, ls2.y1 - ls2.dx * ls2.width / 2);
        LineSegment ls2LB = LineSegment.createFromCoords(ls2.x0 - ls2.dy * ls2.width / 2, ls2.y0 + ls2.dx * ls2.width / 2, ls2.x1 - ls2.dy * ls2.width / 2, ls2.y1 + ls2.dx * ls2.width / 2);

        double coeff1 = CGAL.intersectCoeff(ls1RB, ls2RB);
        double coeff2 = CGAL.intersectCoeff(ls1RB, ls2LB);
        double coeff3 = CGAL.intersectCoeff(ls1LB, ls2LB);
        double coeff4 = CGAL.intersectCoeff(ls1LB, ls2RB);

        double amount = Math.max(Math.max(coeff2, coeff4), Math.max(coeff1, coeff3));
        if (amount > 0) {
            ls1.x0 = ls1.x0 + ls1.dx * amount;
            ls1.y0 = ls1.y0 + ls1.dy * amount;
            ls1.length -= amount;
        }
    }


    public static final class Vector {
        double x;
        double y;

        public Vector(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }


}
