/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.digicore.grid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author jwjoubert
 *
 * @param <T> The type of data to be stored in the OcTree.
 */
public class OcTree<T> implements Serializable {
	
	public static void main(String[] args){
		OcTree<Object> ot = new OcTree<Object>(0.0,0.0,0.0,1.0,1.0,1.0);
		ot.put(0.1, 0.1, 0.1, new Double(1.0));
		ot.put(0.2, 0.2, 0.2, new Double(2.0));
		ot.put(0.8, 0.8, 0.8, new Double(3.0));
		
		Object o1 = ot.get(0.12, 0.12, 0.12);
		Object o2 = ot.get(0.22, 0.22, 0.22);
		Object o3 = ot.get(0.6, 0.6, 0.6);
	}

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
	 * 
	 */
	public OcTree(final double minX, final double minY, final double minZ,
			final double maxX, final double maxY, final double maxZ) {
		this.top = new Node<T>(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	
	public boolean put(double x, double y, double z, final T value){
		if(this.top.put(x, y, z, value)){
			incrementSize();
			return true;
		}
		return false;
	}
	
	
	public T get(final double x, final double y, final double z){
		return this.top.get(x, y, z, new MutableDouble(Double.POSITIVE_INFINITY));
	}
	
	public Collection<T> get(final double x, final double y, final double z, final double distance){
		return null;
	}

	
	
	public static class Cube implements Serializable{
		private static final long serialVersionUID = -6288722195476018755L;
		public final double minX;
		public final double minY;
		public final double minZ;
		public final double maxX;
		public final double maxY;
		public final double maxZ;
		public final double centerX;
		public final double centerY;
		public final double centerZ;
		
		public Cube(double minX, double minY, double minZ, double maxX,
				double maxY, double maxZ) {
			this.minX = Math.min(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.minZ = Math.min(minZ, maxZ);
			this.maxX = Math.max(minX, maxX);
			this.maxY = Math.max(minY, maxY);
			this.maxZ = Math.max(minZ, maxZ);
			this.centerX = (this.minX + this.maxX) / 2;
			this.centerY = (this.minY + this.maxY) / 2;
			this.centerZ = (this.minZ + this.maxZ) / 2;
		}
		
		public double calcDistance(final double x, final double y, final double z){
			double distanceX;
			double distanceY;
			double distanceZ;
			
			if(this.minX <= x && x <= this.maxX){
				distanceX = 0.0;
			} else {
				distanceX = Math.min(Math.abs(this.minX - x), Math.abs(this.maxX - x));
			}
			if(this.minY <= y && y <= this.maxY){
				distanceY = 0.0;
			} else {
				distanceY= Math.min(Math.abs(this.minY - y), Math.abs(this.maxY - y));
			}
			if(this.minZ <= z && z <= this.maxZ){
				distanceZ = 0.0;
			} else {
				distanceZ = Math.min(Math.abs(this.minZ - z), Math.abs(this.maxZ - z));
			}
			
			return Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2) + Math.pow(distanceZ, 2));
		}
		
		public boolean contains(final double x, final double y, final double z){
			return (x >= this.minX &&
					y >= this.minY &&
					z >= this.minZ &&
					x < this.maxX &&
					y < this.maxY &&
					z < this.maxZ);
		}
		
		public boolean containsOrEquals(final double x, final double y, final double z){
			return (x >= this.minX &&
					y >= this.minY &&
					z >= this.minZ &&
					x <= this.maxX &&
					y <= this.maxY &&
					z <= this.maxZ);
		}
		
		public String toString(){
			return "lowerSouthWest: ("+minX+";"+minY+";"+minZ+") upperNorthEast: ("+maxX+";"+maxY+";"+maxZ+")";
		}

		
		
	}
	
	
	public static class Node<T> implements Serializable{
		private static final long serialVersionUID = -3310430997932897472L;

		private Leaf<T> leaf = null;
		private boolean hasChildren = false;
		private Node<T> lowerSouthWest = null;
		private Node<T> lowerSouthEast = null;
		private Node<T> lowerNorthEast = null;
		private Node<T> lowerNorthWest = null;
		private Node<T> upperSouthWest = null;
		private Node<T> upperSouthEast = null;
		private Node<T> upperNorthEast = null;
		private Node<T> upperNorthWest = null;
		private Cube bounds;

		public Node(double minX, double minY, double minZ, double maxX,
				double maxY, double maxZ) {
			this.bounds = new Cube(minX, minY, minZ, maxX, maxY, maxZ);
		}
		
		public boolean put(final double x, final double y, final double z, final T value){
			return put(new Leaf<T>(x, y, z, value));
		}

		private boolean put(final Leaf<T> leaf) {
			if( !this.bounds.containsOrEquals(leaf.x, leaf.y, leaf.z) ){
				throw new IllegalArgumentException("Cannot add a point at " +
						"x=" + leaf.x + ", " +
						"y=" + leaf.y + ", " +
						"z=" + leaf.z + " with bounds " + this.bounds.toString());
			}
			if(this.hasChildren){
				return this.getChild(leaf.x, leaf.y, leaf.z).put(leaf);
			}
			if( this.leaf == null ){
				this.leaf = leaf;
				return true;
			}
			if( this.leaf.x == leaf.x && this.leaf.y == leaf.y && this.leaf.z == leaf.z){
				boolean changed = false;
				for(T value : leaf.values){
					if(!this.leaf.values.contains(value)){
						changed = this.leaf.values.add(value) || changed;
					}
				}
				return changed;
			}
			this.split();
			return getChild(leaf.x, leaf.y, leaf.z).put(leaf);
		}
		
		private void split() {
			this.lowerSouthWest = new Node<T>(
					this.bounds.minX, this.bounds.minY, this.bounds.minZ,
					this.bounds.centerX, this.bounds.centerY, this.bounds.centerZ);
			this.lowerSouthEast = new Node<T>(
					this.bounds.centerX, this.bounds.minY, this.bounds.minZ,
					this.bounds.maxX, this.bounds.centerY, this.bounds.centerZ);
			this.lowerNorthWest = new Node<T>(
					this.bounds.minX, this.bounds.centerY, this.bounds.minZ,
					this.bounds.centerX, this.bounds.maxY, this.bounds.centerZ);
			this.lowerNorthEast = new Node<T>(
					this.bounds.centerX, this.bounds.centerY, this.bounds.minZ,
					this.bounds.maxX, this.bounds.maxY, this.bounds.centerZ);
			this.upperSouthWest = new Node<T>(
					this.bounds.minX, this.bounds.minY, this.bounds.centerZ,
					this.bounds.centerX, this.bounds.centerY, this.bounds.maxZ);
			this.upperSouthEast = new Node<T>(
					this.bounds.centerX, this.bounds.minY, this.bounds.centerZ,
					this.bounds.maxX, this.bounds.centerY, this.bounds.maxZ);
			this.upperNorthWest = new Node<T>(
					this.bounds.minX, this.bounds.centerY, this.bounds.centerZ,
					this.bounds.centerX, this.bounds.maxY, this.bounds.maxZ);
			this.upperNorthEast = new Node<T>(
					this.bounds.centerX, this.bounds.centerY, this.bounds.centerZ,
					this.bounds.maxX, this.bounds.maxY, this.bounds.maxZ);
			this.hasChildren = true;
			if( this.leaf != null ){
				getChild(this.leaf.x, this.leaf.y, this.leaf.z).put(this.leaf);
				this.leaf = null;
			}
		}

		private Node<T> getChild(final double x, final double y, final double z){
			if(this.hasChildren){
				if(x < this.bounds.centerX){
					if(y < this.bounds.centerY){
						if(z < this.bounds.centerZ){
							return this.lowerSouthWest;
						} else{
							return this.upperSouthWest;
						}
					} else{
						if(z < this.bounds.centerZ){
							return this.lowerNorthWest;
						} else{
							return this.upperNorthWest;
						}
					}
				} else{
					if(y < this.bounds.centerY){
						if(z < this.bounds.centerZ){
							return this.lowerSouthEast;
						} else{
							return this.upperSouthEast;
						}
					} else{
						if(z < this.bounds.centerZ){
							return this.lowerNorthEast;
						} else{
							return this.upperNorthEast;
						}
					}
				}
			} else {
				return null;
			}
		}
		
		/* default */ T get(final double x, final double y, final double z, final MutableDouble bestDistance){
			if(this.hasChildren){
				T closest = null;
				Node<T> bestChild = this.getChild(x, y, z);
				if(bestChild != null){
					closest = bestChild.get(x, y, z, bestDistance);
				}
				if(bestChild != this.lowerSouthWest && this.lowerSouthWest.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.lowerSouthWest.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.lowerSouthEast && this.lowerSouthEast.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.lowerSouthEast.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.lowerNorthWest && this.lowerNorthWest.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.lowerNorthWest.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.lowerNorthEast && this.lowerNorthEast.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.lowerNorthEast.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.upperSouthWest && this.upperSouthWest.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.upperSouthWest.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.upperSouthEast && this.upperSouthEast.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.upperSouthEast.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.upperNorthWest && this.upperNorthWest.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.upperNorthWest.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				if(bestChild != this.upperNorthEast && this.upperNorthEast.bounds.calcDistance(x, y, z) < bestDistance.value){
					T value = this.upperNorthEast.get(x, y, z, bestDistance);
					if (value != null){ closest = value; }
				}
				return closest;
			}
			
			/* No more children, so we must contain the closest object. */
			if( this.leaf != null && this.leaf.values.size() > 0 ){
				T value = this.leaf.values.get(0);
				double distance = Math.sqrt(
						Math.pow(this.leaf.x - x, 2) +
						Math.pow(this.leaf.y - y, 2) +
						Math.pow(this.leaf.z - z, 2));
				if( distance < bestDistance.value ){
					bestDistance.value = distance;
					return value;
				}
			}
			return null;
		}
	}

	
	public static class Leaf<T> implements Serializable{
		private static final long serialVersionUID = 8014582856080344739L;
		final public double x;
		final public double y;
		final public double z;
		final public List<T> values;

		public Leaf(double x, double y, double z, T value) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.values = new ArrayList<T>(1);
			this.values.add(value);
		}
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

	
}

