/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.noise;

import org.matsim.contrib.analysis.vsp.qgis.QGisWriter;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;
import org.matsim.contrib.analysis.vsp.qgis.layerTemplates.NoiseRenderer;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


/**
 * @author ikaddoura
 *
 */
public class CreateQGisProjectFile {

public static void main(String args[]){
		
		String time = "16:00:00";
		String workingDirectory =  "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise_averageVSmarginal/output/baseCase/noise_analysis_1_actualSpeed/analysis_it.100/immissions/";
		String qGisProjectFile = "immission" + time + ".qgs";
		
		QGisWriter writer = new QGisWriter(TransformationFactory.DHDN_GK4, workingDirectory);
			
// ################################################################################################################################################
		double[] extent = {4568808,5803042,4622772,5844280};
		writer.setExtent(extent);
				
		VectorLayer noiseLayer = new VectorLayer("noise", workingDirectory + "immission_57600.0.csv", QGisConstants.geometryType.Point, true);
		noiseLayer.setDelimiter(";");
		noiseLayer.setXField("x");
		noiseLayer.setYField("y");
		
		NoiseRenderer renderer = new NoiseRenderer(noiseLayer, 100.);
		renderer.setRenderingAttribute("Immission " + time);
		
		writer.addLayer(noiseLayer);
		
// ################################################################################################################################################
		
		writer.write(qGisProjectFile);
//			
//		if ( System.getProperty("os.name").contains("Mac") || System.getProperty("os.name").contains("mac") ) {
//			String cmd = "/Applications/QGIS.app/Contents/MacOS/QGIS " + workingDirectory + qGisProjectFile +
//					" --snapshot " + workingDirectory + qGisProjectFile + "_snapshot.png";
//			
//			int timeout = 99999;
//			String stdoutFileName = workingDirectory + qGisProjectFile + "_snapshot.log";
//			ExeRunner.run(cmd, stdoutFileName, timeout);
//			
//		} else {
//			// ...
//		}

	}
	
}
