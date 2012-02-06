package others.sergioo.util.clustering;

import java.util.Map;
import java.util.Set;

import others.sergioo.util.algebra.PointND;

public interface ClusteringAlgorithm<T> {

	//Methods
	public Map<Integer, Cluster<T>> getClusters(int size, Set<PointND<T>> points);
	
}
