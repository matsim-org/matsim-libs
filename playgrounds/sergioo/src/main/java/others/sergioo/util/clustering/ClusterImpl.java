package others.sergioo.util.clustering;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import others.sergioo.util.algebra.PointND;
import others.sergioo.util.algebra.PointNDImpl;

public class ClusterImpl<T> implements Cluster<T> {

	
	//Attributes
	protected Set<PointND<T>> points;
	protected PointND<T> first;
	protected PointND<T> mean;
	
	//Constructors
	public ClusterImpl() {
		points = new HashSet<PointND<T>>();
	}
	public ClusterImpl(PointND<T> point) {
		points = new HashSet<PointND<T>>();
		addPoint(point);
	}
	public ClusterImpl(Set<PointND<T>> points) {
		this.points = new HashSet<PointND<T>>();
		for(PointND<T> point:points)
			addPoint(point);
	}

	//Methods
	@Override
	public Set<PointND<T>> getPoints() {
		return points;
	}
	@Override
	public void addPoint(PointND<T> point) {
		if(first==null)
			first = point;
		points.add(point);
		mean=null;
	}
	@Override
	public void removePoint(PointND<T> point) {
		points.remove(point);
		if(points.isEmpty())
			first = null;
		mean=null;
	}
	@Override
	public PointND<T> getMean() {
		if(mean==null) {
			if(Number.class.isAssignableFrom(points.iterator().next().getElement(0).getClass())) {
				if(points.size()==0)
					return null;
				else {
					PointND<java.lang.Double> mean = new PointNDImpl.Double(points.iterator().next().getDimension(), 0.0);
					for(PointND<T> point:points)
						for(int i=0; i<mean.getDimension(); i++)
							mean.setElement(i, (java.lang.Double)mean.getElement(i)+(java.lang.Double)point.getElement(i));
					for(int i=0; i<mean.getDimension(); i++)
						mean.setElement(i, mean.getElement(i)/points.size());
					this.mean = (PointND<T>) mean;
				}
			}
		}
		return mean;
	}
	@Override
	public PointND<T> getMainPoint() {
		return first;
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
	@Override
	public boolean isEmpty() {
		return points.size()==0;
	}
	
	//Static Classes
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
