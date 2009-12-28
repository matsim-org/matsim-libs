/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.analysis.gis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author dgrether
 *
 */
public class PlanScoreAnalysisTableWriter {

	/**
	 * the seperator used
	 */
	private static final String SEPERATOR = "\t";

	/**
	 * newline
	 */
	private static final String NEWLINE = "\n";

	/**
	 * the column headers of the table
	 */
	private static final String[] COLUMNHEADERS = { "PersonId", "xcoord",
			"ycoord", "score-6", "score-3" };

	private BufferedWriter writer;

	public PlanScoreAnalysisTableWriter(String outfiletxt)
			throws FileNotFoundException, IOException {
		this.writer = IOUtils.getBufferedWriter(outfiletxt);
		for (int i = 0; i < COLUMNHEADERS.length; i++) {
			this.writer.write(COLUMNHEADERS[i]);
			this.writer.write(SEPERATOR);
		}
		this.writer.write(NEWLINE);
	}

	public void addLine(String id, String xcoord, String ycoord, String score6,
			String score3) throws IOException {
//		System.out.println("Writing: " + id + " " + xcoord);
		this.writer.write(id);
		this.writer.write(SEPERATOR);
		this.writer.write(xcoord);
		this.writer.write(SEPERATOR);
		this.writer.write(ycoord);
		this.writer.write(SEPERATOR);
		this.writer.write(score6);
		this.writer.write(SEPERATOR);
		this.writer.write(score3);
		this.writer.write(NEWLINE);
	}

	public void close() throws IOException {
		this.writer.flush();
		this.writer.close();
	}

}
