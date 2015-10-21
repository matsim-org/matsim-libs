/*
 * Cadyts - Calibration of dynamic traffic simulations
 *
 * Copyright 2009, 2010 Gunnar Fl�tter�d
 * 
 *
 * This file is part of Cadyts.
 *
 * Cadyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cadyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@epfl.ch
 *
 */
package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MakeHeaders {

	private static final boolean test = false;

	private static List<String> readFile(final String dir, final String startKey)
			throws IOException {
		final List<String> result = new ArrayList<String>();
		final BufferedReader reader = new BufferedReader(new FileReader(dir));
		boolean foundStart = (startKey == null);
		String line;
		while ((line = reader.readLine()) != null) {
			foundStart = (foundStart || startKey == null || line.trim()
					.startsWith(startKey));
			if (foundStart) {
				result.add(line);
			}
		}
		return result;
	}

	private static void processDir(final String dirName, List<String> header)
			throws IOException {
		final File dir = new File(dirName);
		if (!dir.isDirectory()) {
			if (dir.getName().endsWith(".svn")) {
				System.out.println("found " + dir.getName());
			} else if (dir.getName().endsWith(".java")) {
				final List<String> croppedContent = readFile(dirName, "package");
				if (croppedContent.size() == 0) {
					System.err.println("skipped " + dir
							+ " because of missing package keyword");
				} else {
					final PrintWriter writer;
					if (test) {
						writer = null;
					} else {
						writer = new PrintWriter(new File(dirName));
					}
					for (String line : header) {
						if (test) {
							System.out.println(line);
						} else {
							writer.println(line);
						}
					}
					for (String line : croppedContent) {
						if (test) {
							System.out.println(line);
						} else {
							writer.println(line);
						}
					}
					if (!test) {
						writer.flush();
						writer.close();
					}
				}
			}
		}
	}

	private static void traverse(final String dirName, List<String> header)
			throws IOException {
		processDir(dirName, header);
		final File dir = new File(dirName);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				traverse(new File(dir, children[i]).getAbsolutePath(), header);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED");

		System.out.println("opdyts...");
		traverse("src/main/java/floetteroed/opdyts",
				readFile("class-header_opdyts.txt", null));

		// System.out.println("utilities...");
		// traverse("src/main/java/floetteroed/utilities",
		// readFile("class-header_utilities.txt", null));

		System.out.println("DONE");
	}
}
