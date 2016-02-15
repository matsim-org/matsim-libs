package saleem.stockholmscenario.utils;

import java.util.ArrayList;
import java.util.Iterator;

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
}
