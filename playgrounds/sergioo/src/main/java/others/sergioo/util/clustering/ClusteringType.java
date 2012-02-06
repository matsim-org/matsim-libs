package others.sergioo.util.clustering;

import java.util.Set;

import others.sergioo.util.algebra.PointND;

public interface ClusteringType {
	
	public double getDistance(ClusteringType other);
	public PointND<ClusteringType> getMean(Set<PointND<ClusteringType>> set);
	
}
