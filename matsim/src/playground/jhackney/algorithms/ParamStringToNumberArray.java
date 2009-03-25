package playground.jhackney.algorithms;

import org.matsim.core.gbl.Gbl;

public class ParamStringToNumberArray {

	double[] w;
	
	public ParamStringToNumberArray(final String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		w = new double[s.length];
		double sum = 0.;
		for (int i = 0; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue();
			if(w[i]<0.||w[i]>1.){
				Gbl.errorMsg("All parameters \"s_weights\" must be >0 and <1. Check config file.");
			}
			sum=sum+w[i];
		}
		if(s.length!=5){
			Gbl.errorMsg("Number of weights for spatial interactions must equal number of facility types. Check config.");
		}
		if(sum<0){
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
	}
	public double[] getArray(){
		return w;
	}
}
