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

package playground.johannes.gsv.synPop.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.math.LinearDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

/**
 * @author johannes
 *
 */
public class ActivityDistanceTask implements ProxyAnalyzerTask {

	private GeometryFactory geoFactory = new GeometryFactory();
	
	private final ActivityFacilities facilities;
	
	private final String outputDir;
	
	private final DistanceCalculator calc = CartesianDistanceCalculator.getInstance();
	
	public ActivityDistanceTask(ActivityFacilities facilities, String outputDir) {
		this.facilities = facilities;
		this.outputDir = outputDir;
	}
	
	@Override
	public void analyze(Collection<ProxyPerson> persons) {
		DescriptiveStatistics stats = statistics(persons, "work");
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, new LinearDiscretizer(1000), false);
		try {
			TXTWriter.writeMap(hist, "d", "n", outputDir + "d.work.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private DescriptiveStatistics statistics(Collection<ProxyPerson> persons, String purpose) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for(ProxyPerson person : persons) {
			ProxyPlan plan = person.getPlan();
			
			double x = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_X));
			double y = Double.parseDouble((String) person.getAttribute(CommonKeys.PERSON_HOME_COORD_Y));
				
			Point p = geoFactory.createPoint(new Coordinate(x, y));
			
			for(ProxyObject act : plan.getActivities()) {
				if(purpose == null || purpose.equalsIgnoreCase(act.getAttribute(CommonKeys.ACTIVITY_TYPE))) {
					String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
					ActivityFacility facilitiy = facilities.getFacilities().get(new IdImpl(id));
					double d = calc.distance(p, MatsimCoordUtils.coordToPoint(facilitiy.getCoord()));
					stats.addValue(d);
				}
			}
		}
		
		return stats;
	}
}
