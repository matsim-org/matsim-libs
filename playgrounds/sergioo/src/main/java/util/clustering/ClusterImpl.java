package util.clustering;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import util.algebra.PointND;
import util.algebra.PointNDImpl;

public class ClusterImpl<T> implements Cluster<T> {

	
	//Attributes
	protected Set<PointND<T>> points;
	
	//Constructors
	public ClusterImpl() {
		points = new HashSet<PointND<T>>();
	}
	public ClusterImpl(PointND<T> point) {
		points = new HashSet<PointND<T>>();
		points.add(point);
	}
	public ClusterImpl(Set<PointND<T>> points) {
		points = new HashSet<PointND<T>>();
		for(PointND<T> point:points)
			this.points.add(point);
	}

	//Methods
	@Override
	public Set<PointND<T>> getPoints() {
		return points;
	}
	@Override
	public void addPoint(PointND<T> point) {
		points.add(point);
	}
	@Override
	public void removePoint(PointND<T> point) {
		points.remove(point);
	}
	@Override
	public PointND<T> getMean() {
		return null;
	}
	@Override
	public PointND<T> getMainPoint() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public PointND<T> getRandomPoint() {
		PointND<T> randomElement = null;
		Iterator<PointND<T>> pointIterator = points.iterator();
		double random = Math.random()*points.size();
		for(int i=0; i<random; i++)
			randomElement = pointIterator.next();
		return randomElement;
	}
	@Override
	public boolean isInCluster(PointND<T> point) {
		return points.contains(point);
	}
	
	public static class Double extends ClusterImpl<java.lang.Double> {
		
		//Constructors
		public Double() {
			super();
		}
		public Double(PointND<java.lang.Double> point) {
			super(point);
		}
		public Double(Set<PointND<java.lang.Double>> points) {
			super(points);
		}
		
		//Methods
		@Override
		public PointND<java.lang.Double> getMean() {
			if(points.size()==0)
				return null;
			else {
					PointND<java.lang.Double> mean = new PointNDImpl.Double(points.iterator().next().getDimension(), 0.0);
					for(PointND<java.lang.Double> point:points)
						for(int i=0; i<mean.getDimension(); i++)
							mean.setElement(i, mean.getElement(i)+point.getElement(i));
					for(int i=0; i<mean.getDimension(); i++)
						mean.setElement(i, mean.getElement(i)/points.size());
					return mean;
			}
		}
		
	}
	
}
