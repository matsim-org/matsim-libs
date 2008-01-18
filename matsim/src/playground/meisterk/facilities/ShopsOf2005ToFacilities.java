/* *********************************************************************** *
 * project: org.matsim.*
 * ProcessPickPayOpenTimes.java
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

package playground.meisterk.facilities;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class ShopsOf2005ToFacilities {

	private static String inputFilename = "/Volumes/share-baug-ivt-vpl-$/Public/exchange.temp/meister/scan_shops/pickpay_opentimes.txt";

	private static void readPickPayOpenTimes() {

		// TODO Auto-generated method stub
		boolean nextLineIsACloseLine = false;

		List<String> lines = null;
		String[] tokens = null;

		final String OPEN = "Auf";
		final String CLOSE = "Zu";

		String openLinePattern = "\t" + OPEN + "\t";
		String closeLinePattern = "\t" + CLOSE + "\t";
		String anythingButDigits = "[^0-9]";

		try {
			lines = FileUtils.readLines(new File(inputFilename), "UTF-8");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String line : lines) {

			if (line.contains(openLinePattern)) {

				tokens = line.split(anythingButDigits);
				System.out.println(tokens[0]);
				System.out.print("Auf:\t");
				for (String token : tokens) {
					if (!token.equals("") && !token.equals(tokens[0])) {
						System.out.print(token + "\t");
					}
				}
				System.out.println();

			} else if (line.contains(closeLinePattern)) {

				tokens = line.split(anythingButDigits);
				System.out.print("Zu:\t");
				for (String token : tokens) {
					if (!token.equals("")) {
						System.out.print(token + "\t");
					}
				}
				System.out.println();

			}

		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ShopsOf2005ToFacilities.readPickPayOpenTimes();
		
	}

}
