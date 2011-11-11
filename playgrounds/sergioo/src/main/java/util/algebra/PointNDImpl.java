package util.algebra;

import java.util.ArrayList;
import java.util.List;

public abstract class PointNDImpl<T> implements PointND<T>{

	//Attributes
	protected List<T> elements;
	
	//Constructors
	public PointNDImpl (int dimension) {
		elements = new ArrayList<T>(dimension);
	}
	public PointNDImpl (int dimension, T initialElement) {
		elements = new ArrayList<T>();
		for(int i=0; i<dimension; i++)
			elements.add(initialElement);
	}
	public PointNDImpl (List<T> elements) {
		this.elements = new ArrayList<T>();
		for(T element:elements)
			this.elements.add(element);
	}
	
	//Methods
	@Override
	public int getDimension() {
		return elements.size();
	}
	@Override
	public T getElement(int position) {
		return elements.get(position);
	}
	@Override
	public void setElement(int position, T element) {
		elements.set(position, element);
	}
	@Override
	public abstract double getDistance(PointND<T> other);

	@Override
	public abstract PointND<T> clone();

	public static class Double extends PointNDImpl<java.lang.Double> {
		
		//Constructors
		public Double(int dimension) {
			super(dimension);
		}
		public Double (int dimension, java.lang.Double initialElement) {
			super(dimension, initialElement);
		}
		public Double (List<java.lang.Double> elements) {
			super(elements);
		}
		
		//Methods
		@Override
		public double getDistance(PointND<java.lang.Double> other)  {
			double distance = 0;
			for(int i=0; i<elements.size(); i++)
				distance += Math.pow(elements.get(i)-other.getElement(i),2);
			return Math.sqrt(distance);
		}
		@Override
		public PointND<java.lang.Double> clone() {
			return new PointNDImpl.Double(elements);
		}
		
	}
	
}
