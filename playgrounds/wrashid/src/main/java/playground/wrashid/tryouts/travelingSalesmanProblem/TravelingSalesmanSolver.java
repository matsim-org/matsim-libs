/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.tryouts.travelingSalesmanProblem;

/*
 * Created on 08-Jan-2006
 * 
 * This source code is released under the GNU Lesser General Public License Version 3, 29 June 2007
 * see http://www.gnu.org/licenses/lgpl.html or the plain text version of the LGPL included with this project
 * 
 * It comes with no warranty whatsoever
 * 
 */

import java.util.Random;

/**
 * Run the traveling salesman solver
 * 
 * The genetic algorithm will run untill it is aborted, and prints the current
 * best solution from time to time
 * 
 * Edit the source to switch to brute force algorithm, change the random seed or
 * the number of cities Some parameters are hardcoded in the Environment class
 * This should be improved in future versions. The current state of the code is
 * merely a proof of concept, little attention has been paid to proper
 * engineering.
 * 
 * @author Bjoern Guenzel - http://blog.blinker.net
 * @author rashid_waraich (added some code)
 */
public class TravelingSalesmanSolver {

	public final static int RANDOM_SEED = 1234;

	public final static boolean RUN_BRUTE_FORCE = false;
	public final static boolean RUN_GENETIC_ALGORITHM = true;

	public static Random random = new Random(RANDOM_SEED);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO interpret arguments, for example random seed and number of
		// cities, and brute force or genetic algorithm could be

		int[][] coordinates = new int[4][2];
		// car_park_name = M Hotel
		coordinates[0][0] = 371500;
		coordinates[0][1] = 140784;

		// car_park_name = Amara Hotel
		coordinates[1][0] = 371389;
		coordinates[1][1] = 140960;

		// car_park_name = International Plaza

		coordinates[2][0] = 371611;
		coordinates[2][1] = 141024;

		// Icon Village
		coordinates[3][0] = 371389;
		coordinates[3][1] = 140971;

		TravelingSalesman salesman = new TravelingSalesman(coordinates);

		salesman.printCosts();

		// uncomment for brute force:
		if (RUN_BRUTE_FORCE) {
			System.out.println("*** running brute force algorithm ***");
			TravelingSalesmanBruteForce bruteForce = new TravelingSalesmanBruteForce(salesman);
			bruteForce.run();
		}

		if (RUN_GENETIC_ALGORITHM) {
			System.out.println("*** running genetic algorithm algorithm ***");
			Environment environment = new Environment(salesman);

			environment.run();
		}
	}

}
