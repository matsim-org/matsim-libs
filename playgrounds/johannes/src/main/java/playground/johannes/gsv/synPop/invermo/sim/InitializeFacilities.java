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
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.sim.Initializer;
import playground.johannes.gsv.synPop.sim3.SwitchHomeLocation;
import playground.johannes.sna.gis.CRSUtils;

/**
 * @author johannes
 * 
 */
public class InitializeFacilities implements Initializer {

	private final FacilityData facilities;

	private MathTransform transform;

	public InitializeFacilities(FacilityData facilities) {
		this.facilities = facilities;
		try {
			transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init(ProxyPerson person) {
		ProxyPlan plan = person.getPlans().get(0);

		for (ProxyObject act : plan.getActivities()) {
			String str = act.getAttribute("coord");
			ActivityFacility fac = null;
			String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
			if(type == null) {
				type = "misc";
			}
			
			if (str == null) {
				fac = facilities.randomFacility(type);
			} else {
				Coord coord = string2Coord(str);
				fac = facilities.getClosest(coord, type);
				
			}
			act.setAttribute(CommonKeys.ACTIVITY_FACILITY, fac.getId().toString());
//			act.setUserData(MutateStartLocation.FACILITY_KEY, fac);
		}
		
		String str = person.getAttribute("homeCoord");
		ActivityFacility fac;
		if(str != null) {
			Coord coord = string2Coord(str);
			fac = facilities.getClosest(coord, "home");
			
		} else {
			fac = facilities.randomFacility("home");
		}
		person.setUserData(SwitchHomeLocation.USER_FACILITY_KEY, fac);

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

		return new CoordImpl(points[0], points[1]);
	}
}
