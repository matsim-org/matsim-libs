package playground.anhorni.utils;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

public abstract class Utils {

	private final static Logger log = Logger.getLogger(Utils.class);

	// -------------------------------------------------------------
	public static double median(Double [] values) {
		List<Double> list = new Vector<Double>();

		for (int i = 0; i < values.length; i++) {
			list.add(values[i]);
		}
		return median(list);
	}
	
	public static double median(List<Double> values) {

		if (values.size() == 0) return 0.0;

	    Collections.sort(values);
	    if (values.size() % 2 == 1) {
	    	return values.get((values.size()+1)/2-1);
	    }
	    else {
	    	double lower = values.get(values.size()/2-1);
	    	double upper = values.get(values.size()/2);
	    	return (lower + upper) / 2.0;
	    }
	}
	
	// -------------------------------------------------------------
	public static double mean(List<Double> values) {
		double sum = 0.0;
		int cnt = 0;
		if (values.size() == 0) return 0.0;
		for (Double value : values) {
			sum += value;
			cnt++;
		}
		return sum / cnt;
	}
	
	public static double weightedMean(List<Double> values, List<Double> weights) {
		return weightedMean((Double[])values.toArray(new Double[values.size()]), (Double[])weights.toArray(new Double[weights.size()]));
	}
	
	public static double weightedMean(Double[] values, Double[] weights) {
		double sumValues = 0.0;
		double sumWeights = 0.0;
		
		if (values.length == 0) return 0.0;
		
		if (values.length  != weights.length ) {
			log.info("size of weights and values not identical");
			return -1;
		}
		for (int index = 0; index < values.length; index++) {
			sumValues += (values[index] * weights[index]);
			sumWeights += weights[index];
		}
		return sumValues / sumWeights;
	}
	
	// -------------------------------------------------------------
	
	public static double getStdDev(List<Double> values) {
		double mean = Utils.mean(values);

		double sum = 0.0;
		for (Double v : values) {
			sum += (v-mean) * (v-mean);
		}
		return Math.sqrt(sum / values.size());
	}
	
	
	// -------------------------------------------------------------
	
	public static double getVariance(List<Double> values) {
		double mean = Utils.mean(values);

		double sum = 0.0;
		for (Double v : values) {
			sum += (v-mean) * (v-mean);
		}
		return sum / values.size();
	}

	public static double getMin(List<Double> values) {
		double minVal = Double.MAX_VALUE;
		for (Double v : values) {
			if (v < minVal) {
				minVal = v;
			}
		}
		return minVal;
	}

	public static double getMax(List<Double> values) {
		return getMax((Double[])values.toArray(new Double[values.size()]));
	}
	
	public static double getMax(Double[] values) {
		double maxVal = Double.MIN_VALUE;
		for (Double v : values) {
			if (v > maxVal) {
				maxVal = v;
			}
		}
		return maxVal;
	}


// =============================================================================================================	

	
	
	// -----------------
	// dirty hack in a weak minute. Cast problems.
	public static double [] divideAndConvert(List<Double> divisorList, List<Double> denominatorList) {

		double [] array = new double[divisorList.size()];
		if (divisorList.size() != denominatorList.size()) {
			log.error("list do not have the same size!");
		}
		else {
			for (int i = 0; i < divisorList.size(); i++) {
				array[i] = divisorList.get(i) / denominatorList.get(i);
			}
		}
		return array;
	}

	// dirty hack in a weak minute. Cast problems.
	public static double [] convert(List<Double> list) {
		double [] array = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public static String [] convert2String(List<Double> list) {
		String [] array = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i).toString();
		}
		return array;
	}

	public static double [] convert2double(int input[]) {
		double [] array = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			array[i] = input[i];
		}
		return array;
	}

	public static String convertModeMZ2Plans(int wmittel) {

			 //2: Bahn 3: Postauto 5: Tram 6: Bus
			if (wmittel == 2 || wmittel == 3 || wmittel == 5 || wmittel == 6) {
				return "pt";
			}
			// MIV
			//9: Auto  11: Taxi 12: Motorrad, Kleinmotorrad 13: Mofa
			else if (wmittel == 9 || wmittel == 11 || wmittel == 12 || wmittel == 13) {
				return "car";
			}
			//14: Velo
			else if (wmittel == 14) {
				return "bike";
			}
			//15: zu Fuss
			else if (wmittel == 15) {
				return "walk";
			}
			return "undefined";
	}

	public static String getActType(Plan plan, Activity act) {
		if (!act.getType().startsWith("h")) {
			return act.getType();
		}
		else {		// its a trip back home
			// not the first home act!
			if (((PlanImpl) plan).getPreviousLeg(act) != null) {
				return getLongestActivityForRoundTrip((PlanImpl) plan, ((PlanImpl) plan).getPreviousActivity(((PlanImpl) plan).getPreviousLeg(act)));
			}
			else {
				return "home";
			}
		}
	}

	private static String getLongestActivityForRoundTrip(final PlanImpl plan, Activity act) {
		double maxActDur = - 1.0;

		// set it to home in case of home-based round trips
		String longestActivity = "home";

		// home_pre <- ... act (== home)
		ActivityImpl actTemp = (ActivityImpl) act;
		while (actTemp != null && !actTemp.getType().startsWith("h")) {
			actTemp = (ActivityImpl) plan.getPreviousActivity(plan.getPreviousLeg(actTemp));
			if (actTemp.getDuration() > maxActDur && !actTemp.getType().startsWith("h")) {
				maxActDur = actTemp.getDuration();
				longestActivity = actTemp.getType();
			}
		}
		return longestActivity;
	}
}
