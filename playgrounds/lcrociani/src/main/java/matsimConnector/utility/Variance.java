/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package matsimConnector.utility;

/**
 * A class for incremental variance computation. The algorithm has been adapted
 * from http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
 * 
 * @author laemmel
 *
 */
public class Variance {

	double K = 0;
	double n = 0;
	double Ex = 0;
	double Ex2 = 0;

	public void addVar(double x) {
		if (n == 0) {
			K = x;
		}
		n++;
		Ex = Ex + (x - K);
		Ex2 = Ex2 + (x - K) * (x - K);
	}

	public void removeVar(double x) {
		n = n - 1;
		Ex = Ex - (x - K);
		Ex2 = Ex2 - (x - K) * (x - K);
	}

	public double getMean() {
		return K + Ex / n;
	}

	public double getVar() {
		return (Ex2 - (Ex * Ex) / n) / (n - 1);
	}
}
