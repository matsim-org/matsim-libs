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

import java.io.*;

import org.matsim.core.utils.io.UncheckedIOException;

import com.opencsv.CSVWriter;

public class CompactCSVWriter extends CSVWriter {
	public static final String[] EMPTY_LINE = {};
	public static final String EMPTY_CELL = null;

	public CompactCSVWriter(Writer writer) {
		this(writer, '\t');
	}

	public CompactCSVWriter(Writer writer, char separator) {
		super(writer, separator, CSVWriter.NO_QUOTE_CHARACTER);
	}

	@Override
	public void writeNext(String... nextLine) {
		super.writeNext(nextLine);
	}

	public void writeNext(String first, String[] tail) {
		writeHeadAndTail(tail, first);
	}

	public void writeNext(String first, String second, String[] tail) {
		writeHeadAndTail(tail, first, second);
	}

	public void writeNext(String first, String second, String third, String[] tail) {
		writeHeadAndTail(tail, first, second, third);
	}

	public void writeNext(String first, String second, String third, String fourth, String[] tail) {
		writeHeadAndTail(tail, first, second, third, fourth);
	}

	private void writeHeadAndTail(String[] tail, String... head) {
		String[] nextLine = new String[head.length + tail.length];
		System.arraycopy(head, 0, nextLine, 0, head.length);
		System.arraycopy(tail, 0, nextLine, head.length, tail.length);
		writeNext(nextLine);
	}

	public void writeNextEmpty() {
		writeNext(EMPTY_LINE);
	}

	public void writeNext(CSVLineBuilder lineBuilder) {
		writeNext(lineBuilder.build());
	}

	@Override
	public void flush() {
		try {
			super.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() {
		try {
			super.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
