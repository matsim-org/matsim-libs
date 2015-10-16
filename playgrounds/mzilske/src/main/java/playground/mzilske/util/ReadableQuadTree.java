package playground.mzilske.util;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;

public class ReadableQuadTree<T> {
	
	private QuadTree<T> quadTree;
	
	

	public ReadableQuadTree(Map<T, Coord> entries) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for (Entry<T, Coord> entry : entries.entrySet()) {
			minX = Math.min(minX, entry.getValue().getX());
			maxX = Math.max(maxX, entry.getValue().getX());
			minY = Math.min(minY, entry.getValue().getY());
			maxY = Math.max(maxY, entry.getValue().getY());
		}
		
		quadTree = new QuadTree<T>(minX, minY, maxX, maxY);
		
		for (Entry<T, Coord> entry : entries.entrySet()) {
			quadTree.put(entry.getValue().getX(), entry.getValue().getY(), entry.getKey());
		}
		
	}

	public T get(double x, double y) {
		return quadTree.getClosest(x, y);
	}

	public int size() {
		return quadTree.size();
	}
	
	

}
