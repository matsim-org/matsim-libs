/* *********************************************************************** *
 * project: org.matsim.*
 * SocioMatrixTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;

/**
 * @author illenberger
 *
 */
public abstract class SocioMatrixTask extends AnalyzerTask {

	protected void writeDistribution(TObjectIntHashMap<String> distr, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		TObjectIntIterator<String> it = distr.iterator();
		writer.write("attr\tcount");
		writer.newLine();
		for(int i = 0; i < distr.size(); i++) {
			it.advance();
			writer.write(it.key());
			writer.write("\t");
			writer.write(String.valueOf(it.value()));
			writer.newLine();
		}
		writer.close();
	}
	
	protected void writeSocioMatrix(double[][] matrix, List<String> values, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for(String value : values) {
			writer.write("\t");
			writer.write(value);
		}
		writer.newLine();
		
		for(int i = 0; i < matrix.length; i++) {
			writer.write(values.get(i));
			
			for(int j = 0; j < matrix.length; j++) {
				writer.write("\t");
				writer.write(String.format(Locale.US, "%1$.2f", matrix[i][j]));
				
			}
			writer.newLine();
		}
		
		writer.close();
	}
}
