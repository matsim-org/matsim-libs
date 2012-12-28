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

import java.awt.Polygon;

import org.matsim.api.core.v01.Coord;

public class HighStreetTariffZonesZHCity {

	Polygon zhCityCentre;
	Polygon oerlikonCentre;
	
	public HighStreetTariffZonesZHCity(){
		zhCityCentre=new Polygon();
		
		zhCityCentre.addPoint(681947, 248806);
		zhCityCentre.addPoint(682352, 249209);
		zhCityCentre.addPoint(683891, 247778);
		zhCityCentre.addPoint(683995, 247404);
		zhCityCentre.addPoint(683662, 246921);
		zhCityCentre.addPoint(683802, 246753);
		zhCityCentre.addPoint(684204, 246623);
		zhCityCentre.addPoint(683724, 246298);
		zhCityCentre.addPoint(682545, 246262);
		zhCityCentre.addPoint(682423, 246664);
		zhCityCentre.addPoint(682550, 246778);
		zhCityCentre.addPoint(681635, 247636);
		zhCityCentre.addPoint(681577, 247939);
		zhCityCentre.addPoint(681736, 248210);
		zhCityCentre.addPoint(681982, 248343);
		zhCityCentre.addPoint(682288, 248341);
		zhCityCentre.addPoint(682343, 248454);
		
		oerlikonCentre=new Polygon();
		
		oerlikonCentre.addPoint(683047, 251512);
		oerlikonCentre.addPoint(683608, 251887);
		oerlikonCentre.addPoint(683629, 251788);
		oerlikonCentre.addPoint(683764, 251651);
		oerlikonCentre.addPoint(683888, 251683);
		oerlikonCentre.addPoint(683868, 251549);
		oerlikonCentre.addPoint(683936, 251444);
		oerlikonCentre.addPoint(683950, 251331);
		oerlikonCentre.addPoint(683888, 251117);
		oerlikonCentre.addPoint(683773, 251138);
		
	}

	public boolean isInHighTariffZone(double x, double y){
		return zhCityCentre.contains(x, y) || oerlikonCentre.contains(x, y);
	}
	
	public boolean isInHighTariffZone(Coord coord){
		return zhCityCentre.contains(coord.getX(), coord.getY()) || oerlikonCentre.contains(coord.getX(), coord.getY());
	}
}
