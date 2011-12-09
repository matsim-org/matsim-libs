package playground.wrashid.lib.obj.geoGrid;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.tools.network.obj.EnclosingRectangle;

public class QuadTreeInitializer<T> {

	public QuadTree<T> getLinkQuadTree(NetworkImpl network) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Link link : network.getLinks().values()) {
			if (link.getCoord().getX() < minX) {
				minX = link.getCoord().getX();
			}

			if (link.getCoord().getY() < minY) {
				minY = link.getCoord().getY();
			}

			if (link.getCoord().getX() > maxX) {
				maxX = link.getCoord().getX();
			}

			if (link.getCoord().getY() > maxY) {
				maxY = link.getCoord().getY();
			}
		}

		return new QuadTree<T>(minX, minY, maxX + 1.0, maxY + 1.0);
	}
	
	public QuadTree<T> getQuadTree(EnclosingRectangle rectagle){
		return new QuadTree<T>(rectagle.getMinX(), rectagle.getMinY(), rectagle.getMaxX() + 1.0, rectagle.getMaxY() + 1.0);
	}

}
