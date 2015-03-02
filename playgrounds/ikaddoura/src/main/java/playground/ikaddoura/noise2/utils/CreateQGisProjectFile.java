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
package playground.ikaddoura.noise2.utils;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.QGisWriter;
import playground.dhosse.qgis.VectorLayer;
import playground.dhosse.qgis.layerTemplates.NoiseRenderer;

/**
 * @author ikaddoura
 *
 */
public class CreateQGisProjectFile {

public static void main(String args[]){
		
		String time = "16:00:00";
		String workingDirectory =  "/Users/ihab/Documents/workspace/runs-svn/berlin_internalization_noise/output/noise_int_1a/ITERS/it.100/immissions/";
		String qGisProjectFile = "immission" + time + ".qgs";
		
		QGisWriter writer = new QGisWriter(TransformationFactory.DHDN_GK4, workingDirectory);
			
// ################################################################################################################################################
		
		double[] extent = {4582770.625,5807267.875,4608784.375,5825459.125};
		writer.setExtent(extent);
				
		VectorLayer noiseLayer = new VectorLayer("noise", workingDirectory + "immission_merged.csv",
				QGisConstants.geometryType.Point);
		noiseLayer.setDelimiter(",");
		noiseLayer.setXField("xCoord");
		noiseLayer.setYField("yCoord");
		NoiseRenderer renderer = new NoiseRenderer();
		renderer.setRenderingAttribute("immission_" + time);
		noiseLayer.setRenderer(renderer);
		writer.addLayer(noiseLayer);
		
// ################################################################################################################################################
		
		writer.write(qGisProjectFile);

	}
	
}
