/* *********************************************************************** *
 * project: org.matsim.*
 * BkAnalysisWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;

/**
 * @author benjamin
 *
 */
public class BkAnalysisWriter {
	private String outputPath;

	public BkAnalysisWriter(String outputPath) {
		this.outputPath = outputPath;
	}

	public void writeFile(SortedMap<Id, Row> map, String outputFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath + outputFile + ".txt")));
		bw.write("PersonId \t PersonalIncome \t IncomeRank \t Score1 \t Score2 \t ScoreDiff");
		bw.newLine();
		for(Row row : map.values()) {
			bw.write(row.getId() + "\t" + row.getPersonalIncome() + "\t" + row.getIncomeRank() + "\t" + row.getScore1() + "\t" + row.getScore2() + "\t" + row.getScoreDiff());
			bw.newLine();
		}
		bw.close();
	}
}
