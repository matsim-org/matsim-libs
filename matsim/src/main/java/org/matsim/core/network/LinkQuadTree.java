
/* *********************************************************************** *
 * project: org.matsim.*
 * LinkQuadTree.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An optimized data structure to answer nearest-neighbor queries for links in a
 * network. Implementation is based on the idea of a MX-CIF quadtree (Kedem,
 * 1981).
 * <p>
 * "The MX-CIF quadtree of Kedem (1981) (see also Abel and Smith 1983) is a
 * region-based representation where each rectangle is associated with the
 * quadtree node corresponding to the smallest block which contains it in its
 * entirety. Subdivision ceases whenever a node's block contains no rectangles.
 * Alternatively, subdivision can also cease once a quadtree block is smaller
 * than a predetermined threshold size." (H. Samet, Hierarchical spatial data
 * structures, www.cs.umd.edu/~hjs/pubs/SametSSD89.pdf)
 * </p>
 * After having looked at the code for 5 min, I cannot, however, tell if this is
 * exactly what the code is doing, and if so, which version. kai, jul'12
 * <p>
 * The implementation splits nodes whenever a node contains more than one link
 * of which at least one can be assigned to a child node. According to the
 * description above, the leaf nodes would always be empty. This implementation
 * splits nodes whenever it is necessary, but not before, leading to leaf nodes
 * that actually may contain elements.  marcel, jul'12
 *
 * @author mrieser / senozon
 */
public final class LinkQuadTree {

	private final QuadTreeNode top;

	public LinkQuadTree(final double minX, final double minY, final double maxX, final double maxY) {
		this.top = new QuadTreeNode(minX, minY, maxX, maxY);
	}

	public void put(final Link link) {
		this.top.put(new LinkWrapper(link));
	}

	public Link getNearest(final double x, final double y) {
		LinkWrapper w = this.top.getNearest(x, y, new MutableDouble(Double.POSITIVE_INFINITY));
		if (w == null) {
			return null;
		}
		return w.link;
	}

	public void remove(final Link link) {
		this.top.remove(new LinkWrapper(link));
	}

	public double getMinEasting() {
		return this.top.minX;
	}

	public double getMaxEasting() {
		return this.top.maxX;
	}

	public double getMinNorthing() {
		return this.top.minY;
	}

	public double getMaxNorthing() {
		return this.top.maxY;
	}


	public Collection<Link> getDisk(final double x, final double y, final double distance) {
		return this.top.getDisk(x, y, distance, new ArrayList<>()).stream().map(w -> w.link).collect(Collectors.toList());
	}

	private static class QuadTreeNode {

//		private final static int NO_CHILD = -1;
//		private final static int CHILD_NW = 0;
//		private final static int CHILD_NE = 1;
//		private final static int CHILD_SE = 2;
//		private final static int CHILD_SW = 3;

		private enum ChildPosition {CHILD_NW, CHILD_NE, CHILD_SE, CHILD_SW, NO_CHILD}

		// I replaced the "int" by an enum since I find it easier to read, and I needed/wanted to understand the code.  If this causes
		// computational performance losses, we need to change it back.  kai, sep'16

		public final double minX;
		public final double minY;
		public final double maxX;
		public final double maxY;

		private final ArrayList<LinkWrapper> links = new ArrayList<>(3);
		private QuadTreeNode[] children = null;

		public QuadTreeNode(final double minX, final double minY, final double maxX, final double maxY) {
			this.minX = Math.min(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.maxX = Math.max(minX, maxX);
			this.maxY = Math.max(minY, maxY);
		}

		public void put(final LinkWrapper w) {
			if (this.children == null && this.links.isEmpty()) {
				// (means quadtree node neither has children nor contains a link yet)

				this.links.add(w);
				// (node now contains this link)
			} else {
				ChildPosition pos = getChildPosition(w);
				if (pos == ChildPosition.NO_CHILD) {
					// (i.e. link extends over more than one child node, so the current node is the smallest one that fully contains the link.
					this.links.add(w);
				} else {
					if (this.children == null) {
						split();
					}
					this.children[pos.ordinal()].put(w);
				}
			}
		}

		public boolean remove(final LinkWrapper w) {
			ChildPosition pos = getChildPosition(w);
			if (pos == ChildPosition.NO_CHILD || this.children == null) {
				for (int i = 0, n = this.links.size(); i < n; i++) {
					LinkWrapper w2 = this.links.get(i);
					if (w2.link.equals(w.link)) {
						this.links.remove(i);
						return true;
					}
				}
			} else {
				return this.children[pos.ordinal()].remove(w);
			}
			return false;
		}

		/**
		 * @param x
		 * @param y
		 * @param bestDistanceIndicator
		 * @return instance of LinkWrapper that is closest to (x,y) if closer than bestPseudoDistance, null otherwise.  Adjust
		 * bestPseudoDistance in first case
		 */
		public LinkWrapper getNearest(final double x, final double y, final MutableDouble bestDistanceIndicator) {
			LinkWrapper closest = null;

			// go through all links in current quadtree node (= box):
			for (LinkWrapper w : this.links) {
				double tmp = calcLineSegmentDistanceIndicator(x, y, w.link);
				if (tmp < bestDistanceIndicator.value) {
					bestDistanceIndicator.value = tmp;
					closest = w;
				}
			}

			// if we have no child nodes (= child boxes), return closest ...
			if (this.children == null) {
				return closest;
			}

			// otherwise, find the correct one ...
			ChildPosition childPos = this.getChildPosition(x, y);

			if (childPos == ChildPosition.NO_CHILD) {
				throw new RuntimeException("should never get here since (x,y) has to be _somewhere_ with respect to current Node.");
			}

			// (not sure we can have children, but none in the correct "direction"; depends on exact behavior of "split")

			// Find child in correct "direction" and do recursive call:
			LinkWrapper tmp = this.children[childPos.ordinal()].getNearest(x, y, bestDistanceIndicator);
			if (tmp != null) {
				closest = tmp;
			}

			// now also go through all _other_ children ...
			for (ChildPosition c : ChildPosition.values()) {
				//skip already visited children
				if (c == childPos || c == ChildPosition.NO_CHILD) {
					continue;
				}

				QuadTreeNode child = this.children[c.ordinal()];

				// For those other children, check if their bounding box is so close that we also need to look at them:
				if (child.calcDistanceIndicator(x, y) < bestDistanceIndicator.value) {

					// Only if the answer is yes, do a recursive call for actual LinkWrapper instances:
					tmp = child.getNearest(x, y, bestDistanceIndicator);
					if (tmp != null) {
						closest = tmp;
					}
				}
			}

			return closest;
		}

		/**
		 * Very similar to getNearest(), but returns all links within a given distance.
		 *
		 * @return Collection of LinkWrapper instances that are within distance of (x,y). Considers
		 */
		public Collection<LinkWrapper> getDisk(final double x, final double y, final double distance, Collection<LinkWrapper> result) {
			// go through all links in current quadtree node (= box):
			for (LinkWrapper w : this.links) {
				double tmp = Math.sqrt(calcLineSegmentDistanceIndicator(x, y, w.link));
				if (tmp <= distance) {
					result.add(w);
				}
			}

			// if we have no child nodes (= child boxes), return current result ...
			if (this.children == null) {
				return result;
			}

			// otherwise, find the correct one ...
			ChildPosition childPos = this.getChildPosition(x, y);

			if (childPos == ChildPosition.NO_CHILD) {
				throw new RuntimeException("should never get here since (x,y) has to be _somewhere_ with respect to current Node.");
			}

			// Find child in correct "direction" and do recursive call:
			this.children[childPos.ordinal()].getDisk(x, y, distance, result);

			// now also go through all _other_ children ...
			for (ChildPosition c : ChildPosition.values()) {
				//skip already visited children
				if (c == childPos || c == ChildPosition.NO_CHILD) {
					continue;
				}

				QuadTreeNode child = this.children[c.ordinal()];

				// For those other children, check if their bounding box is so close that we also need to look at them:
				if (Math.sqrt(child.calcDistanceIndicator(x, y)) <= distance) {

					// Only if the answer is yes, do a recursive call for actual LinkWrapper instances:
					child.getDisk(x, y, distance, result);
				}
			}

			return result;
		}

		private void split() {
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;
			this.children = new QuadTreeNode[4];
			this.children[ChildPosition.CHILD_NW.ordinal()] = new QuadTreeNode(this.minX, centerY, centerX, this.maxY);
			this.children[ChildPosition.CHILD_NE.ordinal()] = new QuadTreeNode(centerX, centerY, this.maxX, this.maxY);
			this.children[ChildPosition.CHILD_SE.ordinal()] = new QuadTreeNode(centerX, this.minY, this.maxX, centerY);
			this.children[ChildPosition.CHILD_SW.ordinal()] = new QuadTreeNode(this.minX, this.minY, centerX, centerY);

			List<LinkWrapper> keep = new ArrayList<>(this.links.size() / 2);
			for (LinkWrapper w : this.links) {
				ChildPosition pos = getChildPosition(w);
				if (pos == ChildPosition.NO_CHILD) {
					keep.add(w);
				} else {
					this.children[pos.ordinal()].put(w);
					// (seems to me that this cannot happen to more than one link since the quad tree node will split
					// as soon as a second link is added that can be assigned to a child node.  kai, jul'12)
					// (hä?  kai, aug'16)
				}
			}
			this.links.clear();
			this.links.ensureCapacity(keep.size() + 5);
			this.links.addAll(keep);
		}

		private ChildPosition getChildPosition(final LinkWrapper w) {
			// center of the bounding box of this quad tree node:
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;

			if (w.maxX < centerX && w.minY >= centerY) {
				// (bounding box of link lies fully to left and above the center of the quadtree node)
				return ChildPosition.CHILD_NW;
			}
			if (w.minX >= centerX && w.minY >= centerY) {
				return ChildPosition.CHILD_NE;
			}
			if (w.minX >= centerX && w.maxY < centerY) {
				return ChildPosition.CHILD_SE;
			}
			if (w.maxX < centerX && w.maxY < centerY) {
				return ChildPosition.CHILD_SW;
			}
			return ChildPosition.NO_CHILD;
			// (happens when bounding box of link overlaps more than one child node.. i.e. in particular with long links)
		}

		/**
		 * @param x
		 * @param y
		 * @return quad tree child node (=box) we need to look at if we are at (x,y)
		 */
		private ChildPosition getChildPosition(final double x, final double y) {
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;
			if (x < centerX && y >= centerY) {
				return ChildPosition.CHILD_NW;
			}
			if (x >= centerX && y >= centerY) {
				return ChildPosition.CHILD_NE;
			}
			if (x >= centerX/* && y < centerY // this is always true */) {
				return ChildPosition.CHILD_SE;
			}
//			if (x < centerX && y < centerY) { // this should now always be true
			return ChildPosition.CHILD_SW;
//			}
//			throw new RuntimeException("should never get here since (x,y) has to be _somewhere_ with respect to centerX and centerY") ;
		}

		/**
		 * Calculates the distance of a given point to the border of the
		 * rectangle. If the point lies within the rectangle, the distance
		 * is zero.
		 *
		 * @param x left-right location
		 * @param y up-down location
		 * @return distance to border, 0 if inside rectangle or on border
		 */
		private double calcDistanceIndicator(final double x, final double y) {
			double distanceX;
			double distanceY;

			if (this.minX <= x && x <= this.maxX) {
				distanceX = 0;
			} else {
				distanceX = Math.min(Math.abs(this.minX - x), Math.abs(this.maxX - x));
			}
			if (this.minY <= y && y <= this.maxY) {
				distanceY = 0;
			} else {
				distanceY = Math.min(Math.abs(this.minY - y), Math.abs(this.maxY - y));
			}

			return distanceX * distanceX + distanceY * distanceY;
			// (no Math.sqrt(), as it's only used to compare to each other, thus distance "indicator")
		}

	}

	// This is a helper method to calculate the square distance of a point to a link. Chooses the closest point on the link.
	private static double calcLineSegmentDistanceIndicator(final double x, final double y, final Link link) {

		double fx = link.getFromNode().getCoord().getX();
		double fy = link.getFromNode().getCoord().getY();
		double lineDX = link.getToNode().getCoord().getX() - fx;
		double lineDY = link.getToNode().getCoord().getY() - fy;

		if ((lineDX == 0.0) && (lineDY == 0.0)) {
			// the line segment is a point without dimension
			return calcDistanceIndicator(fx, fy, x, y);
		}

		// project (x | y) onto the line segment
		// values of u indicate the position of the projection on the line segment
		double u = ((x - fx) * lineDX + (y - fy) * lineDY) / (lineDX * lineDX + lineDY * lineDY);

		if (u <= 0) {
			// (x | y) is not on the line segment, but before lineFrom
			return calcDistanceIndicator(fx, fy, x, y);
		}
		if (u >= 1) {
			// (x | y) is not on the line segment, but after lineTo
			return calcDistanceIndicator(fx + lineDX, fy + lineDY, x, y);
		}

		// (x | y) is on the line segment
		return calcDistanceIndicator(fx + u * lineDX, fy + u * lineDY, x, y);
	}

	private static double calcDistanceIndicator(final double fromX, final double fromY, final double toX, final double toY) {
		double xDiff = toX - fromX;
		double yDiff = toY - fromY;
		return (xDiff * xDiff) + (yDiff * yDiff);
		// (no Math.sqrt(), as it's only used to compare to each other, thus distance "indicator")
	}

	private static class LinkWrapper {

		/*package*/ final double minX;
		/*package*/ final double minY;
		/*package*/ final double maxX;
		/*package*/ final double maxY;

		/*package*/ final Link link;

		public LinkWrapper(final Link link) {
			double fx = link.getFromNode().getCoord().getX();
			double fy = link.getFromNode().getCoord().getY();
			double tx = link.getToNode().getCoord().getX();
			double ty = link.getToNode().getCoord().getY();

			if (fx == tx) {
				// enforce minimal extent
				this.minX = fx - Math.abs(fx) * 1e-8; // make it adaptive within the number of significant digits
				this.maxX = fx + Math.abs(fx) * 1e-8; // make it adaptive within the number of significant digits
			} else {
				this.minX = Math.min(fx, tx);
				this.maxX = Math.max(fx, tx);
			}
			if (fy == ty) {
				this.minY = fy - Math.abs(fy) * 1e-8; // make it adaptive within the number of significant digits
				this.maxY = fy + Math.abs(fy) * 1e-8; // make it adaptive within the number of significant digits
			} else {
				this.minY = Math.min(fy, ty);
				this.maxY = Math.max(fy, ty);
			}

			this.link = link;
		}
	}

	private static class MutableDouble {
		public double value;

		public MutableDouble(final double value) {
			this.value = value;
		}
	}

}
