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

package playground.johannes.gsv.synPop.invermo.sim;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.facilities.ActivityFacility;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.gsv.synPop.sim3.SwitchHomeLocation;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 * 
 */
public class AssignHomeFacilities implements PersonTask {

	private final FacilityData facilities;

	private MathTransform transform;

	public AssignHomeFacilities(DataPool dataPool) {
		facilities = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
		
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	private Coord string2Coord(String str) {
		String tokens[] = str.split(",");
		double x = Double.parseDouble(tokens[0]);
		double y = Double.parseDouble(tokens[1]);

		double[] points = new double[] { x, y };
		try {
			transform.transform(points, 0, points, 0, 1);
		} catch (TransformException e) {
			e.printStackTrace();
		}

		return new Coord(points[0], points[1]);
	}

	@Override
	public void apply(Person person1) {
		PlainPerson person = (PlainPerson)person1;
		String str = person.getAttribute("homeCoord");
		ActivityFacility fac;
		if(str != null) {
			Coord coord = string2Coord(str);
			fac = facilities.getClosest(coord, ActivityType.HOME);
			
		} else {
			fac = facilities.randomFacility(ActivityType.HOME);
		}
		person.setUserData(SwitchHomeLocation.USER_FACILITY_KEY, fac);
	}
}
