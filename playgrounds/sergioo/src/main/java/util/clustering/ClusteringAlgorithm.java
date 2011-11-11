package util.clustering;

import java.util.Map;
import java.util.Set;

import util.algebra.PointND;

public interface ClusteringAlgorithm<T> {

	//Methods
	public Map<Integer, Cluster<T>> getClusters(int size, Set<PointND<T>> points);
	
}
