/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbability.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis.deprecated;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleDoubleHashMap;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.math.Distribution;

import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 * 
 */
public class AcceptanceProbabilityEdgeType {

	private Discretizer discretizer = new LinearDiscretizer(1000);

	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();

	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.distanceCalculator = calculator;
	}

	public Distribution distribution(Set<? extends SpatialVertex> vertices, Set<Point> choiceSet, String edgeType) {
//			int srid1 = vertices.iterator().next().getPoint().getSRID();
//		int srid2 = choiceSet.iterator().next().getSRID();
//
//		if (srid1 != srid2)
//			throw new RuntimeException(
//					"Vertices and points of choice set do not have the same coordinate reference system.");

		Distribution distribution = new Distribution();
		Set<SpatialEdge> touched = new HashSet<SpatialEdge>();

			for (SpatialVertex vertex : vertices) {
			Point p1 = vertex.getPoint();
//			if (p1.getSRID() != srid1)
//				throw new RuntimeException("Vertices do not have the same coordinate reference system.");

				TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
			for (Point p2 : choiceSet) {

//				if (p2.getSRID() != srid2)
//					throw new RuntimeException("Points do not have the same coordinate reference system.");

					if (p1 != null && p2 != null) {
					double d = distanceCalculator.distance(p1, p2);
					n_d.adjustOrPutValue(discretizer.discretize(d), 1, 1);
				}
			}

			for (int i = 0; i < vertex.getEdges().size(); i++) {
				SpatialEdge e = vertex.getEdges().get(i);
				if (touched.add(e) && edgeType.equalsIgnoreCase(((SocialEdge)e).getType())) {
					SpatialVertex neighbor = e.getOpposite(vertex);

//					if (neighbor.getPoint().getSRID() != srid1)
//						throw new RuntimeException("Vertices do not have the same coordinate reference system.");

					if (p1 != null && neighbor.getPoint() != null) {
						double d = distanceCalculator.distance(p1, neighbor.getPoint());

						double n = n_d.get(discretizer.discretize(d));
						if (n > 0)
							distribution.add(d, 1 / n);
					}
				}
			}
		}
		return distribution;
	}
}
