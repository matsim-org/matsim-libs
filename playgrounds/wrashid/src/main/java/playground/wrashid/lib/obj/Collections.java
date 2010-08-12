package playground.wrashid.lib.obj;

import java.util.Collection;
import java.util.Iterator;

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
	
}
