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

package org.matsim.utils.misc;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An implementation of a QuadTree to store geometric point data. The expected
 * bounds of all added points must be given to the constructor for working
 * properly. While the data structure will still work if points outside the
 * given bounds are added, the performance is likely to drop to that of a linked
 * list.<br />
 * At one location, several different objects can be put. An object can be put
 * to the QuadTree at different locations. But an object cannot be put more than
 * once at the same location.
 *
 * @author mrieser
 * @param <T> The type of data to be stored in the QuadTree.
 */
public class QuadTree<T> {

	/** The top node or root of the tree */
	private Node top = null;

  /** The number of entries in the tree */
  private transient int size = 0;

  /** The number of structural modifications to the tree. */
  private transient int modCount = 0;

  private void incrementSize()   { this.modCount++; this.size++; }
  private void decrementSize()   { this.modCount++; this.size--; }

	/**
	 * Creates an empty QuadTree with the bounds minX/minY -- maxX/maxY. For
	 * optimal performance, all points should be evenly distributed within this
	 * rectangle.
	 *
	 * @param minX The smallest x coordinate expected
	 * @param minY The smallest y coordinate expected
	 * @param maxX The largest x coordinate expected
	 * @param maxY The largest y coordinate expected
	 */
	public QuadTree(final double minX, final double minY, final double maxX, final double maxY) {
		this.top = new Node(minX, minY, maxX, maxY);
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
		return this.top.remove(x, y, value);
	}

	/** Clear the QuadTree. */
	public void clear() {
		this.top.clear();
	}

	/**
	 * Gets the object closest to x/y
	 *
	 * @param x left-right location, longitude
	 * @param y up-down location, latitude
	 * @return the object found closest to x/y
	 */
	public T get(final double x, final double y) {
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
	public Collection<T> get(final double x, final double y, final double distance) {
		return this.top.get(x, y, distance, new ArrayList<T>());
	}

  /**
   * Returns the number of entries in this QuadTree.
   *
   * @return the number of entries in this QuadTree.
   */
  public int size() {
      return this.size;
  }


  /* Support for values() and an iterator over the values. */
  /* This is similar to TreeMap.java and AbstractMap.java */
	transient volatile Collection<T> values = null;

  /**
   * Returns a collection view of the values contained in this map.  The
   * collection's iterator will return the values in the order that their
   * corresponding keys appear in the tree.  The collection is backed by
   * this <tt>TreeMap</tt> instance, so changes to this map are reflected in
   * the collection, and vice-versa.  The collection supports element
   * removal, which removes the corresponding mapping from the map through
   * the <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
   * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a collection view of the values contained in this map.
   */
	public Collection<T> values() {
		if (this.values == null) {
			this.values = new AbstractCollection<T>() {
				@Override
				public Iterator<T> iterator() {
					Iterator<T> iterator = new Iterator<T>() {

						private Leaf currentLeaf = firstLeaf();
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

						public boolean hasNext() {
							return this.next != null;
						}

						public T next() {
							if (this.next == null) {
								return null;
							}
							T current = this.next;
							loadNext();
							return current;
						}

						public void loadNext() {
							boolean searching = true;
							while (searching) {
								if (this.nextIndex < this.currentLeaf.values.size()) {
									this.nextIndex++;
									this.next = this.currentLeaf.values.get(this.nextIndex-1);
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

						public void remove() {
							throw new UnsupportedOperationException();
						}

					};
					return iterator;
				}

				@Override
				public int size() {
					return QuadTree.this.size;
				}
			};
		}
		return this.values;
	}

  private Leaf firstLeaf() {
  	return this.top.firstLeaf();
  }

  private Leaf nextLeaf(final Leaf currentLeaf) {
  	return this.top.nextLeaf(currentLeaf);
  }

	/**
	 * An internal class to hold variable parameters when calling methods.
	 * Here a double value is packaged within an object so the value can be
	 * changed in a method and the changed value is available outside of a method.
	 */
	private class MutableDouble {
		public double value;

		public MutableDouble(final double value) {
			this.value = value;
		}
	}

	/**
	 * An internal class to hold variable parameters when calling methods.
	 * Here a Leaf value is packaged within an object so the value can be
	 * changed in a method and the changed value is available outside of a method.
	 */
	private class MutableLeaf {
		public Leaf value;

		public MutableLeaf(final Leaf value) {
			this.value = value;
		}
	}


	private class Rect {
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
		 * Calculates the distance of a given point to the border of the
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
	}

	private class Leaf {
		final public double x;
		final public double y;
		final public ArrayList<T> values;

		public Leaf(final double x, final double y, final T value) {
			this.x = x;
			this.y = y;
			this.values = new ArrayList<T>();
			this.values.add(value);
		}
	}

	private class Node {

		private Leaf leaf = null;

		private boolean hasChilds = false;
		private Node northwest = null;
		private Node northeast = null;
		private Node southeast = null;
		private Node southwest = null;
		private Rect bounds = null;

		public Node(final double minX, final double minY, final double maxX, final double maxY) {
			this.bounds = new Rect(minX, minY, maxX, maxY);
		}

		public boolean put(final Leaf leaf) {
			if (this.hasChilds) return getChild(leaf.x, leaf.y).put(leaf);
			if (this.leaf == null) {
				this.leaf = leaf;
				return true;
			}
			if (this.leaf.x == leaf.x && this.leaf.y == leaf.y) {
				boolean changed = false;
				for (T value : leaf.values) {
					if (!this.leaf.values.contains(value)) {
						changed = changed || this.leaf.values.add(value);
					}
				}
				return changed;
			}
			this.split();
			return getChild(leaf.x, leaf.y).put(leaf);
		}

		public boolean put(final double x, final double y, final T value) {
			return put(new Leaf(x, y, value));
		}

		public boolean remove(final double x, final double y, final T value) {
			if (this.hasChilds) return getChild(x, y).remove(x, y, value);
			if (this.leaf != null && this.leaf.x == x && this.leaf.y == y) {
				if (this.leaf.values.remove(value)) {
					if (this.leaf.values.size() == 0) {
						this.leaf = null;
					}
					decrementSize();
					return true;
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
			} else {
				if (this.leaf != null) {
					this.leaf.values.clear();
					this.leaf = null;
				}
			}
		}

		private T get(final double x, final double y, final MutableDouble bestDistance) {
			if (this.hasChilds) {
				T closest = null;
				if (this.northwest.bounds.calcDistance(x, y) < bestDistance.value) {
					T value = this.northwest.get(x, y, bestDistance);
					if (value != null) { closest = value; }
				}
				if (this.northeast.bounds.calcDistance(x, y) < bestDistance.value) {
					T value = this.northeast.get(x, y, bestDistance);
					if (value != null) { closest = value; }
				}
				if (this.southeast.bounds.calcDistance(x, y) < bestDistance.value) {
					T value = this.southeast.get(x, y, bestDistance);
					if (value != null) { closest = value; }
				}
				if (this.southwest.bounds.calcDistance(x, y) < bestDistance.value) {
					T value = this.southwest.get(x, y, bestDistance);
					if (value != null) { closest = value; }
				}
				return closest;
			}
			// no more childs, so we must contain the closest object
			if (this.leaf != null && this.leaf.values.size() > 0) {
				T value = this.leaf.values.get(0);
				double distance = Math.sqrt(
						(this.leaf.x - x) * (this.leaf.x - x)
						+ (this.leaf.y - y) * (this.leaf.y - y));
				if (distance < bestDistance.value) {
					bestDistance.value = distance;
					return value;
				}
			}
			return null;
		}

		private Collection<T> get(final double x, final double y, final double maxDistance, final Collection<T> values) {
			if (this.hasChilds) {
				if (this.northwest.bounds.calcDistance(x, y) < maxDistance) {
					this.northwest.get(x, y, maxDistance, values);
				}
				if (this.northeast.bounds.calcDistance(x, y) < maxDistance) {
					this.northeast.get(x, y, maxDistance, values);
				}
				if (this.southeast.bounds.calcDistance(x, y) < maxDistance) {
					this.southeast.get(x, y, maxDistance, values);
				}
				if (this.southwest.bounds.calcDistance(x, y) < maxDistance) {
					this.southwest.get(x, y, maxDistance, values);
				}
				return values;
			}
			// no more childs, so we must contain the closest object
			if (this.leaf != null && this.leaf.values.size() > 0) {
				double distance = Math.sqrt(
						(this.leaf.x - x) * (this.leaf.x - x)
						+ (this.leaf.y - y) * (this.leaf.y - y));
				if (distance < maxDistance) {
					values.addAll(this.leaf.values);
				}
			}
			return values;
		}

		private void split() {
			this.northwest = new Node(this.bounds.minX, this.bounds.centerY, this.bounds.centerX, this.bounds.maxY);
			this.northeast = new Node(this.bounds.centerX, this.bounds.centerY, this.bounds.maxX, this.bounds.maxY);
			this.southeast = new Node(this.bounds.centerX, this.bounds.minY, this.bounds.maxX, this.bounds.centerY);
			this.southwest = new Node(this.bounds.minX, this.bounds.minY, this.bounds.centerX, this.bounds.centerY);
			this.hasChilds = true;
			if (this.leaf != null) {
				getChild(this.leaf.x, this.leaf.y).put(this.leaf);
				this.leaf = null;
			}
		}

		private Node getChild(final double x, final double y) {
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

		public Leaf firstLeaf() {
			if (this.hasChilds) {
				Leaf leaf = this.southwest.firstLeaf();
				if (leaf == null) { leaf = this.northwest.firstLeaf(); }
				if (leaf == null) { leaf = this.southeast.firstLeaf(); }
				if (leaf == null) { leaf = this.northeast.firstLeaf(); }
				return leaf;
			}
			return this.leaf;
		}

		public boolean nextLeaf(final Leaf currentLeaf, final MutableLeaf nextLeaf) {
			if (this.hasChilds) {
				boolean found = this.southwest.nextLeaf(currentLeaf, nextLeaf);
				if (found) {
					if (nextLeaf.value == null) { nextLeaf.value = this.northwest.firstLeaf(); }
					if (nextLeaf.value == null) { nextLeaf.value = this.southeast.firstLeaf(); }
					if (nextLeaf.value == null) { nextLeaf.value = this.northeast.firstLeaf(); }
					return true;
				}
				found = this.northwest.nextLeaf(currentLeaf, nextLeaf);
				if (found) {
					if (nextLeaf.value == null) { nextLeaf.value = this.southeast.firstLeaf(); }
					if (nextLeaf.value == null) { nextLeaf.value = this.northeast.firstLeaf(); }
					return true;
				}
				found = this.southeast.nextLeaf(currentLeaf, nextLeaf);
				if (found) {
					if (nextLeaf.value == null) { nextLeaf.value = this.northeast.firstLeaf(); }
					return true;
				}
				return this.northeast.nextLeaf(currentLeaf, nextLeaf);
			}
			return currentLeaf == this.leaf;
		}

		public Leaf nextLeaf(final Leaf currentLeaf) {
			MutableLeaf nextLeaf = new MutableLeaf(null);
			nextLeaf(currentLeaf, nextLeaf);
			return nextLeaf.value;
		}

	}

}
