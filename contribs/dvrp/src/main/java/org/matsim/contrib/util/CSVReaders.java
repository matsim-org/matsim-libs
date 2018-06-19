/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util;

import java.io.IOException;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class CSVReaders {
	public static List<String[]> readTSV(String file) {
		return readFile(file, '\t');
	}

	public static List<String[]> readCSV(String file) {
		return readFile(file, CSVParser.DEFAULT_SEPARATOR);
	}

	public static List<String[]> readSemicolonSV(String file) {
		return readFile(file, ';');
	}

	public static List<String[]> readFile(String file, char separator) {
		try (CSVReader reader = new CSVReaderBuilder(IOUtils.getBufferedReader(file))
				.withCSVParser(new CSVParserBuilder().withSeparator(separator).build()).build()) {
			return reader.readAll();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
