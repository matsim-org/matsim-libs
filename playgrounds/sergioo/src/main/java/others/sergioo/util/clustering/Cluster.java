package others.sergioo.util.clustering;

import java.util.Set;

import others.sergioo.util.algebra.PointND;

public interface Cluster<T> {

	//Methods
	public Set<PointND<T>> getPoints();
	public void addPoint(PointND<T> point);
	public void removePoint(PointND<T> point);
	public PointND<T> getMean();
	public PointND<T> getMainPoint();
	public PointND<T> getRandomPoint();
	public boolean isInCluster(PointND<T> point);
	public boolean isEmpty();
	
}
