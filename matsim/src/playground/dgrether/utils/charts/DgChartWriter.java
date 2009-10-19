/* *********************************************************************** *
 * project: org.matsim.*
 * ChartWriter
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
package playground.dgrether.utils.charts;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;


/**
 * @author dgrether
 *
 */
public class DgChartWriter {
	
	private static final Logger log = Logger.getLogger(DgChartWriter.class);

	public static void writerChartToFile(String filename, JFreeChart jchart) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), jchart, 800, 600, null, true, 9);
			log.info("DeltaScoreIncomeChart written to : " +filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
