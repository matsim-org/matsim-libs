/* *********************************************************************** *
 * project: org.matsim.*
 * TabelSplitter.java
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

package playground.yalcin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

public class TableSplitter {
	private final String regex;
	private final BufferedReader reader;

	public TableSplitter(final String regex, final String tableFileName)
			throws IOException {
		this.regex = regex;
		reader = IOUtils.getBufferedReader(tableFileName);
	}

	public String[] split(final String line) {
		return line.split(regex);
	}

	public String readLine() throws IOException {
		return reader.readLine();
	}

	public void closeReader() throws IOException {
		reader.close();
	}

	public static void main(final String[] args) throws IOException {
		//boolean deleteSameOD=true;
		TableSplitter ts = new TableSplitter("\t", "C:/Users/yalcin/Desktop/Zurich/Marcel_code/new/odtextnew.txt");
		String outputFileName = "C:/Users/yalcin/Desktop/Zurich/Marcel_code/new/odtextnew_output.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileName);
		String[] head = ts.split(ts.readLine());
		for (int i = 0; i <= 5; i++)
			writer.write(head[i] + "\t");
		writer.write(head[8] + "\t" + head[9] + "\t" + head[15] + "\t"
				+ head[17] + "\n");
		String[] line = null;
		List<String> starts = new LinkedList<String>();
		List<String> ends = new LinkedList<String>();
		String readedLine = null;
		do {
			readedLine = ts.readLine();
			if (readedLine != null)
				line = ts.split(readedLine);
			if (!line[0].equals("")) {
				// System.out.println("starts size was " + starts.size());
				if (starts.size() > 0)
					for (int j = 0; j < starts.size(); j++) {
						System.out.println("ends size was " + ends.size());
						for (int k = 0; k < ends.size(); k++) {
							String start = starts.get(j);
							String end = ends.get(k);
							// System.out.println("start: " + start + "\tend: "
							// + end);
							if (!start.equals(end)) {
								for (int i = 0; i <= 7; i++)
									writer.write("\t");
								writer.write(start + "\t" + end + "\n");
							}
						}
					}
				starts.clear();
				// System.out.println("starts was cleared!");
				ends.clear();
				for (int i = 0; i <= 5; i++)
					writer.write(line[i] + "\t");
				writer.write(line[8] + "\t" + line[9] + "\t" + line[15] + "\t"
						+ line[17] + "\n");
			} else if (!line[15].equals(""))
				starts.add(line[15]);
			// System.out.println("starts added " + line[15]);
			else if (!line[17].equals("")) {
				ends.add(line[17]);
				System.out.println("ends added " + line[17]);
			}
		} while (readedLine != null);
		ts.closeReader();
		writer.close();
		System.out.println("done.");
	}
}
