/* *********************************************************************** *
 * project: org.matsim.*
 * BiTreeGrid.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.gis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * @author illenberger
 *
 */
public class BiTreeGrid<T> {

	private Tile<T> root;
	
	public BiTreeGrid(Envelope rootEnvelope) {
		root = new Tile<T>();
		root.envelope = rootEnvelope;
	}
	
	public BiTreeGrid(BiTreeGrid<?> grid) {
		root = new Tile<T>();
		root.envelope = new Envelope(grid.getRoot().envelope);
		deepCopy((Tile<?>) grid.getRoot(), root);
		
	}
	
	public Tile<T> getTile(Coordinate c) {
		return getTile(c, root);
	}
	
	private Tile<T> getTile(Coordinate c, Tile<T> parent) {
		if(parent.envelope.contains(c)) {
			if(parent.children == null) {
				return parent;
			} else {
				for(Tile<T> child : parent.children) {
					Tile<T> r = getTile(c, child);
					if(r != null) {
						return r;
					}
				}
				return null;
			}
		} else {
			return null;
		}
	}
	
	public Set<Tile<T>> tiles() {
		Set<Tile<T>> tiles = new HashSet<BiTreeGrid.Tile<T>>();
		addTiles(root, tiles);
		return tiles;
	}
	
	private void addTiles(Tile<T> tile, Set<Tile<T>> tiles) {
		if(tile.children == null) {
			tiles.add(tile);
		} else {
			for(Tile<T> child : tile.children) {
				addTiles(child, tiles);
			}
		}
	}
	
	void deepCopy(Tile<?> tile, Tile<T> copy) {
		if(tile.children != null) {
			copy.children = new ArrayList<Tile<T>>(2);
			for(Tile<?> child : tile.children) {
				Tile<T> childCopy = new Tile<T>();
				childCopy.parent = copy;
				childCopy.envelope = new Envelope(child.envelope);
				copy.children.add(childCopy);
				
				deepCopy(child, childCopy);
			}
		}
	}
	
	Tile<T> getRoot() {
		return root;
	}
	
	void splitTile(Tile<T> tile) {
		double x = tile.envelope.getMinX();
		double y = tile.envelope.getMinY();
		double dx = tile.envelope.getWidth();
		double dy = tile.envelope.getHeight();

		Envelope e1;
		Envelope e2;

		if (dx > dy) {
			// split vertically
			e1 = new Envelope(x, x + dx / 2.0, y, y + dy);
			e2 = new Envelope(x + dx / 2.0, x + dx, y, y + dy);
		} else {
			// split horizontally
			e1 = new Envelope(x, x + dx, y, y + dy / 2.0);
			e2 = new Envelope(x, x + dx, y + dy / 2.0, y + dy);
		}
		
		Tile<T> t1 = new Tile<T>();
		t1.parent = tile;
		t1.envelope = e1;

		Tile<T> t2 = new Tile<T>();
		t2.parent = tile;
		t2.envelope = e2;
		
		tile.children = new ArrayList<Tile<T>>(2);
		tile.children.add(t1);
		tile.children.add(t2);
	}
	
	public static class Tile<E> {
		
		private Tile<E> parent;
		
		List<Tile<E>> children;
		
		public Envelope envelope;
		
		public E data; //TODO
	}
}
