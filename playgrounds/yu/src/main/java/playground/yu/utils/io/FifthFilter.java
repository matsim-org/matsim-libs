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
public class FifthFilter extends MyFilter {

	public FifthFilter(String regex, String attTableFilename,
			String outputFilename) throws IOException {
		super(regex, attTableFilename, outputFilename);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// String timeIntervalFileName =
		// "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\DepTimeIndex22test.txt";
		// String attFilePath =
		// "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\Att22R24\\";
		// String outputFilePath =
		// "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\Att22R24\\output222\\";
		String timeIntervalFileName = "test/yu/test/yalcin/DepTimeIndex22.txt";
		String attFilePath = "test/yu/test/yalcin/";
		String outputFilePath = "test/yu/test/yalcin/output/";
		// to read time intervall file:
		TimeIntervalReader tir = readTimeInterval(timeIntervalFileName,
				attFilePath, outputFilePath);
		Set<PrimLine> pls = new HashSet<PrimLine>();
		// to read input-.att-files and config min- and max departure time:
		for (int i = 0; i < tir.getCnt(); i++) {
			pls.clear();
			String attTableFilename, outputFilename;
			Date minDepTime, maxDepTime;
			attTableFilename = tir.getInputFilename(i);
			System.out.println("attTabelFilename :\t" + attTableFilename);
			try {
				minDepTime = SDF.parse(tir.getMinDepTime(i));
				maxDepTime = SDF.parse(tir.getMaxDepTime(i));

				outputFilename = tir.getOutputFilename(i);

				FifthFilter ff = new FifthFilter(";", attTableFilename,
						outputFilename);
				String line;
				// to search headline of the table
				do {
					line = ff.readLine();
					if (PutpathlegFilter.isHead(line))
						break;
					ff.writeln(line);
				} while (line != null);
				ff.writeNewLine(line);// writes "$P...."
				// to read line of table
				// ...... PrimLine pl=null;
				boolean compareSecoLines = false;
				PrimLine currentPl = null;
				Set<PrimLine> referencePls = new HashSet<PrimLine>();
				String currentLine = null;
				do {

					line = ff.readLine();
					if (line != null)
						if (!line.startsWith(";")) {// the line is the primary
							// one
							if (currentPl != null)
								if (!compareSecoLines)
									pls.add(currentPl);
								else if (!referencePls.isEmpty()) {
									boolean totalSame = false;
									for (PrimLine refPl : referencePls)
										if (currentPl.sameSecoLines(refPl)) {
											totalSame = true;
											break;
										}
									if (!totalSame) {
										pls.add(currentPl);
										ff.writeNewLine(currentLine);
										compareSecoLines = false;
										for (int n = 0; n < currentPl
												.getInitSecoLines().size(); n++)
											ff.writeNewLine(currentPl
													.getInitSecoLines().get(n));
									}
									referencePls.clear();
									currentLine = null;
									currentPl = null;
								}
							String[] primLines = ff.split(line);
							if (primLines.length > 1) {
								Date depDate = SDF.parse(primLines[12]);
								if ((minDepTime.before(depDate) || minDepTime
										.equals(depDate))
										&& (depDate.before(maxDepTime) || depDate
												.equals(maxDepTime))) {
									PrimLine pl = new PrimLine(primLines[0],
											primLines[1], primLines[8],
											primLines[14]);
									currentPl = pl;
									currentLine = line;
									if (pls.isEmpty()) {
										ff.writeNewLine(line);
										compareSecoLines = false;
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
													referencePls.add(plSaved);
												}
											if (!samePl) {
												ff.writeNewLine(line);
												compareSecoLines = false;
											} else
												// sign to compare secondary
												// lines, because the following
												// secondary lines are not read
												// yet.
												compareSecoLines = true;
											// Now the primary line can not be
											// saved in Set, it can only be
											// saved in a current primary line.
										} else {// different ODpairs
											pls.clear();
											ff.writeNewLine(line);
											compareSecoLines = false;
										}
									}
								} else
									compareSecoLines = true;
							}
						} else if (currentPl != null) {
							if (!compareSecoLines)
								ff.writeNewLine(line);
							currentPl.addSecoLine(line);
						}
				} while (line != null);
				ff.closeReader();
				ff.closeWriter();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
}
