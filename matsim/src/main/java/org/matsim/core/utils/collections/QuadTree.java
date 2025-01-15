/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTree.java
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

package org.matsim.core.utils.collections;

import java.io.Serializable;
import java.util.*;

/**
 * An implementation of a QuadTree to store data assigned to geometric points.
 * The expected bounds of all added points must be given to the constructor for
 * working properly. Correct functioning of the QuadTree with elements being
 * added outside of the given bounds cannot be guaranteed.<br />
 * At one location, several different objects can be put. An object can be put
 * to the QuadTree at different locations. But an object cannot be put more than
 * once at the same location.
 *
 * @author mrieser
 * @param <T> The type of data to be stored in the QuadTree.
 */
public class QuadTree<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The top node or root of the tree */
	protected Node<T> top = null;

	/** The number of entries in the tree */
	private int size = 0;

	/** The number of structural modifications to the tree. */
	private transient int modCount = 0;

	/**
	 * A cache to store all values of the QuadTree so it does not have to be
	 * computed for every call to {@link #values()}. This is similar to
	 * TreeMap.java and AbstractMap.java
	 */
	transient volatile Collection<T> values = null;

	private void incrementSize() { this.modCount++; this.size++; this.values = null; }
	private void decrementSize() { this.modCount++; this.size--; this.values = null; }

	/**
	 * Creates an empty QuadTree with the bounds minX/minY -- maxX/maxY. For
	 * optimal performance, all points should be evenly distributed within this
	 * rectangle.
	 *
	 * @param minX The smallest x coordinate (easting, longitude) expected
	 * @param minY The smallest y coordinate (northing, latitude) expected
	 * @param maxX The largest x coordinate (easting, longitude) expected
	 * @param maxY The largest y coordinate (northing, latitude) expected
	 */
	public QuadTree(final double minX, final double minY, final double maxX, final double maxY) {
		this.top = new Node<>(minX, minY, maxX, maxY);
	}

	/**
	 * Associates the specified value with the specified coordinates in this
	 * QuadTree.
	 *
	 * @param x x-coordinate where the specified value is to be associated.
	 * @param y y-coordinate where the specified value is to be associated.
	 * @param value value to be associated with the specified coordinates.
	 *
	 * @return true if insertion was successful and the data structure changed,
	 *         false otherwise.
	 */
	public boolean put(final double x, final double y, final T value) {
		if (!this.top.bounds.containsOrEquals(x, y)) {
			throw new IllegalArgumentException("cannot add a point at x=" + x + ", y=" + y + " with bounds " + this.top.bounds);
		}
		if (this.top.put(x, y, value)) {
			incrementSize();
			return true;
		}
		return false;
	}

	/**
	 * Removes the specified object from the specified location.
	 *
	 * @param x x-coordinate from which the specified value should be removed
	 * @param y y-coordinate from which the specified value should be removed
	 * @param value the value to be removed from the specified coordinates
	 *
	 * @return true if the specified value was found at the specified coordinates
	 *         and was successfully removed (data structure changed), false
	 *         otherwise.
	 */
	public boolean remove(final double x, final double y, final T value) {
		if (this.top.remove(x, y, value)) {
			decrementSize();
			return true;
		}
		return false;
	}

	/** Clear the QuadTree. */
	public void clear() {
		this.top.clear();
		this.size = 0;
		this.modCount++;
	}

	/**
	 * Gets the object closest to x/y
	 *
	 * @param x easting, left-right location, longitude
	 * @param y northing, up-down location, latitude
	 * @return the object found closest to x/y
	 */
	public T getClosest(final double x, final double y) {
		return this.top.get(x, y, new MutableDouble(Double.POSITIVE_INFINITY));
	}

	/**
	 * Gets all objects within a certain distance around x/y
	 *
	 * @param x left-right location, longitude
	 * @param y up-down location, latitude
	 * @param distance the maximal distance returned objects can be away from x/y
	 * @return the objects found within distance to x/y
	 */
	public Collection<T> getDisk(final double x, final double y, final double distance) {
		return this.top.get(x, y, distance * distance, new ArrayList<>());
	}

	/**
	 * Gets all objects within a linear ring (including borders).
	 *
	 * Note by JI (sept '15): This method can be significant faster than calling {@link #getDisk(double, double, double)}
	 * and a manual check on the returned elements for >= r_min. For randomly distributed points one can use the
	 * following rule-of-thumb: if r_min/r_max > 0.4 this method is likely to be faster than retrieving all elements within r_max.
	 *
	 * @param x left-right location, longitude
	 * @param y up-down location, latitude
	 * @param rMin inner ring radius
	 * @param rMax outer rind radius
	 * @return objects within the ring
	 */
	public Collection<T> getRing(final double x, final double y, final double rMin, final double rMax) {
		return this.top.get(x, y, rMin * rMin, rMax * rMax, new ArrayList<>());
	}

	/**
	 * Gets all objects within an elliptical region.
	 *
	 * @param x1 first focus, longitude
	 * @param y1 first focus, latitude
	 * @param x2 second focus, longitude
	 * @param y2 second focus, latitude
	 * @param distance the maximal sum of the distances between an object and the two foci
	 * @return the objects found in the elliptical region
	 * @throws IllegalArgumentException if the distance is shorter than the distance between the foci
	 */
	public Collection<T> getElliptical(
			final double x1,
			final double y1,
			final double x2,
			final double y2,
			final double distance) {
		if ( Math.pow( distance , 2 ) < Math.pow( (x1 - x2), 2 ) + Math.pow( (y1 - y2) , 2 ) ) {
			throw new IllegalArgumentException( "wrong ellipse specification: distance must be greater than distance between foci."
					+" x1="+x1
					+" y1="+y1
					+" x2="+x2
					+" y2="+y2
					+" distance="+distance );
		}
		return this.top.getElliptical(x1, y1, x2, y2, distance, new ArrayList<>());
	}


	/**
	 * Gets all objects inside the specified boundary. Objects on the border of the
	 * boundary are not included.
	 *
	 * @param bounds The bounds of the area of interest.
	 * @param values1 A collection to store the found objects in.
	 * @return The objects found within the area.
	 */
	public Collection<T> getRectangle(final Rect bounds, final Collection<T> values1) {
		return this.top.get(bounds, values1);
	}

	/**
	 * Gets all objects inside the specified area. Objects on the border of
	 * the area are not included.
	 *
	 * @param minX The minimum left-right location, longitude
	 * @param minY The minimum up-down location, latitude
	 * @param maxX The maximum left-right location, longitude
	 * @param maxY The maximum up-down location, latitude
	 * @param values1 A collection to store the found objects in.
	 * @return The objects found within the area.
	 */
	public Collection<T> getRectangle(final double minX, final double minY, final double maxX, final double maxY, final Collection<T> values1) {
		return getRectangle(new Rect(minX, minY, maxX, maxY), values1);
	}

	/**
	 * Executes executor on all objects inside a certain boundary
	 *
	 * @param bounds The boundary in which the executor will be applied.
	 * @param executor is executed on the fitting objects
	 * @return the count of objects found within the bounds.
	 */
	public int execute(final Rect bounds, final Executor<T> executor) {
		if (bounds == null) {
			return this.top.execute(this.top.getBounds(), executor);
		}
		return this.top.execute(bounds, executor);
	}

	/**
	 * Executes executor on all objects inside the rectangle (minX,minY):(maxX,maxY)
	 *
	 * @param minX The minimum left-right location, longitude
	 * @param minY The minimum up-down location, latitude
	 * @param maxX The maximum left-right location, longitude
	 * @param maxY The maximum up-down location, latitude
	 * @param executor is executed on the fitting objects
	 * @return the count of objects found within the rectangle.
	 */
	public int execute(final double minX, final double minY, final double maxX, final double maxY, final Executor<T> executor) {
		return execute(new Rect(minX, minY, maxX, maxY), executor);
	}

	/**
	 * Returns the number of entries in this QuadTree.
	 *
	 * @return the number of entries in this QuadTree.
	 */
	public int size() {
		return this.size;
	}

	/** @return the minimum x coordinate (left-right, longitude, easting) of the bounds of the QuadTree. */
	public double getMinEasting() {
		return this.top.getBounds().minX;
	}

	/** @return the maximum x coordinate (left-right, longitude, easting) of the bounds of the QuadTree. */
	public double getMaxEasting() {
		return this.top.getBounds().maxX;
	}

	/** @return the minimum y coordinate (up-down, latitude, northing) of the bounds of the QuadTree. */
	public double getMinNorthing() {
		return this.top.getBounds().minY;
	}

	/** @return the minimum y coordinate (up-down, latitude, northing) of the bounds of the QuadTree. */
	public double getMaxNorthing() {
		return this.top.getBounds().maxY;
	}

	/**
	 * Returns a collection view of the values contained in this map.  The
	 * collection's iterator will return the values in the order that their
	 * corresponding keys appear in the tree.  The collection is backed by
	 * this <tt>QuadMap</tt> instance, so changes to this map are reflected in
	 * the collection.
	 *
	 * @return a collection view of the values contained in this map.
	 */
	public Collection<T> values() {
		if (this.values == null) {
			this.values = new AbstractCollection<>() {
				@Override
				public Iterator<T> iterator() {
					return new Iterator<>() {
						private final int expectedModCount = QuadTree.this.modCount;
						private Leaf<T> currentLeaf = firstLeaf();
						private int nextIndex = 0;
						private T next = first();

						private T first() {
							if (this.currentLeaf == null) {
								return null;
							}
							this.nextIndex = 0;
							loadNext();
							return this.next;
						}

						@Override
						public boolean hasNext() {
							return this.next != null;
						}

						@Override
						public T next() {
							if (this.next == null) {
								return null;
							}
							if (QuadTree.this.modCount != this.expectedModCount) {
								throw new ConcurrentModificationException();
							}
							T current = this.next;
							loadNext();
							return current;
						}

						private void loadNext() {
							boolean searching = true;
							while (searching) {
								int size = this.currentLeaf.value != null ? 1 : this.currentLeaf.values.size();
								if (this.nextIndex < size) {
									this.nextIndex++;
									this.next = this.currentLeaf.value != null ? this.currentLeaf.value : this.currentLeaf.values.get(this.nextIndex - 1);
									searching = false;
								} else {
									this.currentLeaf = nextLeaf(this.currentLeaf);
									if (this.currentLeaf == null) {
										this.next = null;
										searching = false;
									} else {
										this.nextIndex = 0;
									}
								}
							}
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}

					};
				}

				@Override
				public int size() {
					return QuadTree.this.size;
				}
			};
		}
		return this.values;
	}

	private Leaf<T> firstLeaf() {
		return this.top.firstLeaf();
	}

	private Leaf<T> nextLeaf(final Leaf<T> currentLeaf) {
		return this.top.nextLeaf(currentLeaf);
	}

	/**
	 * An internal class to hold variable parameters when calling methods.
	 * Here a double value is packaged within an object so the value can be
	 * changed in a method and the changed value is available outside of a method.
	 */
	private static class MutableDouble {
		public double value;

		public MutableDouble(final double value) {
			this.value = value;
		}
	}

	/**
	 * An internal class to hold variable parameters when calling methods.
	 * Here a Leaf value is packaged within an object so the value can be
	 * changed in a method and the changed value is available outside of a method.
	 *
	 * @param <T> the type for the Leaf
	 */
	private static class MutableLeaf<T> {
		public Leaf<T> value;

		public MutableLeaf(final Leaf<T> value) {
			this.value = value;
		}
	}

	public static class Rect implements Serializable {
		private static final long serialVersionUID = -837712701959689133L;
		public final double minX;
		public final double minY;
		public final double maxX;
		public final double maxY;
		public final double centerX;
		public final double centerY;

		public Rect(final double minX, final double minY, final double maxX, final double maxY) {
			this.minX = Math.min(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.maxX = Math.max(minX, maxX);
			this.maxY = Math.max(minY, maxY);
			this.centerX = (minX + maxX) / 2;
			this.centerY = (minY + maxY) / 2;
		}

		/**
		 * Calculates the (minimum) distance of a given point to the border of the
		 * rectangle. If the point lies within the rectangle, the distance
		 * is zero.
		 *
		 * @param x left-right location
		 * @param y up-down location
		 * @return distance to border, 0 if inside rectangle or on border
		 */
		public double calcDistance(final double x, final double y) {
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

			return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
		}

		/**
		 * Calculates the square of the (minimum) distance of a given point to the border of the
		 * rectangle. If the point lies within the rectangle, the distance
		 * is zero.
		 *
		 * @param x left-right location
		 * @param y up-down location
		 * @return square of distance to border, 0 if inside rectangle or on border
		 */
		public double calcDistanceSqr(final double x, final double y) {
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
		}

		/**
		 * Calculates the square of the distance from the given point to the furthest corner of the rectangle
		 *
		 * @param x left-right location
		 * @param y up-down location
		 * @return square of distance to the furthest corner of the rectangle
		 */
		public double calcMaxDistanceSqr(final double x, final double y) {
			double distanceX = Math.max(Math.abs(this.minX - x), Math.abs(this.maxX - x));
			double distanceY = Math.max(Math.abs(this.minY - y), Math.abs(this.maxY - y));

			return distanceX * distanceX + distanceY * distanceY;
		}


		/**
		 * Tests if a specified coordinate is inside the boundary of this <code>Rect</code>.
		 * @param x the x-coordinate to test
		 * @param y the y-coordinate to test
		 * @return <code>true</code> if the specified coordinates are
		 * inside the boundary of this <code>Rect</code>;
		 * <code>false</code> otherwise.
		 */
		public boolean contains(final double x, final double y) {
			return (x >= this.minX &&
					y >= this.minY &&
					x < this.maxX &&
					y < this.maxY);
		}

		public boolean containsOrEquals(final double x, final double y) {
			return (x >= this.minX &&
					y >= this.minY &&
					x <= this.maxX &&
					y <= this.maxY);
		}

		/**
		 * Tests if a specified rect is inside or on the boundary of this <code>Rect</code>.
		 * @param rect the rect to test
		 * @return <code>true</code> if the specified rect is
		 * inside or on the boundary of this <code>Rect</code>;
		 * <code>false</code> otherwise.
		 */
		public boolean containsOrEquals(final Rect rect) {
			return (rect.minX >= this.minX &&
					rect.minY >= this.minY &&
					rect.maxX <= this.maxX &&
					rect.maxY <= this.maxY);
		}

		/**
		 * Tests if this <code>Rect</code> intersects another <code>Rect</code>.
		 * Intersection is either interior or border of rect.
		 * @param other The rectangle that should be tested for intersection.
		 * @return <code>true</code> if this <code>Rect</code>
		 * intersects the interior of the other <code>Rect</code>; <code>false</code> otherwise.
		 */
		public boolean intersects(final Rect other) {
			if ((this.maxX-this.minX) < 0 || (this.maxY-this.minY) < 0) {
				return false;
			}
			return (other.maxX >= this.minX &&
					other.maxY >= this.minY &&
					other.minX <= this.maxX &&
					other.minY <= this.maxY);
		}

		/**
		 * Computes the intersection of this <code>Rect</code> with the
		 * specified <code>Rect</code>. Returns a new <code>Rect</code>
		 * that represents the intersection of the two rectangles.
		 * If the two rectangles do not intersect, the result will be
		 * null.
		 *
		 * @param     r   the specified <code>Rectangle</code>
		 * @return    the largest <code>Rectangle</code> contained in both the
		 *            specified <code>Rectangle</code> and in
		 *		  this <code>Rectangle</code>; or if the rectangles
		 *            do not intersect, an empty rectangle.
		 */
		public Rect intersection(final Rect r) {
			double tx1 = this.minX;
			double ty1 = this.minY;
			double tx2 = this.maxX;
			double ty2 = this.maxY;
			if (this.minX < r.minX) tx1 = r.minX;
			if (this.minY < r.minY) ty1 = r.minY;
			if (tx2 > r.maxX) tx2 = r.maxX;
			if (ty2 > r.maxY) ty2 = r.maxY;
			// did they intersect at all?
			if(tx2-tx1 <=0.f || ty2-ty1 <= 0.f) return null;

			return new Rect(tx1, ty1, tx2, ty2);
		}

		/**
		 * Adds a <code>Rect</code> to this <code>Rect</code>.
		 * The resulting <code>Rect</code> is the union of the two
		 * rectangles (i.e. the minimum rectangle that contains the two original rectangles)
		 * @param  r the specified <code>Rect</code>
		 */
		public Rect union(final Rect r) {
			return new Rect( Math.min(this.minX, r.minX),
					Math.min(this.minY, r.minY),
					Math.max(this.maxX, r.maxX),
					Math.max(this.maxY, r.maxY));
		}

		/**
		 * Increases the size of the rectangle by scaleX and scaleY.
		 *
		 * (and "increase by" means:
		 *   1.0: increase it by 100% (scale it by 2.0)
		 *   -0.5: decrease it by 50% (scale it by 0.5)
		 *   0.0: do nothing)   michaz
		 *
		 */

		public Rect scale(double scaleX, double scaleY) {
			scaleY *= this.centerY - this.minY;
			scaleX *= this.centerX - this.minX;
			return new Rect(this.minX - scaleX, this.minY-scaleY, this.maxX + scaleX, this.maxY + scaleY);
		}

		@Override
		public String toString() {
			return "topLeft: ("+minX+","+minY+") bottomRight: ("+maxX+","+maxY+")";
		}

	}

	protected static class Leaf<T> implements Serializable {
		private static final long serialVersionUID = -6527830222532634476L;
		final public double x;
		final public double y;
		/* a leaf can contain one more multiple objects at the same coordinate.
		 * in many cases it will be only one, so to save memory, we'll store it in "value".
		 * only if actually multiple objects at the same coordinate need to be stored,
		 * we create the values-list and add all objects there.
		 * So either value or values is non-null. it should never be the case that both are null
		 * or both are non-null. mrieser/oct2018
		 */
		public T value;
		public ArrayList<T> values;

		public Leaf(final double x, final double y, final T value) {
			this.x = x;
			this.y = y;
			this.value = value;
			this.values = null;
		}
	}

	protected static class Node<T> implements Serializable {
		private static final long serialVersionUID = 8151154226742383421L;

		private static final int MAX_CHILDS = 128;

		private ArrayList<Leaf<T>> leaves = null;

		private boolean hasChilds = false;
		private Node<T> northwest = null;
		private Node<T> northeast = null;
		private Node<T> southeast = null;
		private Node<T> southwest = null;
		private final Rect bounds;

		public Node(final double minX, final double minY, final double maxX, final double maxY) {
			this.bounds = new Rect(minX, minY, maxX, maxY);
		}

		public boolean put(final Leaf<T> leaf) {
			if (this.hasChilds) return getChild(leaf.x, leaf.y).put(leaf);
			if (this.leaves == null) {
				this.leaves = new ArrayList<>();
				this.leaves.add(leaf);
				return true;
			}
			for (Leaf<T> l : this.leaves) {
				if (l.x == leaf.x && l.y == leaf.y) {
					if (leaf.value == l.value) {
						return false;
					}
					if (l.values != null) {
						if (l.values.contains(leaf.value)) {
							return false;
						}
						l.values.add(leaf.value);
						return true;
					}
					if (l.value == null) {
						l.value = leaf.value;
						return true;
					} else {
						l.values = new ArrayList<>(3);
						l.values.add(l.value);
						l.value = null;
						l.values.add(leaf.value);
						return true;
					}
				}

			}
			if (this.leaves.size() < MAX_CHILDS) {
				this.leaves.add(leaf);
				return true;
			}
			this.split();
			return getChild(leaf.x, leaf.y).put(leaf);
		}

		public boolean put(final double x, final double y, final T value) {
			return put(new Leaf<>(x, y, value));
		}

		public boolean remove(final double x, final double y, final T value) {
			if (this.hasChilds) return getChild(x, y).remove(x, y, value);
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					if (leaf.x == x && leaf.y == y) {
						if (leaf.value == value) {
							leaf.value = null;
							this.leaves.remove(leaf);
							return true;
						}
						if (leaf.values != null) {
							if (leaf.values.remove(value)) {
								if (leaf.values.isEmpty()) {
									this.leaves.remove(leaf);
								}
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		public void clear() {
			// we could as well just set everything to null and let the
			// garbage collection do its job.
			if (this.hasChilds) {
				this.northwest.clear();
				this.northeast.clear();
				this.southeast.clear();
				this.southwest.clear();
				this.northwest = null;
				this.northeast = null;
				this.southeast = null;
				this.southwest = null;
				this.hasChilds = false;
			} else {
				if (this.leaves != null) {
					this.leaves = null;
				}
			}
		}

		/* default */ T get(final double x, final double y, final MutableDouble bestDistanceSqr) {
			if (this.hasChilds) {
				T closest = null;
				Node<T> bestChild = this.getChild(x, y);
				if (bestChild != null) {
					closest = bestChild.get(x, y, bestDistanceSqr);
				}
				if (bestChild != this.northwest && this.northwest.bounds.calcDistanceSqr(x, y) < bestDistanceSqr.value) {
					T value = this.northwest.get(x, y, bestDistanceSqr);
					if (value != null) { closest = value; }
				}
				if (bestChild != this.northeast && this.northeast.bounds.calcDistanceSqr(x, y) < bestDistanceSqr.value) {
					T value = this.northeast.get(x, y, bestDistanceSqr);
					if (value != null) { closest = value; }
				}
				if (bestChild != this.southeast && this.southeast.bounds.calcDistanceSqr(x, y) < bestDistanceSqr.value) {
					T value = this.southeast.get(x, y, bestDistanceSqr);
					if (value != null) { closest = value; }
				}
				if (bestChild != this.southwest && this.southwest.bounds.calcDistanceSqr(x, y) < bestDistanceSqr.value) {
					T value = this.southwest.get(x, y, bestDistanceSqr);
					if (value != null) { closest = value; }
				}
				return closest;
			}
			// no more childs, so we must contain the closest object
			T closest = null;
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					double deltaX = leaf.x - x;
					double deltaY = leaf.y - y;
					double distanceSqr = deltaX * deltaX + deltaY * deltaY;
					if (distanceSqr < bestDistanceSqr.value) {
						bestDistanceSqr.value = distanceSqr;
						closest = leaf.value != null ? leaf.value : leaf.values.get(0);
					}
				}
			}
			return closest;
		}

		/* default */ Collection<T> getElliptical(
				final double x1,
				final double y1,
				final double x2,
				final double y2,
				final double maxDistance,
				final Collection<T> values) {
			assert Math.pow( maxDistance , 2 ) >= Math.pow( (x1 - x2), 2 ) + Math.pow( (y1 - y2) , 2 );
			if (this.hasChilds) {
				// note: this could probably be improved. The idea here
				// is NOT to dive in quadrants which we know do not contain points
				// in the ellipse.
				// This is done, but we will also dive in some quadrants not intersecting
				// the ellipse, which is just a loss of time (the sum of the minimum distances
				// to an area is always lower than the munimum of the sum of the distances,
				// and the difference can be substantial. Just not sure how efficiently
				// one can estimate the minimum of the sum of the distances).

				// this is a trick to avoid computing distance of quadrant to second focus
				// if the quandrant is already too far from first focus.
				// Some tests showed a huge improvement with this, when a large part
				// of the space has to be pruned (which is typically the case in
				// real applications). By "huge improvement", I mean several times
				// faster (how much depends on the particular instance).
				final double nw1 = this.northwest.bounds.calcDistance(x1, y1);
				if ( nw1 <= maxDistance && nw1 + this.northwest.bounds.calcDistance(x2, y2) <= maxDistance) {
					this.northwest.getElliptical(x1, y1, x2, y2, maxDistance, values);
				}
				final double ne1 = this.northeast.bounds.calcDistance(x1, y1);
				if (ne1 <= maxDistance && ne1 + this.northeast.bounds.calcDistance(x2, y2) <= maxDistance) {
					this.northeast.getElliptical(x1, y1, x2, y2, maxDistance, values);
				}
				final double se1 = this.southeast.bounds.calcDistance(x1, y1);
				if (se1 <= maxDistance && se1 + this.southeast.bounds.calcDistance(x2, y2) <= maxDistance) {
					this.southeast.getElliptical(x1, y1, x2, y2, maxDistance, values);
				}
				final double sw1 = this.southwest.bounds.calcDistance(x1, y1);
				if (sw1 <= maxDistance && sw1 + this.southwest.bounds.calcDistance(x2, y2) <= maxDistance) {
					this.southwest.getElliptical(x1, y1, x2, y2, maxDistance, values);
				}
				return values;
			}
			// no more childs, so we must contain the closest object
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					final double distance1 = Math.sqrt(
							(leaf.x - x1) * (leaf.x - x1)
									+ (leaf.y - y1) * (leaf.y - y1));
					// same trick as above, though it should not be useul in the vast
					// majority of cases
					if (distance1 <= maxDistance) {
						final double distance2 = Math.sqrt(
								(leaf.x - x2) * (leaf.x - x2)
										+ (leaf.y - y2) * (leaf.y - y2));
						if (distance1 + distance2 <= maxDistance) {
							if (leaf.value != null) {
								values.add(leaf.value);
							} else {
								values.addAll(leaf.values);
							}
						}
					}
				}
			}
			return values;
		}

		/* default */ Collection<T> get(final double x, final double y, final double maxDistanceSqr, final Collection<T> values) {
			if (this.hasChilds) {
				if (this.northwest.bounds.calcDistanceSqr(x, y) <= maxDistanceSqr) {
					this.northwest.get(x, y, maxDistanceSqr, values);
				}
				if (this.northeast.bounds.calcDistanceSqr(x, y) <= maxDistanceSqr) {
					this.northeast.get(x, y, maxDistanceSqr, values);
				}
				if (this.southeast.bounds.calcDistanceSqr(x, y) <= maxDistanceSqr) {
					this.southeast.get(x, y, maxDistanceSqr, values);
				}
				if (this.southwest.bounds.calcDistanceSqr(x, y) <= maxDistanceSqr) {
					this.southwest.get(x, y, maxDistanceSqr, values);
				}
				return values;
			}
			// no more childs, so we must contain the closest object
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					double distanceSqr = (leaf.x - x) * (leaf.x - x) + (leaf.y - y) * (leaf.y - y);
					if (distanceSqr <= maxDistanceSqr) {
						if (leaf.value != null) {
							values.add(leaf.value);
						} else {
							values.addAll(leaf.values);
						}
					}
				}
			}
			return values;
		}

		/* default */ Collection<T> get(final double x, final double y, final double rMinSqr, final double rMaxSqr,
																		Collection<T> values) {
			if (this.hasChilds) {
				stepIntoSqr(this.northwest, x, y, rMinSqr, rMaxSqr, values);
				stepIntoSqr(this.northeast, x, y, rMinSqr, rMaxSqr, values);
				stepIntoSqr(this.southeast, x, y, rMinSqr, rMaxSqr, values);
				stepIntoSqr(this.southwest, x, y, rMinSqr, rMaxSqr, values);
				return values;
			}
			// no more childs, so we must contain the closest object
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					double distanceSqr = (leaf.x - x) * (leaf.x - x) + (leaf.y - y) * (leaf.y - y);
					if (distanceSqr <= rMaxSqr && distanceSqr >= rMinSqr) {
						if (leaf.value != null) {
							values.add(leaf.value);
						} else {
							values.addAll(leaf.values);
						}
					}
				}
			}
			return values;
		}

		private void stepIntoSqr(Node<T> node, double x, double y, double rMinSqr, double rMaxSqr, Collection<T> values) {
			double minDistanceSqr = node.bounds.calcDistanceSqr(x, y);
			double maxDistanceSqr = node.bounds.calcMaxDistanceSqr(x, y);

			if (minDistanceSqr <= rMaxSqr && maxDistanceSqr >= rMinSqr) {
				node.get(x, y, rMinSqr, rMaxSqr, values);
			}
		}

		/* default */ Collection<T> get(final Rect bounds, final Collection<T> values) {
			if (this.hasChilds) {
				if (this.northwest.bounds.intersects(bounds)) {
					this.northwest.get(bounds, values);
				}
				if (this.northeast.bounds.intersects(bounds)) {
					this.northeast.get(bounds, values);
				}
				if (this.southeast.bounds.intersects(bounds)) {
					this.southeast.get(bounds, values);
				}
				if (this.southwest.bounds.intersects(bounds)) {
					this.southwest.get(bounds, values);
				}
				return values;
			}
			// no more childs, so we must contain the closest object
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					if (bounds.containsOrEquals(leaf.x, leaf.y)) {
						if (leaf.value != null) {
							values.add(leaf.value);
						} else {
							values.addAll(leaf.values);
						}
					}
				}
			}
			return values;
		}

		/* default */ int execute(final Rect globalBounds, final Executor<T> executor) {
			int count = 0;
			if (this.hasChilds) {
				if (this.northwest.bounds.intersects(globalBounds)) {
					count += this.northwest.execute(globalBounds, executor);
				}
				if (this.northeast.bounds.intersects(globalBounds)) {
					count += this.northeast.execute(globalBounds, executor);
				}
				if (this.southeast.bounds.intersects(globalBounds)) {
					count += this.southeast.execute(globalBounds, executor);
				}
				if (this.southwest.bounds.intersects(globalBounds)) {
					count += this.southwest.execute(globalBounds, executor);
				}
				return count;
			}
			// no more childs, so we must contain the closest object
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					if (globalBounds.contains(leaf.x, leaf.y)) {
						if (leaf.value != null) {
							count++;
							executor.execute(leaf.x, leaf.y, leaf.value);
						} else {
							count += leaf.values.size();
							for (T object : leaf.values) executor.execute(leaf.x, leaf.y, object);
						}
					}
				}
			}
			return count;
		}

		private void split() {
			this.northwest = new Node<>(this.bounds.minX, this.bounds.centerY, this.bounds.centerX, this.bounds.maxY);
			this.northeast = new Node<>(this.bounds.centerX, this.bounds.centerY, this.bounds.maxX, this.bounds.maxY);
			this.southeast = new Node<>(this.bounds.centerX, this.bounds.minY, this.bounds.maxX, this.bounds.centerY);
			this.southwest = new Node<>(this.bounds.minX, this.bounds.minY, this.bounds.centerX, this.bounds.centerY);
			this.hasChilds = true;
			if (this.leaves != null) {
				for (Leaf<T> leaf : this.leaves) {
					getChild(leaf.x, leaf.y).put(leaf);
				}
				this.leaves= null;
			}
		}

		private Node<T> getChild(final double x, final double y) {
			if (this.hasChilds) {
				if (x < this.bounds.centerX) {
					if (y < this.bounds.centerY)
						return this.southwest;
					return this.northwest;
				}
				if (y < this.bounds.centerY)
					return this.southeast;
				return this.northeast;
			}
			return null;
		}

		/* default */ Leaf<T> firstLeaf() {
			if (this.hasChilds) {
				Leaf<T> leaf = this.southwest.firstLeaf();
				if (leaf == null) { leaf = this.northwest.firstLeaf(); }
				if (leaf == null) { leaf = this.southeast.firstLeaf(); }
				if (leaf == null) { leaf = this.northeast.firstLeaf(); }
				return leaf;
			}
			return (this.leaves == null || this.leaves.isEmpty()) ? null : this.leaves.get(0);
		}

		/* default */ boolean nextLeaf(final Leaf<T> currentLeaf, final MutableLeaf<T> nextLeaf) {
			if (this.hasChilds) {
				if (currentLeaf.x <= this.bounds.centerX && currentLeaf.y <= this.bounds.centerY) {
					boolean found = this.southwest.nextLeaf(currentLeaf, nextLeaf);
					if (found) {
						if (nextLeaf.value == null) { nextLeaf.value = this.northwest.firstLeaf(); }
						if (nextLeaf.value == null) { nextLeaf.value = this.southeast.firstLeaf(); }
						if (nextLeaf.value == null) { nextLeaf.value = this.northeast.firstLeaf(); }
						return true;
					}
				}
				if (currentLeaf.x <= this.bounds.centerX && currentLeaf.y >= this.bounds.centerY) {
					boolean found = this.northwest.nextLeaf(currentLeaf, nextLeaf);
					if (found) {
						if (nextLeaf.value == null) { nextLeaf.value = this.southeast.firstLeaf(); }
						if (nextLeaf.value == null) { nextLeaf.value = this.northeast.firstLeaf(); }
						return true;
					}
				}
				if (currentLeaf.x >= this.bounds.centerX && currentLeaf.y <= this.bounds.centerY) {
					boolean found = this.southeast.nextLeaf(currentLeaf, nextLeaf);
					if (found) {
						if (nextLeaf.value == null) { nextLeaf.value = this.northeast.firstLeaf(); }
						return true;
					}
				}
				if (currentLeaf.x >= this.bounds.centerX && currentLeaf.y >= this.bounds.centerY) {
					return this.northeast.nextLeaf(currentLeaf, nextLeaf);
				}
				return false;
			}
			if (this.leaves != null) {
				int idx = this.leaves.indexOf(currentLeaf);
				if (idx >= 0) {
					if (idx + 1 < this.leaves.size()) {
						nextLeaf.value = this.leaves.get(idx + 1);
					}
					return true;
				}
			}
			return false;
		}

		public Leaf<T> nextLeaf(final Leaf<T> currentLeaf) {
			MutableLeaf<T> nextLeaf = new MutableLeaf<>(null);
			nextLeaf(currentLeaf, nextLeaf);
			return nextLeaf.value;
		}

		public Rect getBounds() {
			return this.bounds;
		}

	}

	public interface Executor<T> {
		void execute(double x, double y, T object);
	}
}
