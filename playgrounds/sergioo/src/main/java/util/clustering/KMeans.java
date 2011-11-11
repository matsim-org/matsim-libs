package util.clustering;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.algebra.PointND;

public class KMeans implements ClusteringAlgorithm<Double> {

	//Methods
	@Override
	public Map<Integer, Cluster<Double>> getClusters(int size, Set<PointND<Double>> points) {
		Cluster<Double> all = new ClusterImpl<Double>(points);
		Map<Integer, Cluster<Double>> clusters = new HashMap<Integer, Cluster<Double>>();
		for(int i=0; i<size; i++) {
			PointND<Double> random = null;
			boolean free;
			do {
				free = true;
				random = all.getRandomPoint();
				for(int j=0; j<i && free; j++)
					if(clusters.get(j).isInCluster(random))
						free = false;
			} while(!free);
			clusters.put(i, new ClusterImpl<Double>(random));
		}
		for(PointND<Double> point:points) {
			int nearestClusterKey = 0;
			for(int i=0; i<size; i++)
				if(clusters.get(i).getMean().getDistance(point)<clusters.get(nearestClusterKey).getMean().getDistance(point))
					nearestClusterKey = i;
			clusters.get(nearestClusterKey).addPoint(point);
		}
		boolean changes = true;
		while(changes) {
			changes = false;
			Map<Integer, Cluster<Double>> newClusters = new HashMap<Integer, Cluster<Double>>();
			for(int i=0; i<size; i++)
				newClusters.put(i,new ClusterImpl<Double>());
			for(Integer currentClusterKey: clusters.keySet())	
				for(PointND<Double> point:clusters.get(currentClusterKey).getPoints()) {
					int nearestClusterKey = 0;
					for(int i=0; i<size; i++)
						if(clusters.get(i).getMean().getDistance(point)<clusters.get(nearestClusterKey).getMean().getDistance(point))
							nearestClusterKey = i;
					newClusters.get(nearestClusterKey).addPoint(point);
					if(currentClusterKey!=nearestClusterKey)
						changes = true;
				}
			clusters = newClusters;
		}
		return clusters;
	}
	
}
