package org.matsim.core.network;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

/**
 * @author mrieser / senozon
 *
 * An optimized data structure to answer nearest-neighbor queries for links in a network.
 * Implementation is based on the idea of a MX-CIF quadtree (Kedem, 1981).
 */
public class LinkQuadTree {

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

		private final ArrayList<LinkWrapper> links = new ArrayList<LinkWrapper>(3);
		private Node[] childs = null;

		public Node(final double minX, final double minY, final double maxX, final double maxY) {
			this.minX = Math.min(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.maxX = Math.max(minX, maxX);
			this.maxY = Math.max(minY, maxY);
		}

		public void put(final LinkWrapper w) {
			if (this.childs == null && this.links.isEmpty()) {
				this.links.add(w);
			} else {
				int pos = getChildPosition(w);
				if (pos == NO_CHILD) {
					this.links.add(w);
				} else {
					if (this.childs == null) {
						split();
					}
					this.childs[pos].put(w);
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
			if (this.childs != null) {
				int childNo = this.getChildPosition(x, y);
				if (childNo != NO_CHILD) {
					LinkWrapper tmp = this.childs[childNo].getNearest(x, y, bestDistance);
					if (tmp != null) {
						closest = tmp;
					}
					for (int c = 0; c < 4; c++) {
						if (c != childNo) {
							Node child = this.childs[c];
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
			this.childs = new Node[4];
			this.childs[CHILD_NW] = new Node(this.minX, centerY, centerX, this.maxY);
			this.childs[CHILD_NE] = new Node(centerX, centerY, this.maxX, this.maxY);
			this.childs[CHILD_SE] = new Node(centerX, this.minY, this.maxX, centerY);
			this.childs[CHILD_SW] = new Node(this.minX, this.minY, centerX, centerY);

			List<LinkWrapper> keep = new ArrayList<LinkWrapper>(this.links.size() / 2);
			for (LinkWrapper w : this.links) {
				int pos = getChildPosition(w);
				if (pos == NO_CHILD) {
					keep.add(w);
				} else {
					this.childs[pos].put(w);
				}
			}
			this.links.clear();
			this.links.ensureCapacity(keep.size() + 5);
			this.links.addAll(keep);
		}

		private int getChildPosition(final LinkWrapper w) {
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;
			if (w.maxX < centerX && w.minY > centerY) {
				return CHILD_NW;
			}
			if (w.minX > centerX && w.minY > centerY) {
				return CHILD_NE;
			}
			if (w.minX > centerX && w.maxY < centerY) {
				return CHILD_SE;
			}
			if (w.maxX < centerX && w.maxY < centerY) {
				return CHILD_SW;
			}
			return NO_CHILD;
		}

		private int getChildPosition(final double x, final double y) {
			double centerX = (minX + maxX) / 2;
			double centerY = (minY + maxY) / 2;
			if (x < centerX && y > centerY) {
				return CHILD_NW;
			}
			if (x > centerX && y > centerY) {
				return CHILD_NE;
			}
			if (x > centerX && y < centerY) {
				return CHILD_SE;
			}
			if (x < centerX && y < centerY) {
				return CHILD_SW;
			}
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
			
			this.minX = Math.min(fx, tx);
			this.minY = Math.min(fy, ty);
			this.maxX = Math.max(fx, tx);
			this.maxY = Math.max(fy, ty);

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
