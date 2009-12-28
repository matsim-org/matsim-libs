/* *********************************************************************** *
 * project: org.matsim.*
 * PutpathlegFilter.java
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
import java.util.List;

/**
 * @author yu
 * 
 */
public class PutpathlegFilter extends MyFilter {
	public static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

	static class IndexFileReader extends TableSplitter {
		private final List<String> inputFileIndexs = new ArrayList<String>();
		private final List<String> minDepTimes = new ArrayList<String>();
		private final List<String> maxDepTimes = new ArrayList<String>();
		private final String attFilepath, outputAttFilepath;
		private int cnt = 0;

		public IndexFileReader(final String regex, final String tableFileName,
				final String attFilepath, final String outputAttFilepath)
				throws IOException {
			super(regex, tableFileName);
			this.attFilepath = attFilepath;
			this.outputAttFilepath = outputAttFilepath;
		}

		public void makeParams(final String line) {
			if (line != null) {
				String[] params = split(line);
				inputFileIndexs.add(params[0]);
				minDepTimes.add(params[1]);
				maxDepTimes.add(params[2]);
				cnt++;
			}
		}

		public String getInputFilename(final int i) {
			return attFilepath + "MyFirstAttList22R24 ("
					+ inputFileIndexs.get(i) + ").att";
		}

		public String getOutputFilename(final int i) {
			return outputAttFilepath + "MyFirstAttList22R24 ("
					+ inputFileIndexs.get(i) + ").att";
		}

		public String getMinDepTime(final int i) {
			return minDepTimes.get(i);
		}

		public String getMaxDepTime(final int i) {
			return maxDepTimes.get(i);
		}

		/**
		 * @return the cnt
		 */
		public int getCnt() {
			return cnt;
		}
	}

	// /////////////////////////////////////////////////////////////////////////////////////
	private final Date minDepTime, maxDepTime;

	/**
	 * @param regex
	 * @param tableFilename
	 * @param depTime
	 *            according to time pattern: "HH:mm:ss"
	 * @param arrTime
	 *            according to time pattern: "HH:mm:ss"
	 * @throws IOException
	 * @throws ParseException
	 */
	public PutpathlegFilter(final String regex, final String tableFilename,
			final String minDepTime, final String maxDepTime,
			final String outputFilename) throws IOException, ParseException {
		super(regex, tableFilename, outputFilename);
		this.minDepTime = SDF.parse(minDepTime);
		this.maxDepTime = SDF.parse(maxDepTime);
	}

	protected static boolean isHead(final String line) {
		return line.startsWith("$P");
	}

	protected boolean rightDepTime(final String depTime) throws ParseException {
		Date depDate = SDF.parse(depTime);
		return (minDepTime.before(depDate) || minDepTime.equals(depDate))
				&& (depDate.before(maxDepTime) || depDate.equals(maxDepTime));
	}

	public void run(final int i) {

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

		for (int i = 0; i <= 9; i++) {

			String putpathlegTableFilename, minDepTime, maxDepTime, outputFilename;
			if (ifr != null) {
				putpathlegTableFilename = ifr.getInputFilename(i);
				minDepTime = ifr.getMinDepTime(i);
				maxDepTime = ifr.getMaxDepTime(i);
				outputFilename = ifr.getOutputFilename(i);

				try {
					PutpathlegFilter pf = new PutpathlegFilter(";",
							putpathlegTableFilename, minDepTime, maxDepTime,
							outputFilename);
					String line;

					do {
						line = pf.readLine();
						if (PutpathlegFilter.isHead(line))
							break;
						pf.writeln(line);
					} while (line != null);

					pf.writeNewLine(line);// writes "$P...."

					boolean writeable = false;

					do {
						line = pf.readLine();
						if (line != null)
							if (line.startsWith(";")) {// the line is not
								// firstLine
								if (writeable)
									pf.writeNewLine(line);
							} else {// the line is firstLine
								String[] firstLines = pf.split(line);
								if (firstLines.length > 1)
									if (pf
											.rightDepTime(firstLines[firstLines.length - 3])) {
										pf.writeNewLine(line);
										writeable = true;
									} else
										writeable = false;
							}
					} while (line != null);

					pf.closeReader();
					pf.closeWriter();

				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
