package playground.mmoyo.Validators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Id;

public class CostValidator {
	private Map<Id,List<double[]>> negativeValuesMap = new TreeMap<Id,List<double[]>>();

	public CostValidator (){
		
	}
		
	public void pushNegativeValue(Id idLink, double time, double cost){
		double[] negativeArray = {time, cost}; 
		if (!negativeValuesMap.containsKey(idLink)){
			List<double[]> negativeList = new ArrayList<double[]>();
			negativeValuesMap.put(idLink, negativeList);
		}
		negativeValuesMap.get(idLink).add(negativeArray);
	}

	public void printNegativeVaues(){
		System.out.println("Link - Time - Cost");
		Iterator <Map.Entry<Id,List<double[]>>> iter = negativeValuesMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Id,List<double[]>> entry =  iter.next();
			List<double[]> list = (List<double[]>) entry.getValue();
			for (double[]  v : list){
				System.out.println(entry.getKey() + "	" + v[0] + "	" + v[1]);
			}
		}
		iter = null;   			
	}
	
}
