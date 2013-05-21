package playground.dziemke.potsdam.analysis.disaggregated.adapted;

import java.util.Comparator;


public class DoubleIdComparator implements Comparator <DoubleId> {

	@Override
	public int compare(DoubleId arg0, DoubleId arg1) {
		double age0=(arg0).time;
		double age1=(arg1).time;
		if (age0>age1) {
			return 1;
		}
		else if(age0<age1){
			return -1;
		}
		return 0;
	}
	
}