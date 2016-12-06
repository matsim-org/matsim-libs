/* *********************************************************************** *
 * project: org.matsim.*
 * Parcel.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.southafrica.population.census2011.capeTown.facilities;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to capture the land use data provided by the city of Cape Town.
 * 
 * @author jwjoubert
 */
public class Parcel implements BasicLocation<Parcel>  {
	final private Id<Parcel> id;
	final private Coord coord;
	final private Logger log = Logger.getLogger(Parcel.class);
	private final MultiPolygon polygon;
	private Map<Landuse, Double> landuses = new TreeMap<>();

	
	public Parcel(SimpleFeature feature) {
		/* Use the OBJECTID as the base for the Id. */
		this.id = Id.create("gdb_" + feature.getAttribute("OBJECTID").toString(), Parcel.class);

		Object g = feature.getDefaultGeometry();
		if(g != null && g instanceof MultiPolygon){
			this.polygon = (MultiPolygon)g;
			Point point = this.polygon.getCentroid();
			this.coord = CoordUtils.createCoord(point.getX(), point.getY());
			
			populateLanduse(feature);
		} else{
			this.polygon = null;
			this.coord = null;
			log.error("Parcel is not a MultiPolygon: OBJECTID " + this.id.toString());
			if(g == null){
				log.error("Default geometry is null.");
			} else{
				log.error("Parcel is of type " + g.getClass().toString());
			}
		}
	}

	
	public void populateLanduse(SimpleFeature feature){
		Double d;
		{d = parseArea(Landuse.AGRIC, feature); if(d != null){ landuses.put(Landuse.AGRIC, d); } }
		{d = parseArea(Landuse.BUS_GENERIC, feature); if(d != null){ landuses.put(Landuse.BUS_GENERIC, d); } }
		{d = parseArea(Landuse.BUS_OFFICE, feature); if(d != null){ landuses.put(Landuse.BUS_OFFICE, d); } }
		{d = parseArea(Landuse.BUS_RETAIL, feature); if(d != null){ landuses.put(Landuse.BUS_RETAIL, d); } }
		{d = parseArea(Landuse.CIV_GENERIC, feature); if(d != null){ landuses.put(Landuse.CIV_GENERIC, d); } }
		{d = parseArea(Landuse.CIV_HOSPITAL, feature); if(d != null){ landuses.put(Landuse.CIV_HOSPITAL, d); } }
		{d = parseArea(Landuse.CIV_POWER, feature); if(d != null){ landuses.put(Landuse.CIV_POWER, d); } }
		{d = parseArea(Landuse.CIV_PUBLIC_SERVICE, feature); if(d != null){ landuses.put(Landuse.CIV_PUBLIC_SERVICE, d); } }
		{d = parseArea(Landuse.IND_GENERIC, feature); if(d != null){ landuses.put(Landuse.IND_GENERIC, d); } }
		{d = parseArea(Landuse.IND_SRVICE, feature); if(d != null){ landuses.put(Landuse.IND_SRVICE, d); } }
		{d = parseArea(Landuse.IND_WAREHOUSE, feature); if(d != null){ landuses.put(Landuse.IND_WAREHOUSE, d); } }
		{d = parseArea(Landuse.INST_GENERIC, feature); if(d != null){ landuses.put(Landuse.INST_GENERIC, d); } }
		{d = parseArea(Landuse.INST_POI, feature); if(d != null){ landuses.put(Landuse.INST_POI, d); } }
		{d = parseArea(Landuse.MINING, feature); if(d != null){ landuses.put(Landuse.MINING, d); } }
		{d = parseArea(Landuse.MIXED_USE, feature); if(d != null){ landuses.put(Landuse.MIXED_USE, d); } }
		{d = parseArea(Landuse.NR_GENERIC, feature); if(d != null){ landuses.put(Landuse.NR_GENERIC, d); } }
		{d = parseArea(Landuse.OS, feature); if(d != null){ landuses.put(Landuse.OS, d); } }
		{d = parseArea(Landuse.OTHER, feature); if(d != null){ landuses.put(Landuse.OTHER, d); } }
		{d = parseArea(Landuse.PARKING, feature); if(d != null){ landuses.put(Landuse.PARKING, d); } }
		{d = parseArea(Landuse.RES_GENERIC, feature); if(d != null){ landuses.put(Landuse.RES_GENERIC, d); } }
		{d = parseArea(Landuse.RES_MULTI, feature); if(d != null){ landuses.put(Landuse.RES_MULTI, d); } }
		{d = parseArea(Landuse.RES_SINGLE, feature); if(d != null){ landuses.put(Landuse.RES_SINGLE, d); } }
		{d = parseArea(Landuse.SPECIAL, feature); if(d != null){ landuses.put(Landuse.SPECIAL, d); } }
		{d = parseArea(Landuse.TRANSPORT, feature); if(d != null){ landuses.put(Landuse.TRANSPORT, d); } }
		{d = parseArea(Landuse.UTILITY, feature); if(d != null){ landuses.put(Landuse.UTILITY, d); } }
	}
	
	
	public Double parseArea(Landuse landuse, SimpleFeature feature){
		Double d = null;
		
		Object attr = feature.getAttribute(landuse.getAttributeName());
		if(attr != null){
			if(attr instanceof Double){
				double area = (Double)attr;
				d = area > 0 ? area : null;
			}
		}
		
		return d;
	}
	
	
	public int getNumberOfLanduses(){
		return this.landuses.size();
	}
	
	
	public Map<Landuse, Double> getLanduses(){
		return this.landuses;
	}
	
	
	public enum Landuse {
		AGRIC("AGRIC"),
		BUS_GENERIC("BUS_GENRC"),
		BUS_OFFICE("BUS_OFFICE"),
		BUS_RETAIL("BUS_RETAIL"),
		CIV_GENERIC("CIV_GENRC"),
		CIV_HOSPITAL("CIV_HOSP"),
		CIV_POWER("CIV_POW"),
		CIV_PUBLIC_SERVICE("CIV_PUBSRV"),
		IND_GENERIC("IND_GEN"),
		IND_SRVICE("IND_SRVC"),
		IND_WAREHOUSE("IND_WHS"),
		INST_GENERIC("INST_GENRC"),
		INST_POI("INST_POI"),
		MINING("MINING"),
		MIXED_USE("MIXED_USE"),
		NR_GENERIC("NR_GENRC"),
		OS("OS"),
		OTHER("OTHER"),
		PARKING("PRKG"),
		RES_GENERIC("RES_GENRC"),
		RES_MULTI("RES_MULTI"),
		RES_SINGLE("RES_SNGL"),
		SPECIAL("SPECIAL"),
		TRANSPORT("TRSPRT"),
		UTILITY("UTILITY");
		
		private final String attr;
		
		private Landuse(String attr) {
			this.attr = attr;
		}
		
		public String getAttributeName(){
			return this.attr;
		}
	}


	@Override
	public Id<Parcel> getId() {
		return this.id;
	}


	@Override
	public Coord getCoord() {
		return this.coord;
	}
	
}
