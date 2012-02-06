package others.sergioo.util.clustering;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import others.sergioo.util.algebra.PointND;

public class KMeans<T> implements ClusteringAlgorithm<T> {

	//Methods
	@Override
	public Map<Integer, Cluster<T>> getClusters(int size, Set<PointND<T>> points) {
		Cluster<T> all = new ClusterImpl<T>(points);
		Map<Integer, Cluster<T>> clusters = new HashMap<Integer, Cluster<T>>();
		for(int i=0; i<size; i++) {
			PointND<T> random = null;
			boolean free;
			do {
				free = true;
				random = all.getRandomPoint();
				for(int j=0; j<i && free; j++)
					if(clusters.get(j).isInCluster(random))
						free = false;
			} while(!free);
			clusters.put(i, new ClusterImpl<T>(random));
		}
		for(PointND<T> point:points) {
			int nearestClusterKey = 0;
			for(int i=0; i<size; i++)
				if(clusters.get(i).getMainPoint().getDistance(point)<clusters.get(nearestClusterKey).getMainPoint().getDistance(point))
					nearestClusterKey = i;
			clusters.get(nearestClusterKey).addPoint(point);
		}
		int changes = 1;
		while(changes>0) {
			changes = 0;
			Map<Integer, Cluster<T>> newClusters = new HashMap<Integer, Cluster<T>>();
			for(int i=0; i<size; i++)
				newClusters.put(i,new ClusterImpl<T>());
			for(Integer currentClusterKey: clusters.keySet())	
				for(PointND<T> point:clusters.get(currentClusterKey).getPoints()) {
					int nearestClusterKey = 0;
					for(int i=0; i<size; i++)
						if(!clusters.get(i).isEmpty() && clusters.get(i).getMean().getDistance(point)<clusters.get(nearestClusterKey).getMean().getDistance(point))
							nearestClusterKey = i;
					newClusters.get(nearestClusterKey).addPoint(point);
					if(currentClusterKey!=nearestClusterKey)
						changes++;
				}
			clusters = newClusters;
			System.out.println(changes);
		}
		return clusters;
	}
	
}
