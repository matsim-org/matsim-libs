package org.matsim.core.network;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

/**
 * An optimized data structure to answer nearest-neighbor queries for links in a
 * network. Implementation is based on the idea of a MX-CIF quadtree (Kedem,
 * 1981).
 * <p/>
 * "The MX-CIF quadtree of Kedem (1981) (see also Abel and Smith 1983) is a
 * region-based representation where each rectangle is associated with the
 * quadtree node corresponding to the smallest block which contains it in its
 * entirety. Subdivision ceases whenever a node's block contains no rectangles.
 * Alternatively, subdivision can also cease once a quadtree block is smaller
 * than a predetermined threshold size." (H. Samet, Hierarchical spatial data
 * structures, www.cs.umd.edu/~hjs/pubs/SametSSD89.pdf)
 * <p/>
 * After having looked at the code for 5 min, I cannot, however, tell if this is
 * exactly what the code is doing, and if so, which version. kai, jul'12
 *
 * The implementation splits nodes whenever a node contains more than one link
 * of which at least one can be assigned to a child node. According to the
 * description above, the leaf nodes would always be empty. This implementation
 * splits nodes whenever it is necessary, but not before, leading to leaf nodes
 * that actually may contain elements.  marcel, jul'12
 *
 * @author mrieser / senozon
 */
public final class LinkQuadTree {

	private final Node top;

	public LinkQuadTree(final double minX, final double minY, final double maxX, final double maxY) {
		this.top = new Node(minX, minY, maxX, maxY);
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

	private static class Node {

		private final static int NO_CHILD = -1;
		private final static int CHILD_NW = 0;
		private final static int CHILD_NE = 1;
		private final static int CHILD_SE = 2;
		private final static int CHILD_SW = 3;

		public final double minX;
		public final double minY;
		public final double maxX;
		public final double maxY;

		private final ArrayList<LinkWrapper> links = new ArrayList<>(3);
		private Node[] children = null;

		public Node(final double minX, final double minY, final double maxX, final double maxY) {
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
				int pos = getChildPosition(w);
				if (pos == NO_CHILD) {
					// (i.e. link extends over more than one child node, so the current node is the smallest one that fully contains the link.
					this.links.add(w);
				} else {
					if (this.children == null) {
						split();
					}
					this.children[pos].put(w);
				}
			}
		}

		public LinkWrapper getNearest(final double x, final double y, final MutableDouble bestDistance) {
			LinkWrapper closest = null;
			for (LinkWrapper w : this.links) {
				double tmp = calcLineSegmentPseudoDistance(x, y, w.link);
				if (tmp < bestDistance.value) {
					bestDistance.value = tmp;
					closest = w;
				}
			}
			if (this.children != null) {
				int childNo = this.getChildPosition(x, y);
				if (childNo != NO_CHILD) {
					LinkWrapper tmp = this.children[childNo].getNearest(x, y, bestDistance);
					if (tmp != null) {
						closest = tmp;
					}

					// my current intuition is that the following block should be one level up.  kai, jul'12
					for (int c = 0; c < 4; c++) {
						if (c != childNo) {
							Node child = this.children[c];
							if (child.calcPseudoDistance(x, y) < bestDistance.value) {
								tmp = child.getNearest(x, y, bestDistance);
								if (tmp != null) {
									closest = tmp;
								}
							}
						}
					}
				}
			}

			return closest;
		}

		private void split() {
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;
			this.children = new Node[4];
			this.children[CHILD_NW] = new Node(this.minX, centerY, centerX, this.maxY);
			this.children[CHILD_NE] = new Node(centerX, centerY, this.maxX, this.maxY);
			this.children[CHILD_SE] = new Node(centerX, this.minY, this.maxX, centerY);
			this.children[CHILD_SW] = new Node(this.minX, this.minY, centerX, centerY);

			List<LinkWrapper> keep = new ArrayList<>(this.links.size() / 2);
			for (LinkWrapper w : this.links) {
				int pos = getChildPosition(w);
				if (pos == NO_CHILD) {
					keep.add(w);
				} else {
					this.children[pos].put(w);
					// (seems to me that this cannot happen to more than one link since the quadtree node will split
					// as soon as a second link is added that can be assigned to a child node.  kai, jul'12)
				}
			}
			this.links.clear();
			this.links.ensureCapacity(keep.size() + 5);
			this.links.addAll(keep);
		}

		private int getChildPosition(final LinkWrapper w) {
			// center of the bounding box of this quadtree node:
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;

			if (w.maxX < centerX && w.minY >= centerY) {
				// (bounding box of link lies fully to left and above the center of the quadtree node)
				return CHILD_NW;
			}
			if (w.minX >= centerX && w.minY >= centerY) {
				return CHILD_NE;
			}
			if (w.minX >= centerX && w.maxY < centerY) {
				return CHILD_SE;
			}
			if (w.maxX < centerX && w.maxY < centerY) {
				return CHILD_SW;
			}
			return NO_CHILD;
			// (happens when bounding box of link overlaps more than one child node.. i.e. in particular with long links)
		}

		private int getChildPosition(final double x, final double y) {
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;
			if (x < centerX && y >= centerY) {
				return CHILD_NW;
			}
			if (x >= centerX && y >= centerY) {
				return CHILD_NE;
			}
			if (x >= centerX && y < centerY) {
				return CHILD_SE;
			}
			if (x < centerX && y < centerY) {
				return CHILD_SW;
			}
			// this should never happen...
			return NO_CHILD;
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
		private double calcPseudoDistance(final double x, final double y) {
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

			return distanceX * distanceX + distanceY * distanceY; // no Math.sqrt(), as it's only used to compare to each other, thus "pseudo distance"
		}

	}

	private static double calcLineSegmentPseudoDistance(final double x, final double y, final Link link) {

		double fx = link.getFromNode().getCoord().getX();
		double fy = link.getFromNode().getCoord().getY();
		double lineDX = link.getToNode().getCoord().getX() - fx;
		double lineDY = link.getToNode().getCoord().getY() - fy;

		if ((lineDX == 0.0) && (lineDY == 0.0)) {
			// the line segment is a point without dimension
			return calcPseudoDistance(fx, fy, x, y);
		}

		double u = ((x - fx)*lineDX + (y - fy)*lineDY) / (lineDX*lineDX + lineDY*lineDY);

		if (u <= 0) {
			// (x | y) is not on the line segment, but before lineFrom
			return calcPseudoDistance(fx, fy, x, y);
		}
		if (u >= 1) {
			// (x | y) is not on the line segment, but after lineTo
			return calcPseudoDistance(fx + lineDX, fy + lineDY, x, y);
		}
		return calcPseudoDistance(fx + u*lineDX, fy + u*lineDY, x, y);

	}

	private static double calcPseudoDistance(final double fromX, final double fromY, final double toX, final double toY) {
		double xDiff = toX - fromX;
		double yDiff = toY - fromY;
		return (xDiff*xDiff) + (yDiff*yDiff); // no Math.sqrt, as we use the values only to compare to each other. Thus "pseudo distance"
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
				this.minX = fx - fx*1e-8; // make it adaptive within the number of significant digits
				this.maxX = fx + fx*1e-8; // make it adaptive within the number of significant digits
			} else {
				this.minX = Math.min(fx, tx);
				this.maxX = Math.max(fx, tx);
			}
			if (fy == ty) {
				this.minY = fy - fy*1e-8; // make it adaptive within the number of significant digits
				this.maxY = fy + fy*1e-8; // make it adaptive within the number of significant digits
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
