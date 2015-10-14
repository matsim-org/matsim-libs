/* *********************************************************************** *
 * project: org.matsim.*
 * TripAcceptanceProba.java
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
package playground.johannes.studies.mz2005;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.pysical.Trajectory;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.mz2005.io.ActivityType;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class TripAcceptanceProba extends TrajectoryAnalyzerTask {

	private final Quadtree cultureLocs;

	private final Quadtree gastroLocs;

	private final ActivityFacilities facilities;

	private final DistanceCalculator distCalc;

	private final Discretizer discretizer;

	public TripAcceptanceProba(ActivityFacilities facilities, DistanceCalculator distCalc) {
		this.distCalc = distCalc;
		this.facilities = facilities;
		
//		MathTransform transform = null;
//		try {
//			transform = CRS.findMathTransform(CRSUtils.getCRS(21781), DefaultGeographicCRS.WGS84);
//		} catch (FactoryException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		List<ActivityFacility> list = filterFacilities(facilities, ActivityType.culture.name());
		cultureLocs = new Quadtree();
		for(ActivityFacility f : list) {
			Point p = MatsimCoordUtils.coordToPoint(f.getCoord());
//			p = CRSUtils.transformPoint(p, transform);
			cultureLocs.insert(p.getEnvelopeInternal(), p);
		}
		
		list = filterFacilities(facilities, ActivityType.gastro.name());
		gastroLocs = new Quadtree();
		for(ActivityFacility f : list) {
			Point p = MatsimCoordUtils.coordToPoint(f.getCoord());
//			p = CRSUtils.transformPoint(p, transform);
			gastroLocs.insert(p.getEnvelopeInternal(), p);
		}
		
		discretizer = new LinearDiscretizer(1000.0);
	}
	
	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		DescriptivePiStatistics cultureStats = new DescriptivePiStatistics();
		DescriptivePiStatistics gastroStats = new DescriptivePiStatistics();
		
		ProgressLogger.init(trajectories.size(), 1, 5);
		for (Trajectory t : trajectories) {
			for (int i = 1; i < t.getElements().size(); i += 2) {
				Activity prev = (Activity) t.getElements().get(i - 1);
				Activity next = (Activity) t.getElements().get(i + 1);

				if (next.getType().equals(ActivityType.culture.name())
						|| next.getType().equals(ActivityType.gastro.name())) {
					Coord source = facilities.getFacilities().get(prev.getFacilityId()).getCoord();
					Coord target = facilities.getFacilities().get(next.getFacilityId()).getCoord();

					Point sourcePoint = MatsimCoordUtils.coordToPoint(source);
					double d = distCalc.distance(MatsimCoordUtils.coordToPoint(target),	sourcePoint);
					double r = discretizer.discretize(d);

					Envelope env = new Envelope(source.getX() - r, source.getX() + r, source.getY() - r, source.getY() + r);
					
					Quadtree quadtree;
					DescriptivePiStatistics stats;
					if(next.getType().equals(ActivityType.culture.name())) {
						quadtree = cultureLocs;
						stats = cultureStats;
					} else {
						quadtree = gastroLocs;
						stats = gastroStats;
					}
					
					List<Point> result = quadtree.query(env);
					
					int n = 0;
					for(Point p : result) {
						double d2 = distCalc.distance(sourcePoint, p);
						d2 = discretizer.discretize(d2);
						
						if(d2 == r) {
							n++;
						}
					}
					
					stats.addValue(d, 1/(double)n);
				}
			}
			ProgressLogger.step();
		}
		
		try {
			writeHistograms(cultureStats, "p_acc.culture", 50, 1);
			writeHistograms(gastroStats, "p_acc.gastro", 50, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<ActivityFacility> filterFacilities(ActivityFacilities facilities, String type) {
		List<ActivityFacility> facList = new ArrayList<ActivityFacility>(facilities.getFacilities().size());
		
		for(Entry<Id<ActivityFacility>, ? extends ActivityFacility> entry : facilities.getFacilities().entrySet()) {
			ActivityFacility facility = entry.getValue();
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(type == null || option.getType().equals(type)) {
					facList.add(facility);
					break;
				}
			}
		}
		
		return facList;
	}
}
