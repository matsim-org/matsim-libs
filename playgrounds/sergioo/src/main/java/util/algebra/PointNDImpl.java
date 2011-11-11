package util.algebra;

import java.util.ArrayList;
import java.util.List;

public class PointNDImpl<T> implements PointND<T>{

	//Attributes
	private int dimension;
	private List<T> elements;
	
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
		dimension = elements.size();
		this.elements = new ArrayList<T>();
		for(T element:elements)
			this.elements.add(element);
	}
	
	//Methods
	@Override
	public int getDimension() {
		return dimension;
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
	public double getDistance(PointND<T> other) {
		if(elements.get(0).getClass()==Double.class && other.getElement(0).getClass()==Double.class) {
			double distance = 0;
			for(int i=0; i<elements.size(); i++)
				distance += Math.pow(((Double)elements.get(i))-((Double)other.getElement(i)),2);
			return Math.sqrt(distance);
		}
		else
			return 0;
	}
	@Override
	public PointND<T> clone() {
		return new PointNDImpl<T>(elements);
	}

}
