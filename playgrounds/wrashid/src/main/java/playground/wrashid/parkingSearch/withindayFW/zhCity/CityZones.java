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

package playground.wrashid.parkingSearch.withindayFW.zhCity;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;


public class CityZones {

	private QuadTree<CityZone> zones;
	
	public CityZones(String fileName){
		
		
		Matrix matrix = GeneralLib.readStringMatrix(fileName,";");
		
		int gidIndex=matrix.getColumnIndex("gid");
		int nameIndex=matrix.getColumnIndex("NAME");
		int ppAntBlauIndex=matrix.getColumnIndex("pp_anteil_blau");
		int ppAntBewIndex=matrix.getColumnIndex("pp_anteil_bew");
		int ppGebTypIndex=matrix.getColumnIndex("pp_gebuehr_typ");
		int phGeb2hIndex=matrix.getColumnIndex("ph_gebuehr_2h");
		int xCoordIndex=matrix.getColumnIndex("x_coord_centre");
		int yCoordIndex=matrix.getColumnIndex("y_coord_centre");
		
		LinkedList<CityZone> zoneList=new LinkedList<CityZone>();
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			CityZone z=new CityZone();
			
			z.setId(matrix.getString(i, gidIndex));
			z.setName(matrix.getString(i, nameIndex));
			z.setPctBlueParking(matrix.getDouble(i,  ppAntBlauIndex));
			z.setPctNonFreeParking(matrix.getDouble(i, ppAntBewIndex));
			z.setZoneTariffType(matrix.getInteger(i, ppGebTypIndex));
			z.setParkingGarageFee2h(matrix.getDouble(i, phGeb2hIndex));
			z.setZoneCentreCoord(matrix.getDouble(i, xCoordIndex),matrix.getDouble(i, yCoordIndex));
			
			zoneList.add(z);
		}
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (CityZone zone : zoneList) {
			if (zone.getZoneCentreCoord().getX() < minx) { minx = zone.getZoneCentreCoord().getX(); }
			if (zone.getZoneCentreCoord().getY() < miny) { miny = zone.getZoneCentreCoord().getY(); }
			if (zone.getZoneCentreCoord().getX() > maxx) { maxx = zone.getZoneCentreCoord().getX(); }
			if (zone.getZoneCentreCoord().getY() > maxy) { maxy = zone.getZoneCentreCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		zones=new QuadTree<CityZone>(minx, miny, maxx, maxy);
		
		for (CityZone zone : zoneList) {
			zones.put(zone.getZoneCentreCoord().getX(), zone.getZoneCentreCoord().getY(), zone);
		}
	}

	public CityZone getClosestZone(Coord coord){
		return zones.get(coord.getX(), coord.getY());
	}
	
}
