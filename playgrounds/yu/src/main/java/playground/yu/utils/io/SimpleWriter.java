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

package playground.yu.utils.io;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/**
 * a small and simple writer
 * 
 * @author yu
 * 
 */
public class SimpleWriter implements Closeable, Flushable {
	private BufferedWriter writer = null;
	private static String intermission;

	public static void setIntermission(String intermission) {
		SimpleWriter.intermission = intermission;
	}

	public static void appendIntermission(StringBuffer stringBuffer) {
		stringBuffer.append(intermission);
	}

	public SimpleWriter(final String outputFilename) {
		writer = IOUtils.getBufferedWriter(outputFilename);
	}

	public SimpleWriter(String outputFilename, String contents2write) {
		writer = IOUtils.getBufferedWriter(outputFilename);
		write(contents2write);
		close();
	}

	public void write(char[] c) {
		if (writer != null) {
			try {
				writer.write(c);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void write(char c) {
		if (writer != null) {
			try {
				writer.write(c);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void write(String s) {
		if (writer != null) {
			try {
				writer.write(s);
			} catch (IOException e) {
				System.err.println("writer was not initialized yet!");
				e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	@Override
	public void flush() {
		try {
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeln(StringBuffer line) {
		writeln(line.toString());
	}

}
