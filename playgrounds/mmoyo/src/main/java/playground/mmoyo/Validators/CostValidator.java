package playground.mmoyo.Validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

/**
 * This class makes sure that there are not negative values in standard links
 * That is possible when a PTLine has several profiles
 */
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
		System.out.println("negative values found:" + negativeValuesMap.size());
		System.out.println("Link - Time - Cost");
		for(Map.Entry <Id,List<double[]>> entry: negativeValuesMap.entrySet() ){
			List<double[]> list = entry.getValue();
			for (double[]  v : list){
				System.out.println(entry.getKey() + "	" + v[0] + "	" + v[1]);
			}
		}
	}
	
}
