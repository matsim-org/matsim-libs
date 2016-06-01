/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.gis;

import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.CRS;
import org.matsim.contrib.common.gis.CRSUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneEsriShapeIO;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author johannes
 * 
 */
public class MergeZoneAttributes {

	/**
	 * @param args
	 * @throws IOException
	 * @throws FactoryException
	 */
	public static void main(String[] args) throws IOException, FactoryException {
		MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(4326), CRSUtils.getCRS(31467));
		ZoneCollection gsvZones = ZoneEsriShapeIO.read("/home/johannes/gsv/matrices/zones_zone.SHP");

		ZoneCollection deZones = ZoneEsriShapeIO.read("/home/johannes/gsv/gis/vg250-ew_3112.gk3.shape.ebenen/vg250-ew_ebenen/vg250_krs.shp");

		ZoneCollection newCollection = new ZoneCollection(null);

		for (Zone gsvZone : gsvZones.getZones()) {
			if (gsvZone.getAttribute("NUTS0_CODE").equalsIgnoreCase("DE")) {
				Point p = gsvZone.getGeometry().getCentroid();
				p = CRSUtils.transformPoint(p, transform);
				Zone zone = deZones.get(p.getCoordinate());

				Zone newZone = new Zone(gsvZone.getGeometry());
				
				newZone.setAttribute("nuts0_code", gsvZone.getAttribute("NUTS0_CODE"));
				newZone.setAttribute("nuts1_code", gsvZone.getAttribute("NUTS1_CODE"));
				newZone.setAttribute("nuts2_code", gsvZone.getAttribute("NUTS2_CODE"));
				newZone.setAttribute("nuts3_code", gsvZone.getAttribute("CODE"));

				newZone.setAttribute("nuts0_name", gsvZone.getAttribute("NUTS0_NAME"));
				newZone.setAttribute("nuts1_name", gsvZone.getAttribute("NUTS1_NAME"));
				newZone.setAttribute("nuts2_name", gsvZone.getAttribute("NUTS2_NAME"));
				newZone.setAttribute("nuts3_name", gsvZone.getAttribute("NUTS3_NAME"));

				newZone.setAttribute("gsvId", gsvZone.getAttribute("NO"));

				if(zone != null) {
//					double ewz = Double.parseDouble(zone.getAttribute("EWZ"));
					newZone.setAttribute("inhabitants", zone.getAttribute("EWZ"));
				} else {
					System.err.println("No inhabitants for zone " + newZone.getAttribute("nuts2_name"));
				}
				newCollection.add(newZone);
				
			}
		}

		String data = ZoneGeoJsonIO.toJson(newCollection.getZones());
		Files.write(Paths.get("/home/johannes/gsv/gis/de.nuts3.json"), data.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

	}

}
