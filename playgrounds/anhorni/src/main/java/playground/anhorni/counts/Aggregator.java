package playground.anhorni.counts;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

public class Aggregator {
	private final static Logger log = Logger.getLogger(Aggregator.class);
	
	// volumes per hour
	private TreeMap<Integer, List<Double>> volumes = new TreeMap<Integer, List<Double>>();
	
	// volumes per day
	private TreeMap<Integer, List<Double>> volumesDay = new TreeMap<Integer, List<Double>>();
		
	private double [] avg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double [] median = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
		0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double [] standarddev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
		0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double avgDay = 0.0;
	private double standarddevDay = 0.0;
		
	public void aggregate(TreeMap<Integer, List<Double>> volumes, TreeMap<Integer, List<Double>> volumesDay, boolean removeOutliers) {
		this.volumes = volumes;	
		this.volumesDay = volumesDay;
		if (removeOutliers) this.removeOutliers();
		this.avg();
		this.standarddev_s();
		this.median();	
		
		this.avgDay = this.avgDay();
		this.standarddevDay = this.standarddevDay_s();
	}
		
	public int getSize(int hour) {
		return this.volumes.get(hour).size();
	}
	
	public TreeMap<Integer, List<Double>> getVolumes() {
		return volumes;
	}

	public TreeMap<Integer, List<Double>> getVolumesDay() {
		return volumesDay;
	}

	private void removeOutliers() {
		
		for (int hour = 0; hour < 24; hour++) {		
			List<Double> filteredVolumes = new Vector<Double>();		
			List<Double> volumesTemp = volumes.get(hour);
			Collections.sort(volumesTemp);
			
				//This exactly reproduces the former PERL script
						
//						int numberOfElements = volumesTemp.size();
//						int numberOfElements2Remove = (int)Math.ceil(numberOfElements * 0.05);
//			
//						if (volumesTemp.size() > 0) {			
//							filteredVolumes.addAll(volumesTemp.subList(
//									(int)Math.ceil(numberOfElements2Remove/2.0), (int)Math.floor(numberOfElements - numberOfElements2Remove/2.0)));	
//							
//							volumes.get(hour).clear();
//							volumes.get(hour).addAll(filteredVolumes);
//						}
			
			int numberOfElements = volumesTemp.size();
			int numberOfElements2RemovePerSide = (int)Math.round(numberOfElements * 0.05 / 2.0);

			if (volumesTemp.size() > 0) {
				
				// List<E> subList(int fromIndex, int toIndex);
				// @param fromIndex low endpoint (inclusive) of the subList.
			    // @param toIndex high endpoint (exclusive) of the subList.
				
				filteredVolumes.addAll(volumesTemp.subList(
						numberOfElements2RemovePerSide, (int)Math.round(numberOfElements - numberOfElements2RemovePerSide)));	
								
				volumes.get(hour).clear();
				volumes.get(hour).addAll(filteredVolumes);
			}
		}
	}
	
	public double getDailyVolume(int date) {
		double dailyVol = 0.0;
		for (double hourlyVol : this.volumesDay.get(date)) {
			dailyVol += hourlyVol;
		}
		return dailyVol;
	}
	
	private double avgDay() {
		int n = 0;
		double avgDayTmp = 0.0;
		for (Integer date : this.volumesDay.keySet()) {
			double dailyVol = this.getDailyVolume(date);
			if (dailyVol > -1.0) { // filter undefined values
				avgDayTmp += dailyVol;
				n++;
			}
		}
		return (avgDayTmp / n);
	}
	
	private double standarddevDay_s() {
		int n = 0;
		double variance = 0.0;
		for (Integer date : this.volumesDay.keySet()) {
			double dailyVol = this.getDailyVolume(date);
			if (dailyVol > -1.0) { // filter undefined values
				variance += Math.pow(dailyVol - this.avgDay , 2.0);
				n++;
			}
		}
		if (n == 0) {
			log.error("Something went wrong (n == 0) for the daily volumes"); 
			return 0.0;
		}
		variance /= ( n - 1 );
		return Math.sqrt(variance);
	}
	
	private void avg() {	
		for (int hour = 0; hour < 24; hour++) {
			int n = 0;
			for (double vol : volumes.get(hour)) {
				if (vol > -1.0) { // filter undefined values
					avg[hour] += vol;
					n++;
				}
			}
			avg[hour] /= n;
		}	
	}
	
	private void standarddev_s() {
		for (int hour = 0; hour < 24; hour++) {
			double variance = 0.0;
			int n = 0;
			for (double vol : volumes.get(hour)) {
				if (vol > -1.0) { // filter undefined values
					variance += Math.pow(vol - avg[hour] , 2.0);
					n++;
				}
			}
			if (n == 0) {
				log.error("Something went wrong (n == 0) for hour :" + hour); 
				return;
			}
			variance /= ( n - 1 );
			standarddev[hour] = Math.sqrt(variance);
		}	
	}

// old version: 
//	private void standarddev() {
//		for (int hour = 0; hour < 24; hour++) {
//			Iterator<Double> vol_it;
//			vol_it = volumes.get(hour).iterator();
//			while (vol_it.hasNext()) {
//				double vol = vol_it.next();		
//				if (vol > -1.0) {
//					standarddev[hour] += Math.pow(vol - avg[hour] , 2.0);
//				}
//			}
//		}
//		for (int i = 0; i < 24; i++) {
//			standarddev[i] = Math.sqrt(standarddev[i] / volumes.get(i).size());
//		}	
//	}
		
	private void median() {
		for (int i = 0; i < 24; i++) {
			median[i] = this.medianPerHour(volumes.get(i));
		}
	}
	
	private double medianPerHour(List<Double> values) {
		
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

	public double getAvgDay() {
		return avgDay;
	}

	public void setAvgDay(double avgDay) {
		this.avgDay = avgDay;
	}

	public double getStandarddevDay() {
		return standarddevDay;
	}

	public void setStandarddevDay(double standarddevDay) {
		this.standarddevDay = standarddevDay;
	}

	public double[] getAvg() {
		return avg;
	}
	public double[] getMedian() {
		return median;
	}
	public double[] getStandarddev() {
		return standarddev;
	}	
}
