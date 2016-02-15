/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package herbie.creation.freight;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CSShapeFileWriter {
	private final static Logger log = Logger.getLogger(CSShapeFileWriter.class);	
					
	public void writeODRelations(String outdir, List<ODRelation> relations) {
		log.info("Writing freight OD relations");			
		ArrayList<SimpleFeature> features =  (ArrayList<SimpleFeature>) this.generateRelations(relations);	
		log.info("Created " + features.size() + " features");
		if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries(features, outdir + "/heaviestODRelations.shp");
		}
	}
	
	public Collection<SimpleFeature> generateRelations(List<ODRelation> relations) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder().
				setCrs(crs).setName("rel").addAttribute("ID", Double.class).create();
		for (ODRelation relation : relations) {
			SimpleFeature ft;
			ft = factory.createPolyline(
					new Coordinate [] {MGC.coord2Coordinate(relation.getOrigin()), MGC.coord2Coordinate(relation.getDestination())},
					new Object[] {relation.getWeight()}, null);
			features.add(ft);
		}
		return features;
	}
}
