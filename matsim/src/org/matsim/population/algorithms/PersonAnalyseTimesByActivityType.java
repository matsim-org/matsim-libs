/* *********************************************************************** *
 * project: org.matsim.*
 * PlansAnalyseTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.Iterator;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.misc.Time;

/**
 * Summarizes numbers of departures, arrivals and en-routes by activity type.
 * 
 * @author meisterk
 *
 */
public class PersonAnalyseTimesByActivityType extends AbstractPersonAlgorithm {

	public enum Activities {

		h (0), 
		w (1), 
		l (2), 
		s (3), 
		e (4),
		all (5);

		private final int position;

		private Activities(int position) {
			this.position = position;
		}

		public int getPosition() {
			return position;
		}

	}

	private static final int ALL_POS = Activities.valueOf("all").getPosition();

	private int timeBinSize;
	private int[][] numDeps;
	private int[][] numArrs;
	private int[][] numTraveling;

	public PersonAnalyseTimesByActivityType(int timeBinSize) {
		super();
		this.timeBinSize = timeBinSize;
		this.numDeps = new int[Activities.values().length][0 * 3600 / timeBinSize];
		this.numArrs = new int[Activities.values().length][0 * 3600 / timeBinSize];
		this.numTraveling = new int[Activities.values().length][0 * 3600 / timeBinSize];
		// how to use the enum
//		System.out.println(Activities.values().length);
//		for (int ii=0; ii < Activities.values().length; ii++) {
//		System.out.println(Activities.values()[ii]);
//		}
//		System.out.println(Activities.valueOf("h"));
//		System.out.println(Activities.valueOf("h").getPosition());
//		System.out.println(Activities.valueOf("l").getPosition());
	}

	public void run(Person person) {

//		this.analyseDepartures(person);
//		this.analyseArrivals(person);
		this.analyseTraveling(person);

	}

	public int[][] getNumDeps() {
		return numDeps;
	}

	private void analyseDepartures(Person person) {

		Plan plan = person.getPlans().get(0);

		Iterator actIter = plan.getIteratorAct();
		while (actIter.hasNext()) {

			Act act = (Act) actIter.next();
			String actType = act.getType().substring(0, 1);
			double depTime = act.getEndTime();
			if (depTime != Time.UNDEFINED_TIME) {

				int actIndex = Activities.valueOf(actType).getPosition();

				int timeIndex = ((int) depTime) / timeBinSize;
				int oldLength = numDeps[actIndex].length;

//				System.out.println("time index: " + timeIndex);
//				System.out.println("old length: " + oldLength);

				if (timeIndex >= oldLength) {

					numDeps[actIndex] = (int[]) PersonAnalyseTimesByActivityType.resizeArray(numDeps[actIndex], timeIndex + 1);

					// init new fields with 0
					for (int ii=oldLength; ii < numDeps[actIndex].length; ii++) {

						numDeps[actIndex][ii] = 0;

					}

					System.out.println("new length of " + actType + ": " + numDeps[actIndex].length);

				}

				numDeps[actIndex][timeIndex]++;

				// resize summary array too
				if (numDeps[actIndex].length > numDeps[ALL_POS].length) {

					oldLength = numDeps[ALL_POS].length;
					numDeps[ALL_POS] = (int[]) PersonAnalyseTimesByActivityType.resizeArray(numDeps[ALL_POS], timeIndex + 1);

					// init new fields with 0
					for (int ii=oldLength; ii < numDeps[ALL_POS].length; ii++) {

						numDeps[ALL_POS][ii] = 0;

					}

					System.out.println("new length of " + actType + ": " + numDeps[actIndex].length);

				}

				numDeps[ALL_POS][timeIndex]++;

			}
//			System.out.println(act.getType());

		}
	}

	private void analyseArrivals(Person person) {

		Plan plan = person.getPlans().get(0);

		Iterator actIter = plan.getIteratorAct();
		while (actIter.hasNext()) {

			Act act = (Act) actIter.next();
			String actType = act.getType().substring(0, 1);
			double arrTime = act.getStartTime();
			if (arrTime != 0.0) {

				int actIndex = Activities.valueOf(actType).getPosition();

				int timeIndex = ((int) arrTime) / timeBinSize;
				int oldLength = numArrs[actIndex].length;

//				System.out.println("time index: " + timeIndex);
//				System.out.println("old length: " + oldLength);

				if (timeIndex >= oldLength) {

					numArrs[actIndex] = (int[]) PersonAnalyseTimesByActivityType.resizeArray(numArrs[actIndex], timeIndex + 1);

					// init new fields with 0
					for (int ii=oldLength; ii < numArrs[actIndex].length; ii++) {

						numArrs[actIndex][ii] = 0;

					}

					System.out.println("new length of " + actType + ": " + numArrs[actIndex].length);

				}

				numArrs[actIndex][timeIndex]++;

				// resize summary array too
				if (numArrs[actIndex].length > numArrs[ALL_POS].length) {

					oldLength = numArrs[ALL_POS].length;
					numArrs[ALL_POS] = (int[]) PersonAnalyseTimesByActivityType.resizeArray(numArrs[ALL_POS], timeIndex + 1);

					// init new fields with 0
					for (int ii=oldLength; ii < numArrs[ALL_POS].length; ii++) {

						numArrs[ALL_POS][ii] = 0;

					}

					System.out.println("new length of " + actType + ": " + numArrs[actIndex].length);

				}

				numArrs[ALL_POS][timeIndex]++;

			}
//			System.out.println(act.getType());

		}

	}

	private void analyseTraveling(Person person) {

		Plan plan = person.getPlans().get(0);

		double depTime = -1.0, arrTime = -1.0;
		int actIndex = -1;
		String actType = null;
		int oldLength;

		for (Object o : plan.getActsLegs()) {

			if (o.getClass().equals(Act.class)) {

				if (depTime != -1.0) {
					actType = ((Act) o).getType().substring(0, 1);
//					System.out.println(actType);
					actIndex = Activities.valueOf(actType).getPosition();
					
					// write trip into time bins
					int startTimeBinIndex = ((int) depTime) / this.timeBinSize;
					int endTimeBinIndex = ((int) arrTime) / this.timeBinSize;
//					System.out.println(startTimeBinIndex);
//					System.out.println(endTimeBinIndex);
					if (this.numTraveling[actIndex].length <= endTimeBinIndex) {

						oldLength = numTraveling[actIndex].length;
						numTraveling[actIndex] = (int[]) PersonAnalyseTimesByActivityType.resizeArray(numTraveling[actIndex], endTimeBinIndex + 1);

						// init new fields with 0
						for (int ii=oldLength; ii < numTraveling[actIndex].length; ii++) {
							numTraveling[actIndex][ii] = 0;
						}

						System.out.println("new length of " + actType + ": " + numTraveling[actIndex].length);
					}
					
					// resize summary array too
					if (numTraveling[actIndex].length > numTraveling[ALL_POS].length) {

						oldLength = numTraveling[ALL_POS].length;
						numTraveling[ALL_POS] = (int[]) PersonAnalyseTimesByActivityType.resizeArray(numTraveling[ALL_POS], endTimeBinIndex + 1);

						// init new fields with 0
						for (int ii=oldLength; ii < numTraveling[ALL_POS].length; ii++) {
							numTraveling[ALL_POS][ii] = 0;
						}

						System.out.println("new length of all: " + numTraveling[ALL_POS].length);

					}

					for (int ii = startTimeBinIndex; ii <= endTimeBinIndex; ii++) {
						this.numTraveling[actIndex][ii]++;
						this.numTraveling[this.ALL_POS][ii]++;
					}
					
				}

//				System.out.println();
				
			} else if (o.getClass().equals(Leg.class)) {

				depTime = ((Leg) o).getDepTime();
				arrTime = ((Leg) o).getArrTime();

			}
		}
	}

	private static Object resizeArray (Object oldArray, int newSize) {
		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class elementType = oldArray.getClass().getComponentType();
		Object newArray = java.lang.reflect.Array.newInstance(
				elementType,newSize);
		int preserveLength = Math.min(oldSize,newSize);
		if (preserveLength > 0)
			System.arraycopy (oldArray,0,newArray,0,preserveLength);
		return newArray; 
	}

	public int[][] getNumArrs() {
		return numArrs;
	}

	public int[][] getNumTraveling() {
		return numTraveling;
	}

}