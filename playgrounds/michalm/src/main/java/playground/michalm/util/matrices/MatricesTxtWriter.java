/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.util.matrices;

import java.io.*;
import java.util.*;

import org.matsim.matrices.*;

import com.google.common.base.*;

public class MatricesTxtWriter {
	private final Map<String, Matrix> matrices;
	private String keyHeader = "key";
	private Function<String, String> formatter = Functions.identity();

	public static MatricesTxtWriter createForSingleMatrix(Matrix matrix) {
		MatricesTxtWriter writer = new MatricesTxtWriter(Collections.singletonMap("", matrix));
		writer.setKeyHeader(null);
		return writer;
	}

	public MatricesTxtWriter(Map<String, Matrix> matrices) {
		this.matrices = matrices;
	}

	/**
	 * to skip displaying the key column, set keyHeader to null
	 * 
	 * @param keyHeader
	 */
	public void setKeyHeader(String keyHeader) {
		this.keyHeader = keyHeader;
	}

	public void setKeyFormatter(Function<String, String> formatter) {
		this.formatter = formatter;
	}

	public void write(String file) {
		try (PrintWriter pw = new PrintWriter(file)) {
			writeKey(pw, keyHeader);
			pw.println("from\tto\tvalue");

			for (Map.Entry<String, Matrix> mapEntry : matrices.entrySet()) {
				String key = keyHeader == null ? null : formatter.apply(mapEntry.getKey());

				for (Entry e : MatrixUtils.createEntryIterable(mapEntry.getValue())) {
					writeKey(pw, key);
					pw.printf("%s\t%s\t%f\n", e.getFromLocation(), e.getToLocation(), e.getValue());
				}
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeKey(PrintWriter pw, String key) {
		if (keyHeader != null) {
			pw.print(key + "\t");
		}
	}
}
