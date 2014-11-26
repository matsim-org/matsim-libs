/* *********************************************************************** *
 * project: org.matsim.*
 * SuperQueue.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor;

public class SuperQueue {

	public static void main(String[] args) {

		int numCells = 10;
		double timeStepSizeInSeconds = 1;
		double rhoMaxInPersonsPerMeter = 4;
		double backwardShockWaveSpeed = 1.34;
		double forwardPedestrianSpeed = 1.34;
		double conflictDelay = .6;
		double lengthInMeter = forwardPedestrianSpeed * timeStepSizeInSeconds
				* 2;
		double flowCapacity = forwardPedestrianSpeed * rhoMaxInPersonsPerMeter
				/ 20;

		double[] rho1 = new double[numCells];
		double[] rho2 = new double[numCells];
		double[] sigma1 = new double[numCells];
		double[] sigma2 = new double[numCells];
		double[] delta1 = new double[numCells];
		double[] delta2 = new double[numCells];
		double[] outflow1 = new double[numCells];
		double[] outflow2 = new double[numCells];

		rho1[0] = rhoMaxInPersonsPerMeter;
		rho2[rho2.length - 1] = rhoMaxInPersonsPerMeter;

		for (double timeStep = 0; timeStep < 100; timeStep++) {
			for (int i = 0; i < rho1.length; i++) {

				delta2[i] = Math.min(forwardPedestrianSpeed * rho2[i],
						flowCapacity);
				delta1[i] = Math.min(forwardPedestrianSpeed * rho1[i],
						flowCapacity);

				double space = (rhoMaxInPersonsPerMeter - rho1[i] - rho2[i])
						* lengthInMeter / (2 * timeStepSizeInSeconds);
				sigma1[i] = Math.min(
						space,
						Math.max(1e-1 * flowCapacity, rhoMaxInPersonsPerMeter
								/ rho2[i] * (1 / conflictDelay - delta2[i])));
				sigma2[i] = Math.min(
						space,
						Math.max(1e-1 * flowCapacity, rhoMaxInPersonsPerMeter
								/ rho1[i] * (1 / conflictDelay - delta1[i])));

				// sigma1[i] = sigma1[i] < 0 ? 0 : sigma1[i];
				// sigma2[i] = sigma2[i] < 0 ? 0 : sigma2[i];
			}

			for (int i = 0; i < rho1.length - 1; i++) {
				outflow1[i] = Math.min(delta1[i], sigma1[i + 1]);
				outflow2[i + 1] = Math.min(delta2[i + 1], sigma2[i]);

			}
			for (int i = 0; i < rho1.length - 1; i++) {
				double out1 = outflow1[i] * timeStepSizeInSeconds
						/ lengthInMeter;
				rho1[i] -= out1;
				rho1[i + 1] += out1;

				double out2 = outflow2[i + 1] * timeStepSizeInSeconds
						/ lengthInMeter;
				rho2[i + 1] -= out2;
				rho2[i] += out2;
			}
			System.out.print(timeStep + ":\t");
			for (int i = 0; i < rho1.length; i++) {
				System.out.print(rho1[i] + "\t");
			}
			System.out.println();
			System.out.print(timeStep + ":\t");
			for (int i = 0; i < rho1.length; i++) {
				System.out.print(rho2[i] + "\t");
			}
			System.out.println();
			System.out.println();

		}

	}
}
