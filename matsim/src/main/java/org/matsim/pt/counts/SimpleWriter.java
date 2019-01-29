/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.pt.counts;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * a small and simple writer, in order to avoid copious "try" and "catch" for
 * "Exception"
 * 
 * @author yChen
 * 
 */
public class SimpleWriter implements Closeable, Flushable {

	private final BufferedWriter writer;

	public SimpleWriter(final String outputFilename) {
		writer = IOUtils.getBufferedWriter(outputFilename);
	}

	public void write(char[] c) {
		if (writer != null)
			try {
				writer.write(c);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
	}

	public void write(char c) {
		if (writer != null)
			try {
				writer.write(c);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
	}

	public void write(String s) {
		if (writer != null) {
			try {
				writer.write(s);
			} catch (IOException e) {
				System.err.println("writer was not initialized yet!");
				throw new UncheckedIOException(e);
			}
		}
	}

	public void write(Object o) {
		write(o.toString());
	}

	public void writeln(String s) {
		write(s + "\n");
	}

	public void writeln(Object o) {
		write(o + "\n");
	}

	public void writeln() {
		write('\n');
	}

	@Override
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void flush() {
		try {
			writer.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void writeln(StringBuffer line) {
		writeln(line.toString());
	}

}
