/* *********************************************************************** *
 * project: org.matsim.*
 * GeneralLogObject.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.lib.obj;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.list.Lists;

public class GeneralLogObject {

	private String outputFile;
	private boolean fileClosed = false;
	private HashMap<String, ArrayList<String>> logGroups = new HashMap<String, ArrayList<String>>();

	public GeneralLogObject(String outputFile) {
		this.outputFile = outputFile;
	}

	public ArrayList<String> createArrayListForNewLogGroup(String nameOfNewLogGroup) {
		ArrayList<String> arrayList = new ArrayList<String>();

		if (logGroups.containsKey(nameOfNewLogGroup)) {
			DebugLib.stopSystemAndReportInconsistency("log group '" + nameOfNewLogGroup + "' exists already.");
		}

		logGroups.put(nameOfNewLogGroup, arrayList);

		return arrayList;
	}

	public ArrayList<String> getLogArrayListOfLogGroup(String nameOfNewLogGroup) {
		ArrayList<String> arrayList = logGroups.get(nameOfNewLogGroup);

		if (arrayList == null) {
			DebugLib.stopSystemAndReportInconsistency("log group '" + nameOfNewLogGroup + "' does not exist.");
		}

		return arrayList;
	}

	public void writeFileAndCloseStream() {
		if (!fileClosed) {
			ArrayList<String> outputList = new ArrayList<String>();

			try {
				FileOutputStream fos = new FileOutputStream(outputFile);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos);

				for (String logGroupName : logGroups.keySet()) {

					ArrayList<String> arrayList = logGroups.get(logGroupName);

					outputList.add("=============================================");
					outputList.add("Log Group Name: " + logGroupName);
					outputList.add("=============================================");
					outputList.addAll(arrayList);
				}

				char[] charArray = Lists.getCharsOfAllArrayItemsWithNewLineCharacterInbetween(outputList);
				outputStreamWriter.write(charArray);

				outputStreamWriter.flush();
				outputStreamWriter.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		fileClosed = true;
	}

}
