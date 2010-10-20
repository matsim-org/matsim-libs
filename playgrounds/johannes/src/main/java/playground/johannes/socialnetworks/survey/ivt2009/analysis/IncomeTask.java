/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeTask.java
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.SocioMatrixTask;

/**
 * @author illenberger
 *
 */
public class IncomeTask extends SocioMatrixTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		SocialGraph graph = (SocialGraph) g;
		Income income = new Income();
		double[][] matrix = income.socioMatrix(graph);
		List<String> values = income.getAttributes();
		
		double[][] matrixAvr = income.socioMatrixLocalAvr(graph);
		
		try {
			writeSocioMatrix(matrix, values, getOutputDirectory() + "/income.matrix.txt");
			writeSocioMatrix(matrixAvr, values, getOutputDirectory() + "/income.matrix.norm2.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
