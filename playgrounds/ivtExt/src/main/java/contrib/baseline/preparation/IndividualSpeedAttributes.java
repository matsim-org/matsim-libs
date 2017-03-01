/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package contrib.baseline.preparation;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Analysis of trip data base for speeds.
 *
 * @author boescpa
 */
public class IndividualSpeedAttributes {
	private final static String DELIMITER = ";";
	private final ISA_DataStructure dataStructure = new ISA_DataStructure();

	public static void main(final String[] args) {
		final String pathToDataFile = args[0];

		IndividualSpeedAttributes isa = new IndividualSpeedAttributes();
		long numberOfReadElements = isa.readData(pathToDataFile);
		System.out.println("Number of elements: " + numberOfReadElements);
		System.out.println("Average speed WALK: " + isa.dataStructure.getAverageSpeed("walk"));
		System.out.println("Average speed BIKE: " + isa.dataStructure.getAverageSpeed("bike"));
		System.out.println("Is complete: " + isa.dataStructure.isComplete()); // todo-boescpa Take care of emtpy categories!!!
	}


	private long readData(String pathToFile) {
		Counter counter = new Counter("  data point # ");
		BufferedReader reader = IOUtils.getBufferedReader(pathToFile);
		try {
			reader.readLine(); // header
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split(DELIMITER);
				String mode = lineElements[1].replace('"',' ').trim();
				double speed = Double.parseDouble(lineElements[2].replace(',', '.'));
				int age = Integer.parseInt(lineElements[3]);
				// 0 = <20, 1 = <30, 2 = <40, ..., 6 = <80, 7 = >=80
				if (age < 20) {
					age = 0;
				} else if (age < 30) {
					age = 1;
				} else if (age < 40) {
					age = 2;
				} else if (age < 50) {
					age = 3;
				} else if (age < 60) {
					age = 4;
				} else if (age < 70) {
					age = 5;
				} else if (age < 80) {
					age = 6;
				} else {
					age = 7;
				}
				int sex = Integer.parseInt(lineElements[4]); // 0 = m, 1 = f
				int employed = Integer.parseInt(lineElements[5]); //0 = unemployed, 1 = employed
				int area = Integer.parseInt(lineElements[6]);
				if (area == 1 || area == 4) {
					area = 0; // urban
				} else if (area == 2 || area == 3) {
					area = 1; // suburban
				} else {
					area = 2; // exurban
				}
				double weight = Double.parseDouble(lineElements[8].replace('"','0'));
				this.dataStructure.addEntry(mode, sex, employed, area, age, speed, weight);
				counter.incCounter();
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return counter.getCounter();
	}

	private class ISA_DataStructure {
		final String[] MODES = {"walk", "bike", "car"};

		//                mode     -> sex      -> employed  -> area      -> age    			-> speeds AND weight
		private final Map<String, Map<Integer, Map<Integer, Map<Integer, Map<Integer, List<Tuple<Double, Double>>>>>>> dataTree;

		ISA_DataStructure() {
			dataTree = new HashMap<>();
			for (String mode : MODES) {
				dataTree.put(mode, new HashMap<>());
				int sex = 0; // 0 = m, 1 = f
				while (sex < 2) {
					dataTree.get(mode).put(sex, new HashMap<>());
					int employed = 0; // 0 = unemployed, 1 = employed
					while (employed < 2) {
						dataTree.get(mode).get(sex).put(employed, new HashMap<>());
						int area = 0; // 0 = urban, 1 = suburban, 2 = exurban
						while (area < 3) {
							dataTree.get(mode).get(sex).get(employed).put(area, new HashMap<>());
							int age = 0; // 0 = <20, 1 = <30, 2 = <40, ..., 6 = <80, 7 = >=80
							while (age < 8) {
								dataTree.get(mode).get(sex).get(employed).get(area).put(age, new LinkedList<>());
								age++;
							}
							area++;
						}
						employed++;
					}
					sex++;
				}
			}
		}

		void addEntry(String mode, int sex, int employed, int area, int age, double speed, double weight) {
			dataTree.get(mode).get(sex).get(employed).get(area).get(age).add(new Tuple<>(speed, weight));
		}

		List<Tuple<Double, Double>> getSpeeds(String mode, int sex, int employed, int area, int age) {
			return dataTree.get(mode).get(sex).get(employed).get(area).get(age);
		}

		double getAverageSpeed(String mode) {
			if (dataTree.keySet().contains(mode)) {
				double totWeight = 0.;
				double totSpeed = 0.;
				for (int sex : dataTree.get(mode).keySet()) {
					for (int employed : dataTree.get(mode).get(sex).keySet()) {
						for (int area : dataTree.get(mode).get(sex).get(employed).keySet()) {
							for (int age : dataTree.get(mode).get(sex).get(employed).get(area).keySet()) {
								if (!dataTree.get(mode).get(sex).get(employed).get(area).get(age).isEmpty()) {
									for (Tuple<Double, Double> speed : dataTree.get(mode).get(sex).get(employed).get(area).get(age)) {
										totSpeed += speed.getFirst();
										totWeight += speed.getSecond();
									}
								}
							}
						}
					}
				}
				return totSpeed/totWeight;
			}
			return -1;
		}

		String isComplete() {
			String isComplete = "no\n";
			for (String mode : MODES) {
				int sex = 0; // 0 = m, 1 = f
				while (sex < 2) {
					int employed = 0; // 0 = unemployed, 1 = employed
					while (employed < 2) {
						int area = 0; // 0 = urban, 1 = suburban, 2 = exurban
						while (area < 3) {
							int age = 0; // 0 = <20, 1 = <30, 2 = <40, ..., 6 = <80, 7 = >=80
							while (age < 8) {
								if (dataTree.get(mode).get(sex).get(employed).get(area).get(age).isEmpty()) {
									isComplete = isComplete.concat(mode + "_" + sex + "_" + employed + "_" + area + "_" + age + "\n");
								}
								age++;
							}
							area++;
						}
						employed++;
					}
					sex++;
				}
			}
			if (isComplete.equals("no\n")) isComplete = "Data structure is complete.";
			return isComplete;
		}
	}

}
