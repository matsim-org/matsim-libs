package playground.anhorni.locationchoice.preprocess.helper;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;

public class QuadTreeRing<T> extends QuadTree<T> {

	private static final long serialVersionUID = 1L;

	public QuadTreeRing(double minX, double minY, double maxX, double maxY) {
		super(minX, minY, maxX, maxY);
	}
	
	public Collection<T> get(final double x, final double y, final double outerRadius, final double innerradius) {
		Collection<T> locations = super.get(x, y, outerRadius);
		Collection<T> innerLocations = super.get(x, y, innerradius);		
		locations.removeAll(innerLocations);
		return locations;
	}
}
