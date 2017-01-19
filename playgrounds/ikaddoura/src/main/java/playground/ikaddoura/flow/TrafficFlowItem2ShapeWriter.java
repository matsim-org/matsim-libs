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

package playground.ikaddoura.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

/**
* Writes traffic flow items to a shape file.
*  
* @author ikaddoura
*/

public class TrafficFlowItem2ShapeWriter {
	private static final Logger log = Logger.getLogger(TrafficFlowItem2ShapeWriter.class);

	private List<TrafficItem> trafficItems = null;
	
	public TrafficFlowItem2ShapeWriter(List<TrafficItem> trafficItems) {
		this.trafficItems = trafficItems;
	}
	
	public void writeTrafficItemsToShapeFile(String outputShpFile, String crs) {
							
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
				.setCrs(MGC.getCRS(crs))
				.setName("Item")
				
				.addAttribute("ID", String.class)
				.addAttribute("t_0", Double.class)
				.addAttribute("t_act", Double.class)
				.addAttribute("cn", Double.class)
				.addAttribute("download", String.class)
				.addAttribute("fc", Double.class)
				.addAttribute("jam", Double.class)
				.addAttribute("length", Double.class)
				
				.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (TrafficItem item : trafficItems) {		
				
			Object[] attributeValues = new Object[] {
					item.getId(),
					item.getFreespeed(),
					item.getActualSpeed(),
					item.getConfidence(),
					item.getDownloadTime(),
					item.getFc(),
					item.getJamFactor(),
					item.getLength()
			};
			
			SimpleFeature feature = factory.createPolyline(item.getCoordinates(), attributeValues, null);			
			features.add(feature);
		}
		
		if (features.isEmpty()) {
			log.warn("No traffic flow items. Nothing to write into a shape file.");
		} else {
			log.info("Writing out traffic flow items to shapefile... ");
			ShapeFileWriter.writeGeometries(features, outputShpFile);
			log.info("Writing out traffic flow items to shapefile... Done.");
		}
	}

}

