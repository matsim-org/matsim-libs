/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.complexNetworks.gtiLandUse;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.southafrica.freight.digicore.algorithms.djcluster.HullConverter;
import playground.southafrica.utilities.Header;

public class HullParser {
	private final static Logger LOG = Logger.getLogger(HullParser.class);

	public static void main(String[] args) {
		Header.printHeader(HullParser.class.toString(), args);
		String facilitiesFile = args[0];
		String facilityAttributeFile = args[1];
		String outputFilename = args[2];
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new FacilitiesReaderMatsimV1(sc).parse(facilitiesFile);
		
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);

		oar.putAttributeConverter(Point.class, new HullConverter() );
		oar.putAttributeConverter(LineString.class, new HullConverter() );
		oar.putAttributeConverter(Polygon.class, new HullConverter() );
		oar.parse(facilityAttributeFile);
		
		LOG.info("Number of facilities to convert: " + sc.getActivityFacilities().getFacilities().size());
		Counter counter = new Counter("   facilities # ");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFilename);
		
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		try{
			bw.write("Id,Long,Lat");
			bw.newLine();
			
			for(Id id : sc.getActivityFacilities().getFacilities().keySet()){
				Object o = oa.getAttribute(id.toString(), "concaveHull");
				if(o instanceof Geometry){
					Geometry g = (Geometry)o;
					for(Coordinate c : g.getCoordinates()){
						Coord saAlbers = new CoordImpl(c.x, c.y);
						Coord wgs84 = ct.transform(saAlbers);
						
						bw.write(String.format("%s,%.6f,%.6f\n", id.toString(), wgs84.getX(), wgs84.getY()));
					}
				} else{
					LOG.warn("Object not of type Geometry, but " + o.getClass());
				}
				counter.incCounter();
			}
			counter.printCounter();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFilename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFilename);
			}
		}
		
		
		
		Header.printFooter();
	}

}
