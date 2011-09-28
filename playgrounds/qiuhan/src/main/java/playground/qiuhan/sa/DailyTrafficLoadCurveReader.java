/* *********************************************************************** *
 * project: org.matsim.*
 * DailyTrafficLoadCurveReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.qiuhan.sa;

import playground.yu.utils.io.SimpleReader;

/**
 * @author Qiuhan
 * 
 */
public class DailyTrafficLoadCurveReader {
	private DailyTrafficLoadCurve dailytrafficloadcurve = null;

	public DailyTrafficLoadCurveReader(
			final DailyTrafficLoadCurve dailytrafficloadcurve) {
		this.dailytrafficloadcurve = dailytrafficloadcurve;
	}

	public void readFile(final String filename) {
		SimpleReader reader = new SimpleReader(filename);
		String line = reader.readLine();
		while (line != null) {
			line = reader.readLine();
			if (line != null) {
				String[] strs = line.split("\t");
				int time = Integer.parseInt(strs[0]);
				double share = Double.parseDouble(strs[1]);
				this.dailytrafficloadcurve.addTrafficShare(time, share);
			}
		}

		reader.close();
	}

	public static void main(String[] args) {
		String curveFilename = "C:/Users/Chen/Documents/Studien/QiuhanSA/dummyTagesganglinie.txt";
		String chartFilename = "C:/Users/Chen/Documents/Studien/QiuhanSA/output/dummyTagesganglinie.png";

		DailyTrafficLoadCurve curve = new DailyTrafficLoadCurve();
		DailyTrafficLoadCurveReader reader = new DailyTrafficLoadCurveReader(
				curve);

		reader.readFile(curveFilename);

		curve.writeTrafficLoadCurveChart(chartFilename);
	}
}
