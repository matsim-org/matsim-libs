package saleem.stockholmmodel.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
/**
 * A class to convert a list to array.
 * 
 * @author Mohammad Saleem
 *
 */
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
}
