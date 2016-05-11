/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.hafas.hafasCreator;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Identifies the busiest day of the validity period of the schedule and returns all bitfeld-numbers for the
 * lines running on this day.
 *
 * @author boescpa
 */
public class BitfeldAnalyzer {
	protected static Logger log = Logger.getLogger(BitfeldAnalyzer.class);

	protected static Set<Integer> findBitfeldnumbersOfBusiestDay(String FPLAN, String BITFELD) {
		final Set<Integer> bitfeldNummern = new HashSet<>();
		final int posMaxFVals = find4DayBlockWithMostFVals(FPLAN, BITFELD);
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BITFELD), "latin1"));
			String newLine = readsLines.readLine();
			while (newLine != null) {
				/*Spalte Typ Bedeutung
				1−6 INT32 Bitfeldnummer
				8−103 CHAR Bitfeld (Binärkodierung der Tage, an welchen Fahrt, in Hexadezimalzahlen notiert.)*/
				int bitfeldnummer = Integer.parseInt(newLine.substring(0, 6));
				String bitfeld = newLine.substring(7, 103);
				/* As we assume that the posMaxFVals describes a 4-day block with either Monday-Tuesday-Wednesday-Thursday or
				Tuesday-Wednesday-Thursday-Friday and because we don't want a Monday to be the reference day, we select those
				lines which have the second bit on one. The second stands for the 4 in the hexadecimal calculation, else we
				want all hexadecimal values which include a 4, that is 4, 5, 6, 7, 12 (C), 13 (D), 14 (E) and 15 (F).*/
				int matches = (bitfeld.charAt(posMaxFVals) == '4')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == '5')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == '6')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == '7')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'C')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'D')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'E')? 1 : 0;
				matches += (bitfeld.charAt(posMaxFVals) == 'F')? 1 : 0;
				if (matches >= 1) {
					bitfeldNummern.add(bitfeldnummer);
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		bitfeldNummern.add(0);
		return bitfeldNummern;
	}

	/**
	 * Returns the 4-day bitfeld block that has the most F-values. The assumption is that this block is either a
	 * Monday-Tuesday-Wednesday-Thursday or a Tuesday-Wednesday-Thursday-Friday block because all other blocks have
	 * at least one Weekend-Day and therefore are less like to produce an F (an F means traveling at all four days).
	 *
	 * @param BITFELD
	 * @return
	 */
	private static int find4DayBlockWithMostFVals(String FPLAN, String BITFELD) {
		Map<Integer, Integer> departuresPerBitfeld = new HashMap<>();
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(FPLAN), "latin1"));
			String newLine = readsLines.readLine();
			int numberOfDepartures = 0;
			while (newLine != null) {
				if (newLine.charAt(0) == '*') {
					if (newLine.charAt(1) == 'Z') {
						try {
							numberOfDepartures = Integer.parseInt(newLine.substring(22, 25)) + 1;
						} catch (Exception e) {
							numberOfDepartures = 1;
						}
					}
					if (newLine.charAt(1) == 'A' && newLine.charAt(3) == 'V') {
						if (newLine.substring(22, 28).trim().length() > 0) {
							int bitfeldNumber = Integer.parseInt(newLine.substring(22, 28));
							int bitfeldValue = numberOfDepartures;
							if (departuresPerBitfeld.containsKey(bitfeldNumber)) {
								bitfeldValue += departuresPerBitfeld.get(bitfeldNumber);
							}
							departuresPerBitfeld.put(bitfeldNumber, bitfeldValue);
						}
					}
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int[] bitfeldStats = new int[96];
		try {
			BufferedReader readsLines = new BufferedReader(new InputStreamReader(new FileInputStream(BITFELD), "latin1"));
			String newLine = readsLines.readLine();
			while (newLine != null) {
				/*Spalte Typ Bedeutung
				1−6 INT32 Bitfeldnummer
				8−103 CHAR Bitfeld (Binärkodierung der Tage, an welchen Fahrt, in Hexadezimalzahlen notiert.)*/
				int bitFeldValue = 1;
				if (departuresPerBitfeld.containsKey(Integer.parseInt(newLine.substring(0, 6)))) {
					bitFeldValue = departuresPerBitfeld.get(Integer.parseInt(newLine.substring(0, 6)));
				}
				String bitfeld = newLine.substring(7, 103);
				for (int i = 0; i < bitfeld.length(); i++) {
					if (bitfeld.charAt(i) == 'F') {
						bitfeldStats[i] += bitFeldValue;
					}
				}
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int maxFNumber = 0;
		int posMaxFNumber = -1;
		for (int i = 0; i < bitfeldStats.length; i++) {
			if (bitfeldStats[i] > maxFNumber) {
				maxFNumber = bitfeldStats[i];
				posMaxFNumber = i;
			}
		}
		log.info("Selected HAFAS-4day-block: " + posMaxFNumber);
		return posMaxFNumber;
	}
}
