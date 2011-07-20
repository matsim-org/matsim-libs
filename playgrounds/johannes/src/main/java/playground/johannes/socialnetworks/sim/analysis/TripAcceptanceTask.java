/* *********************************************************************** *
 * project: org.matsim.*
 * TripAcceptanceTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleIntHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class TripAcceptanceTask extends TrajectoryAnalyzerTask {
	
	private static final Logger logger = Logger.getLogger(TripAcceptanceTask.class);

	private ActivityFacilities facilities;
	
//	private static final String TYPE = "loutdoor";
	
	private DistanceCalculator distCalc = new CartesianDistanceCalculator();
	
//	private Discretizer discretizer = new LinearDiscretizer(1000.0);
	
	public TripAcceptanceTask(ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	@Override
	public void analyze(Set<Trajectory> plans, Map<String, DescriptiveStatistics> results) {
		GeometryFactory factory = new GeometryFactory();
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(4326), CRSUtils.getCRS(21781));
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Set<Point> opportunities = new HashSet<Point>();
		
		
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			for(ActivityOption opt : facility.getActivityOptions().values()) {
				if(opt.getType().equals("culture") || opt.getType().equals("sports")) {
					Coord coord = facility.getCoord();
					opportunities.add(factory.createPoint(new Coordinate(coord.getX(), coord.getY())));
					break;
				}
			}
		}
		
		DescriptivePiStatistics stats = new WSMStatsFactory().newInstance();
		
		logger.info("Calculating trip acceptance...");
		ProgressLogger.init(plans.size(), 1, 5);
		
		for(Trajectory plan : plans) {
			for(int i = 2; i < plan.getElements().size(); i+=2) {
				Activity act = (Activity) plan.getElements().get(i);
				
				if(act.getType().equals("lindoor") || act.getType().equals("loutdoor")) {
					Activity prev = (Activity) plan.getElements().get(i - 2);
					Point p1 = factory.createPoint(new Coordinate(prev.getCoord().getX(), prev.getCoord().getY()));
					Point p2 = factory.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
					
					p1 = CRSUtils.transformPoint(p1, transform);
					p2 = CRSUtils.transformPoint(p2, transform);
					
					TDoubleIntHashMap M_i = new TDoubleIntHashMap();

					TDoubleArrayList distances = new TDoubleArrayList();
					for(Point point : opportunities) {
						double d = distCalc.distance(p1, point);
						distances.add(d);
					}

					Discretizer discr1 = FixedSampleSizeDiscretizer.create(distances.toNativeArray(), 50, 300);
					for(int j = 0; j < distances.size(); j++) {
						double d = distances.get(j);
						d = discr1.discretize(d);
						M_i.adjustOrPutValue(d, 1, 1);
					}
					
					double d = distCalc.distance(p1, p2);
//					d = discr1.discretize(d);
					
					int M = M_i.get(discr1.discretize(d));
					if(M > 0) {
						stats.addValue(d, 1/(double)M);
					}
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		logger.info(String.format("Sample size = %1$s.", stats.getN()));
		try {
			writeHistograms(stats, "p_trip_leisure", 20, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}



}
