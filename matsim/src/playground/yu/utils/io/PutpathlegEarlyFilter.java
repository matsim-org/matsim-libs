/* *********************************************************************** *
 * project: org.matsim.*
 * PutpathlegEarlyFilter.java
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yu
 * 
 */
public class PutpathlegEarlyFilter extends PutpathlegFilter {
	public static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

	private static class PutPathLeg {
		private final String origZoneNo, destZoneNo;
		private final Date depTime;
		private final String firstLine;
		private final List<String> followings = new ArrayList<String>();

		public PutPathLeg(final String origZoneNo, final String destZoneNo,
				final String depTime, final String firstLine)
				throws ParseException {
			this.origZoneNo = origZoneNo;
			this.destZoneNo = destZoneNo;
			this.depTime = SDF.parse(depTime);
			this.firstLine = firstLine;
		}

		public void addFollowingline(final String line) {
			followings.add(line);
		}

		public String getOrigZoneNo() {
			return origZoneNo;
		}

		public String getDestZoneNo() {
			return destZoneNo;
		}

		public Date getDepTime() {
			return depTime;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(firstLine);
			for (int i = 0; i < followings.size(); i++)
				sb.append("\n" + followings.get(i));
			return sb.toString();
		}
	}

	private final Map<String, PutPathLeg> putpathlegs = new HashMap<String, PutPathLeg>();

	public PutpathlegEarlyFilter(final String regex,
			final String tableFilename, final String minDepTime,
			final String maxDepTime, final String outputFilename)
			throws IOException, ParseException {
		super(regex, tableFilename, minDepTime, maxDepTime, outputFilename);
	}

	public void putPutpathleg(final PutPathLeg ppl) {
		putpathlegs.put(ppl.getOrigZoneNo() + ppl.getDestZoneNo(), ppl);
	}

	public PutPathLeg getPutpathleg(final String origDestZoneNo) {
		return putpathlegs.get(origDestZoneNo);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String inputFilename = "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\DepTimeIndex22.txt";
		String attFilePath = "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\Att22R24\\";
		String outputFilePath = "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\Att22R24\\output\\";
		IndexFileReader ifr = null;
		try {
			ifr = new IndexFileReader("\t", inputFilename, attFilePath,
					outputFilePath);
			String line = ifr.readLine();
			while (line != null) {
				line = ifr.readLine();
				ifr.makeParams(line);
			}
			ifr.closeReader();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (ifr != null)
			for (int i = 0; i <= ifr.getCnt(); i++) {

				String putpathlegTableFilename, minDepTime, maxDepTime, outputFilename;

				putpathlegTableFilename = ifr.getInputFilename(i);
				minDepTime = ifr.getMinDepTime(i);
				maxDepTime = ifr.getMaxDepTime(i);
				outputFilename = ifr.getOutputFilename(i);

				try {
					PutpathlegEarlyFilter pef = new PutpathlegEarlyFilter(";",
							putpathlegTableFilename, minDepTime, maxDepTime,
							outputFilename);
					String line;

					do {
						line = pef.readLine();
						if (PutpathlegFilter.isHead(line))
							break;
						pef.writeln(line);
					} while (line != null);

					pef.writeNewLine(line);// writes "$P...."
					PutPathLeg ppl = null;
					do {
						line = pef.readLine();
						boolean refreshedPpl = false;
						String origDestZoneNo = "";
						if (line != null)
							if (line.startsWith(";")) {// the line is not the
								// first line of an
								// OD-zone
								if (refreshedPpl)
									pef.getPutpathleg(origDestZoneNo)
											.addFollowingline(line);

								/* if (writeable) pf.writeNewLine(line); */
							} else {// the line is the first line of an OD-zone
								String[] firstLines = pef.split(line);
								if (firstLines.length > 1) {
									origDestZoneNo = firstLines[0]
											+ firstLines[1];
									ppl = pef.getPutpathleg(origDestZoneNo);// take
									// out
									// old
									// ppl
									String depTime = firstLines[firstLines.length - 3];
									if (ppl != null) {
										// TODO compare the depTime of old with
										// new ppl
										if (ppl.getDepTime().before(
												SDF.parse(depTime)))
											refreshedPpl = false;
										else {
											ppl = new PutPathLeg(firstLines[0],
													firstLines[1], depTime,
													line);
											pef.putPutpathleg(ppl);
											refreshedPpl = true;
										}
									} else {
										ppl = new PutPathLeg(firstLines[0],
												firstLines[1], depTime, line);
										pef.putPutpathleg(ppl);
										refreshedPpl = true;
									}
								}
							}
						/*
						 * if (pf .rightDepTime(firstLines[firstLines.length -
						 * 3])) { pf.writeNewLine(line); writeable = true; }
						 * else writeable = false;
						 */
					} while (line != null);

					pef.closeReader();

					for (PutPathLeg p : pef.putpathlegs.values())
						pef.writeNewLine(p.toString());

					pef.closeWriter();

				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}

			}
	}
}
