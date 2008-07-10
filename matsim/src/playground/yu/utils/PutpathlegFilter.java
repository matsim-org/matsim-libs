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
package playground.yu.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class PutpathlegFilter extends TableSplitter {
	public static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	// //////////////////////////////////////////////////////////////////////////////
	/*
	 * // private static class PutPathLeg{ // private String
	 * origZoneNo,destZoneNo; // private Date depTime; // private String line; //
	 * private List<String> followings=new ArrayList<String>(); // public
	 * PutPathLeg(final String origZoneNo, final String destZoneNo, final String
	 * depTime, final String line) throws ParseException{ //
	 * this.origZoneNo=origZoneNo; // this.destZoneNo=destZoneNo; //
	 * this.depTime=sdf.parse(depTime); // this.line=line; // } // public void
	 * addFollowingline(String line){ // followings.add(line); // } // public
	 * String getOrigZoneNo() { // return origZoneNo; // } // public String
	 * getDestZoneNo() { // return destZoneNo; // } // public Date getDepTime() { //
	 * return depTime; // } // public String toString(){ // StringBuilder sb=new
	 * StringBuilder(line+"\n"); // for(int i=0;i<followings.size();i++){ //
	 * sb.append(followings.get(i)); // } // return sb.toString(); // } // }
	 */

	private static class IndexFileReader extends TableSplitter {
		private final List<String> inputFilenames = new ArrayList<String>();
		private final List<String> minDepTimes = new ArrayList<String>();
		private final List<String> maxDepTimes = new ArrayList<String>();

		public IndexFileReader(final String regex, final String tableFileName)
				throws IOException {
			super(regex, tableFileName);
		}

		public void makeParams(final String line) {
			String[] params = split(line);
			inputFilenames.add(params[0]);
			minDepTimes.add(params[1]);
			maxDepTimes.add(params[2]);
		}

		public String getInputFilename(final int i) {
			return inputFilenames.get(i);
		}

		public String getMinDepTime(final int i) {
			return minDepTimes.get(i);
		}

		public String getMaxDepTime(final int i) {
			return maxDepTimes.get(i);
		}
	}

	// /////////////////////////////////////////////////////////////////////////////////////
	private final Date minDepTime, maxDepTime;
	private final BufferedWriter writer;

	// private Map<String, PutPathLeg> putpathlegs=new HashMap<String,
	// PutPathLeg>();
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
		super(regex, tableFilename);
		this.minDepTime = sdf.parse(minDepTime);
		this.maxDepTime = sdf.parse(maxDepTime);
		writer = IOUtils.getBufferedWriter(outputFilename);
	}

	public void writeLine(final String line) {
		try {
			writer.write(line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static boolean isHead(final String line) {
		return line.startsWith("$P");
	}

	protected boolean rightDepTime(final String depTime) throws ParseException {
		Date depDate = sdf.parse(depTime);
		return (minDepTime.before(depDate) || minDepTime.equals(depDate))
				&& (depDate.before(maxDepTime) || depDate.equals(maxDepTime));
	}

	public void run(final int i) {

	}

	/*
	 * // public void putPutpathleg(PutPathLeg ppl){ //
	 * putpathlegs.put(ppl.getOrigZoneNo()+ppl.getDestZoneNo(), ppl); // } //
	 * public PutPathLeg getPutpathleg(String origDestZoneNo){ // return
	 * putpathlegs.get(origDestZoneNo); // }
	 */
	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String inputFilename = "";
		IndexFileReader ifr = null;
		try {
			ifr = new IndexFileReader("\t", inputFilename);
			String line = ifr.readLine();
			while (line != null) {
				line = ifr.readLine();
				ifr.makeParams(line);
			}
			ifr.closeReader();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		for (int i = 0; i <= 1000; i++) {

			String putpathlegTableFilename, minDepTime, maxDepTime, outputFilename;
			if (ifr != null) {
				putpathlegTableFilename = ifr.getInputFilename(i);
				minDepTime = ifr.getMinDepTime(i);
				maxDepTime = ifr.getMaxDepTime(i);
				outputFilename = putpathlegTableFilename.split(".")[0]
						+ "_output.att";

				try {
					PutpathlegFilter pf = new PutpathlegFilter(";",
							putpathlegTableFilename, minDepTime, maxDepTime,
							outputFilename);
					String line;

					do {
						line = pf.readLine();
						pf.writeLine(line);
						if (PutpathlegFilter.isHead(line))
							break;
					} while (line != null);

					boolean writeable = false;
					do {

						line = pf.readLine();
						if (line != null)
							if (line.startsWith(";")) {
								if (writeable)
									pf.writeLine(line);
							} else {
								String[] firstLines = pf.split(line);
								if (firstLines.length == 13)
									if (pf.rightDepTime(firstLines[11])) {
										pf.writeLine(line);
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
