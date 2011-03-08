package playground.wrashid.lib.obj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Collections {

	public static double[] convertDoubleCollectionToArray(Collection<Double> values){
		double[] doubleArray= new double[values.size()];
		
		int i=0;
		for (double collectionValue:values){
			doubleArray[i]=collectionValue;
			i++;
		}
		
		return doubleArray;
	}
	
	public static List getSortedKeySet(Collection keySet){
		List keys = new ArrayList(keySet); 
		keys.remove(null);
		java.util.Collections.sort(keys);
		return keys;
	}
	
	
}
