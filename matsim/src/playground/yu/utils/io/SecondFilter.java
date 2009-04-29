/* *********************************************************************** *
 * project: org.matsim.*
 * SecondFilter.java
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

/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yu
 * 
 */
public class SecondFilter extends MyFilter {

	public SecondFilter(String regex, String attTableFilename,
			String outputFilename) throws IOException {
		super(regex, attTableFilename, outputFilename);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String timeIntervalFileName = "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\DepTimeIndex22test.txt";
		String attFilePath = "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\Att22R24\\";
		String outputFilePath = "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\Att22R24\\output222\\";
		// to read time intervall file:
		TimeIntervalReader tir = readTimeInterval(timeIntervalFileName,
				attFilePath, outputFilePath);
		Set<PrimLine> pls = new HashSet<PrimLine>();
		// to read input-.att-files and config min- and max departure time:
		for (int i = 0; i <= tir.getCnt(); i++) {
			pls.clear();
			String attTableFilename, outputFilename;
			Date minDepTime, maxDepTime;
			attTableFilename = tir.getInputFilename(i);
			try {
				minDepTime = SDF.parse(tir.getMinDepTime(i));
				maxDepTime = SDF.parse(tir.getMaxDepTime(i));

				outputFilename = tir.getOutputFilename(i);

				SecondFilter sf = new SecondFilter(";", attTableFilename,
						outputFilename);
				String line;
				// to search headline of the table
				do {
					line = sf.readLine();
					if (PutpathlegFilter.isHead(line))
						break;
					sf.writeln(line);
				} while (line != null);
				sf.writeNewLine(line);// writes "$P...."
				// to read line of table
				// ...... PrimLine pl=null;
				do {
					line = sf.readLine();
					if (line != null)
						if (!line.startsWith(";")) {// the line is the first
							// line of an OD-zone
							String[] primLines = sf.split(line);
							if (primLines.length > 1) {
								Date depDate = SDF.parse(primLines[12]);
								if ((minDepTime.before(depDate) || minDepTime
										.equals(depDate))
										&& (depDate.before(maxDepTime) || depDate
												.equals(maxDepTime))) {

									PrimLine pl = new PrimLine(primLines[0],
											primLines[1], primLines[8],
											primLines[14]);
									if (pls.isEmpty()) {
										pls.add(pl);
										sf.writeNewLine(line);
									} else {
										boolean sameODpair = true;
										for (PrimLine plSaved : pls)
											if (!plSaved.sameODpair(pl)) {
												sameODpair = false;
												break;
											}
										if (sameODpair) {
											boolean samePl = false;
											for (PrimLine plSaved : pls)
												if (plSaved.samePrimLine(pl)) {
													samePl = true;
													break;
												}
											if (!samePl) {
												pls.add(pl);
												sf.writeNewLine(line);
											}
										} else {
											pls.clear();
											pls.add(pl);
											sf.writeNewLine(line);
										}
									}
								}
							}
						}
				} while (line != null);
				sf.closeReader();
				sf.closeWriter();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
}
