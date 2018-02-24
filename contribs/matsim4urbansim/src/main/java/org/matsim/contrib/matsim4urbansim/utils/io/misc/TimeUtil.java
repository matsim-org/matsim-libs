package org.matsim.contrib.matsim4urbansim.utils.io.misc;

import org.junit.Assert;

/**
 * see also org.matsim.core.utils.misc.Time for time conversion
 * @author thomas
 *
 */
public class TimeUtil {
	
	public static double convertSeconds2Minutes(double sec){
		
		int m = (int)(sec / 60);
		double s = (sec % 60)/100;
		double min = m + s;
		
		return min;
	}

	/**
	 * for testing only
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		double s1 = 3600.;
		double s2 = 93.;
		double s3 = 7225.;
		
		Assert.assertEquals(60., convertSeconds2Minutes(s1));
		Assert.assertEquals(1.33, convertSeconds2Minutes(s2));
		Assert.assertEquals(120.25, convertSeconds2Minutes(s3));
	}

}
