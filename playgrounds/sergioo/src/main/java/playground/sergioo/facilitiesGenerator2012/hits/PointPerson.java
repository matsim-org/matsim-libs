package playground.sergioo.facilitiesGenerator2012.hits;

import java.util.Collection;

import org.apache.commons.math3.ml.clustering.Clusterable;

import others.sergioo.util.algebra.PointNDImpl;

public class PointPerson extends PointNDImpl.Double implements Clusterable {
	
	//Attributes
	private String id;
	private String occupation;
	private double weight = 1;
	private String placeType;
	
	//Constructors
	public PointPerson(String id, String occupation, java.lang.Double[] times, String placeType) {
		super(times);
		this.id = id;
		this.occupation = occupation;
		this.placeType = placeType;
	}
	private PointPerson(int dimension, java.lang.Double initialElement) {
		super(dimension, initialElement);
	}
	
	//Methods
	public String getId() {
		return id;
	}
	public String getOccupation() {
		return occupation;
	}
	public String getPlaceType() {
		return placeType;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	public double distanceFrom(PointPerson p) {
		return getDistance(p);
	}
	public PointPerson centroidOf(Collection<PointPerson> points) {
		PointPerson mean = new PointPerson(points.iterator().next().getDimension(), 0.0);
		double totalWeight = 0;
		for(PointPerson point:points) {
			totalWeight += point.getWeight();
			for(int i=0; i<mean.getDimension(); i++)
				mean.setElement(i, (java.lang.Double)mean.getElement(i)+point.getWeight()*(java.lang.Double)point.getElement(i));
		}
		for(int i=0; i<mean.getDimension(); i++)
			mean.setElement(i, mean.getElement(i)/totalWeight);
		return mean;
	}
	
}
