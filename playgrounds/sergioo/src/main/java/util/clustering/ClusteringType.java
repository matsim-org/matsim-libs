package util.clustering;

import java.util.Set;

import util.algebra.PointND;

public interface ClusteringType {
	
	public double getDistance(ClusteringType other);
	public PointND<ClusteringType> getMean(Set<PointND<ClusteringType>> set);
	
}
