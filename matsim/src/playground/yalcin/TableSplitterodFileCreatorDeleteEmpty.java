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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class TableSplitterodFileCreatorDeleteEmpty {
	private final String regex;
	private final BufferedReader reader;
	/**
	 * @param arg0
	 *            code
	 * @param arg1
	 *            matrix-counter
	 */
	private final Map<String, Integer> odMatrixs = new HashMap<String, Integer>();

	public TableSplitterodFileCreatorDeleteEmpty(final String regex, final String tableFileName)
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

	public int getMatrixCnt(final String code) {
		return odMatrixs.get(code).intValue();
	}

	public void countMatrix(final String code) {
		Integer cnt = odMatrixs.get(code);
		odMatrixs.put(code, cnt != null ? cnt.intValue() + 1 : 0);
	}

	private void writeMatrixHead(final BufferedWriter writer) {
		try {
			writer.write("$ON;D3;Y5\n" + "*  \n"
					+ "*Orig      Dest         Number  \n"
					+ "*Zone       Zone         Trips \n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeMatrix(final String matrixName, final String matrix) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(matrixName);
			writeMatrixHead(writer);
			writer.write(matrix);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) throws IOException {
		TableSplitterodFileCreatorDeleteEmpty ts = new TableSplitterodFileCreatorDeleteEmpty("\t", "C:/Users/yalcin/Desktop/Zurich/Marcel_code/new/odtextnew.txt");
		String outputFileName = "C:/Users/yalcin/Desktop/Zurich/Marcel_code/new/odtextnew_output2.txt";
		String outputPath = "C:/Users/yalcin/Desktop/Zurich/Marcel_code/new/ODTables1/";
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
		String code = null;
		do {
			readedLine = ts.readLine();
			if (readedLine != null)
				line = ts.split(readedLine);
			StringBuilder od = null;
			if (!line[0].equals("")) {
				if (starts.size() > 0 && ends.size() > 0) {
					od = new StringBuilder();
					for (int j = 0; j < starts.size(); j++)
						for (int k = 0; k < ends.size(); k++) {
							String start = starts.get(j);
							String end = ends.get(k);
							if (!start.equals(end)) {
								for (int i = 0; i <= 7; i++)
									writer.write("\t");
								writer.write(start + "\t" + end + "\n");
								od.append(start + "\t" + end + "\t1\n");
							}
						}
					if (od.length() > 0)
						ts.writeMatrix(outputPath + "odmatrix" + code + " ("
								+ ts.getMatrixCnt(code) + ").mtx", od
								.toString());
				}
				starts.clear();
				ends.clear();
				for (int i = 0; i <= 5; i++)
					writer.write(line[i] + "\t");
				code = line[0];
				ts.countMatrix(code);
				writer.write(line[8] + "\t" + line[9] + "\t" + line[15] + "\t"
						+ line[17] + "\n");
			} else if (!line[15].equals(""))
				starts.add(line[15]);
			else if (!line[17].equals(""))
				ends.add(line[17]);
		} while (readedLine != null);
		ts.closeReader();
		writer.close();
		System.out.println("done.");
	}
}
