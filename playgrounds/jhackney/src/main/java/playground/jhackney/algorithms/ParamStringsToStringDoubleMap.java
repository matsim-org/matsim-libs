package playground.jhackney.algorithms;

import java.util.LinkedHashMap;

import org.matsim.core.gbl.Gbl;

public class ParamStringsToStringDoubleMap {
	LinkedHashMap<String,Double> map= new LinkedHashMap<String, Double>();
	
	public ParamStringsToStringDoubleMap(final String types, final String longString) {
		String patternStr = ",";
		String[] s1;
		String[] s2;

		s1 = longString.split(patternStr);
		s2 = types.split(patternStr);
		double[] w1 = new double[s1.length];
		String[] w2 = new String[s2.length];
		double sum = 0.;
		for (int i = 0; i < s1.length; i++) {
			w1[i] = Double.valueOf(s1[i]).doubleValue();
			w2[i] = s2[i];
			if(w1[i]<0.||w1[i]>1.){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w1[i];
			map.put(w2[i],w1[i]);
		}
		if(s1.length!=s2.length){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
	}
	
	public LinkedHashMap<String,Double> getMap(){
		return map;
	}
}
