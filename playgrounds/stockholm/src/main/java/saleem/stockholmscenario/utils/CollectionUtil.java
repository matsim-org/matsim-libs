package saleem.stockholmscenario.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class CollectionUtil<Type> {
	public ArrayList<Type> toArrayList(Iterator<Type> iter){
		ArrayList<Type> arraylist = new ArrayList<Type>();
		while(iter.hasNext()){
			arraylist.add(iter.next());
		}
		return arraylist;
	}
	public double[] toArray(List<Double> alist){//Converting a list to array
		double[] array = new double[alist.size()];
		Iterator<Double> iter = alist.iterator();
		int i=0;
		while(iter.hasNext()){
			array[i]=iter.next();
			i++;
		}
		return array;
	}
	public double[] toArrayDelays(ArrayList<Double> alist){//Converting a list to array and reducing the slope
		double[] array = new double[alist.size()];
		Iterator<Double> iter = alist.iterator();
		int i=0;
		while(iter.hasNext()){
			array[i]=iter.next()/10;
			i++;
		}
		return array;
	}
}
