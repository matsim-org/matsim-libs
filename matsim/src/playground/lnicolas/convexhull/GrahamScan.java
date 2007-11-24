/* *********************************************************************** *
 * project: org.matsim.*
 * GrahamScan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.convexhull;

import java.util.Stack;
import java.util.Vector;

import org.matsim.network.Node;

/**
 * This class only contains static methods for computing the convex hull.
 *
 * @version 1.1, 12/04/98
 * @author Pavol Federl
 */
public class GrahamScan
{

    /**
     * Compute a convex hull for a set of points using a modified Graham
     * scan algorithm, where only LEFT TURN calculation is used. LEFT TURN
     * is calculated using ESSA algorithm which correctly determines the
     * sign of a sum of n floating point numbers.
     *
     * @param points java.util.Vector
     * - points for which convex hull is to be calculated
     *
     * @return java.util.Vector
     * - convex hull (in counter-clockwise order)
     */
    public static Vector<Node> computeHull(Vector<Node> points)
    {
	// if there are less than 3 points, return the points
	// themselves as the convex hull
	if (points.size() < 3) {
		return points;
	}

	// compute the stairs
	Vector<Node> stairs = computeStairs( points);

	// if there are less than 3 points left in 'stairs', then return
	// stairs as the resulting convex hull
	if( stairs.size() < 3) {
		return stairs;
	}

	// get the first stairs-point (which is on the left edge)
	Node sm = stairs.firstElement();

	// start building the convex hull
	//   - sm is in the convex hull
	Stack<Node> hull = new Stack<Node>();
	hull.push(sm);

	// add the rest of the stairs to the hull
	// - to complete the convex hull properly, we have to make sure that
	//   we try to add stair[0] as the last point
	stairs.addElement( sm);
	int i = 1;
	while (i < stairs.size())
	{
	    // pi = next point to be processed
		Node pi = stairs.elementAt(i);

	    // pl = top point on the hull stack
		Node pl = hull.peek();

	    // if pi == pl, skip pi
	    if (pl.getId() == pi.getId()) {
	    	i = i + 1;
	    	continue;
	    }

	    // if there is only one point on the hull, add pi to the hull
	    if (hull.size() == 1) {
	    	hull.push( pi);
	    	i = i + 1;
	    	continue;
	    }

	    // there are at least two points on the hull - do the
	    // left-turn test
	    hull.pop();
	    Node pll = hull.peek();
	    if (leftTurn(pll, pl, pi)) {
	    	hull.push(pl);
	    	hull.push(pi);
	    	i = i + 1;
	    	continue;
	    }
	}

	// get rid of the last point, because it is already
	// at the beginning of the hull
	hull.pop();

	// return the convex hull
	return hull;
    }


    /**
     * Computes the stairs of points. Each quadrant (SW,SE,NE,NW) has stairs
     * in it. These stairs are computed and then concatenated to produce the
     * result. The property of these stairs is that the resulting points are
     * in counterclockwise order, and only candidates for the convex hull
     * are stored in the set.
     *
     * @param pts java.util.Vector
     * - set of points for which stairs are to be calculated
     *
     * @return java.util.Vector
     * - the computed stairs
     */
    public static Vector<Node> computeStairs(Vector<Node> pts) {
		// if there are no input points, return with no stairs
		if (pts.size() == 0) {
			return new Vector<Node>();
		}

		// find the bounding polygon of all points
		Node upl = pts.elementAt(0);
		Node upr = upl;
		Node lol = upl;
		Node lor = upl;
		Node leu = upl;
		Node led = upl;
		Node riu = upl;
		Node rid = upl;
		for (int i = 1; i < pts.size(); i++) {
			Node pi = pts.elementAt(i);
			// check if we have new left point
			if (pi.getCoord().getX() < leu.getCoord().getX()) {
				leu = led = pi;
			} else if (pi.getCoord().getX() == leu.getCoord().getX()) {
				if (leu.getCoord().getY() < pi.getCoord().getY()) {
					leu = pi;
				}
				if (led.getCoord().getY() > pi.getCoord().getY()) {
					led = pi;
				}
			}

			// check if we have new right point
			if (pi.getCoord().getX() > riu.getCoord().getX()) {
				riu = rid = pi;
			} else if (pi.getCoord().getX() == riu.getCoord().getX()) {
				if (riu.getCoord().getY() < pi.getCoord().getY()) {
					riu = pi;
				}
				if (rid.getCoord().getY() > pi.getCoord().getY()) {
					rid = pi;
				}
			}

			// check if we have new up point
			if (pi.getCoord().getY() > upl.getCoord().getY()) {
				upl = upr = pi;
			} else if (pi.getCoord().getY() == upl.getCoord().getY()) {
				if (pi.getCoord().getX() < upl.getCoord().getX()) {
					upl = pi;
				}
				if (pi.getCoord().getX() > upr.getCoord().getX()) {
					upr = pi;
				}
			}

			// check if we have new low point
			if (pi.getCoord().getY() < lol.getCoord().getY()) {
				lol = lor = pi;
			} else if (pi.getCoord().getY() == lol.getCoord().getY()) {
				if (pi.getCoord().getX() < lol.getCoord().getX()) {
					lol = pi;
				}
				if (pi.getCoord().getX() > lor.getCoord().getX()) {
					lor = pi;
				}
			}
		}

		// divide the input points into 4 rectangles (SouthEast, SouthWest,
		// NorthEast, NorthWest)
		Vector<Node> se = new Vector<Node>();
		Vector<Node> sw = new Vector<Node>();
		Vector<Node> ne = new Vector<Node>();
		Vector<Node> nw = new Vector<Node>();
		for (int i = 0; i < pts.size(); i++) {
			Node pi = pts.elementAt(i);

			// south-east
			if (pi.getCoord().getX() > lor.getCoord().getX()
					&& pi.getCoord().getY() < rid.getCoord().getY()) {
				se.addElement(pi);
			}
			// south-west
			if (pi.getCoord().getX() < lol.getCoord().getX()
					&& pi.getCoord().getY() < led.getCoord().getY()) {
				sw.addElement(pi);
			}
			// north-east
			if (pi.getCoord().getX() > upr.getCoord().getX()
					&& pi.getCoord().getY() > riu.getCoord().getY()) {
				ne.addElement(pi);
			}
			// north-west
			if (pi.getCoord().getX() < upl.getCoord().getX()
					&& pi.getCoord().getY() > leu.getCoord().getY()) {
				nw.addElement(pi);
			}
		}

		// here we store the result
		Vector<Node> res = new Vector<Node>();

		// add the points on the left edge to the result
		res.addElement(leu);
		if (led != res.lastElement()) {
			res.addElement(led);
		}

		// =================================================
		// add the stairs in the SOUTH-WEST rectangle
		// =================================================
		if (sw.size() > 0) {
			// sort SW points by increasing X-coordinate
			Sort.quick(sw, new ComparatorAdapter() {
				@Override
				public int compare(Object o1, Object o2) {
					if (((Node) o1).getCoord().getX() < ((Node) o2).getCoord().getX()) {
						return 1;
					} else {
						return -1;
					}
				}
			});

			// filter out points with strictly decreasing Y-coordinate
			Node p0 = led;
			Node pn = lol;
			Node last = p0;
			for (int i = 0; i < sw.size(); i++) {
				Node pi = sw.elementAt(i);
				if (last.getCoord().getY() > pi.getCoord().getY()) {
					if (leftTurn(p0, pi, pn)) {
						last = pi;
						res.addElement(last);
					}
				}
			}
		}

		// add points on the bottom edge
		if (lol != res.lastElement()) {
			res.addElement(lol);
		}
		if (lor != res.lastElement()) {
			res.addElement(lor);
		}

		// =================================================
		// add the stairs from the SOUTH-EAST rectangle
		// =================================================
		if (se.size() > 0) {
			// sort SE points by increasing Y-coordinate
			Sort.quick(se, new ComparatorAdapter() {
				@Override
				public int compare(Object o1, Object o2) {
					if (((Node) o1).getCoord().getY() < ((Node) o2).getCoord().getY()) {
						return 1;
					} else {
						return -1;
					}
				}
			});

			// filter out points with strictly increasing X-coordinate
			Node p0 = lor;
			Node pn = rid;
			Node last = p0;
			for (int i = 0; i < se.size(); i++) {
				Node pi = se.elementAt(i);
				if (last.getCoord().getX() < pi.getCoord().getX()) {
					if (leftTurn(p0, pi, pn)) {
						last = pi;
						res.addElement(last);
					}
				}
			}
		}

		// add points on the right edge
		if (rid != res.lastElement()) {
			res.addElement(rid);
		}
		if (riu != res.lastElement()) {
			res.addElement(riu);
		}

		// =================================================
		// add the stairs from the NORTH-EAST rectangle
		// =================================================
		if (ne.size() > 0) {
			// sort NE points by decreasing X-coordinate
			Sort.quick(ne, new ComparatorAdapter() {
				@Override
				public int compare(Object o1, Object o2) {
					if (((Node) o1).getCoord().getX() > ((Node) o2).getCoord().getX()) {
						return 1;
					} else {
						return -1;
					}
				}
			});

			// only filter out points with strictly increasing Y-coordinate
			Node p0 = riu;
			Node pn = upr;
			Node last = p0;
			for (int i = 0; i < ne.size(); i++) {
				Node pi = ne.elementAt(i);
				if (last.getCoord().getY() < pi.getCoord().getY()) {
					if (leftTurn(p0, pi, pn)) {
						last = pi;
						res.addElement(last);
					}
				}
			}
		}

		// add point on the top edge
		if (upr != res.lastElement()) {
			res.addElement(upr);
		}
		if (upl != res.lastElement()) {
			res.addElement(upl);
		}

		// =================================================
		// add the stairs from the NORTH-WEST rectangle
		// =================================================
		if (nw.size() > 0) {
			// sort NW points by decreasing Y-coordinate
			Sort.quick(nw, new ComparatorAdapter() {
				@Override
				public int compare(Object o1, Object o2) {
					if (((Node) o1).getCoord().getY() > ((Node) o2).getCoord().getY()) {
						return 1;
					} else {
						return -1;
					}
				}
			});

			// only filter out points with strictly decreasing in X-coordinate
			Node p0 = upl;
			Node pn = leu;
			Node last = p0;
			for (int i = 0; i < nw.size(); i++) {
				Node pi = nw.elementAt(i);
				if (last.getCoord().getX() > pi.getCoord().getX()) {
					if (leftTurn(p0, pi, pn)) {
						last = pi;
						res.addElement(last);
					}
				}
			}
		}

		// if the first and last points are the same, then remove
		// the last point
		if (res.size() > 1) {
			if (res.firstElement() == res.lastElement()) {
				res.removeElementAt(res.size() - 1);
			}
		}

		// return the computed stairs
		return res;
	}

    /**
	 * Determines whether two oriented line segments form a left turn. The line
	 * segments are specified by AB, and BC.
	 *
	 * @param a
	 *            convexHull.CPoint - point A
	 * @param b
	 *            convexHull.CPoint - point B
	 * @param c
	 *            convexHull.CPoint - point C
	 * @return bool - whether the line segments form a left turn
	 */
    public static boolean leftTurn(Node a, Node b, Node c) {
		double a1 = a.getCoord().getX();
		double a2 = a.getCoord().getY();
		double b1 = b.getCoord().getX();
		double b2 = b.getCoord().getY();
		double c1 = c.getCoord().getX();
		double c2 = c.getCoord().getY();

		boolean use_essa = true;
		if (use_essa) {
			// this would be the code if we didn't use ESSA
			double det = (b1 - a1) * (c2 - a2) - (b2 - a2) * (c1 - a1);
			if (det > 0)
				return true;
			else
				return false;
		} else {
			// use ESSA
			floatVector vec = new floatVector();
			vec.addElement(new Float(a1 * b2));
			vec.addElement(new Float(a2 * c1));
			vec.addElement(new Float(b1 * c2));
			vec.addElement(new Float(-b2 * c1));
			vec.addElement(new Float(-a2 * b1));
			vec.addElement(new Float(-a1 * c2));

			Essa essa = new Essa(vec);
			int determinant_sign = essa.sgsum();

			if (determinant_sign > 0)
				return true;
			else
				return false;
		}
	}

    /**
	 * Prints out a list of points.
	 *
	 * @param str
	 *            string to be printed before the list
	 * @param points
	 *            list of points
	 */
    public static void printPoints(String str, Vector<Node> points) {
		System.out.println("");
		System.out.println(str);
		System.out.println("----------------");
		System.out.println("");

		printPoints(points);
		System.out.println("");
	}

    /**
     * Prints out a list of points.
     *
     * @param points list of points
     */
	public static void printPoints(Vector<Node> points) {
		for (int i = 0; i < points.size(); i++) {
			Node p = points.elementAt(i);
			System.out.println((i + 1) + ") [" + p.getCoord().getX() + ","
					+ p.getCoord().getY() + "]");
		}
	}
}

