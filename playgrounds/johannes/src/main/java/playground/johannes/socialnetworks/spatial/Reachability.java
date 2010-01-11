/* *********************************************************************** *
 * project: org.matsim.*
 * Reachability.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.spatial;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.statistics.Correlations;
import playground.johannes.socialnetworks.statistics.Distribution;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class Reachability {

	public static ZoneLayerDouble createReachablityTable(TravelTimeMatrix matrix, ZoneLayerDouble densityLayer) {
		ZoneLayerDouble reachablityLayer = new ZoneLayerDouble(matrix.getZones());
		
		for(Zone z_i : matrix.getZones()) {
			double sum = 0;
			double total = 0;
			for(Zone z_j : matrix.getZones()) {
				double tt = matrix.getTravelTime(z_i, z_j);
				double inhabitants = densityLayer.getValue(z_j) * z_j.getBorder().getArea()/(1000*1000);
				sum += tt * inhabitants;
				total += inhabitants;
			}
			double val = sum/(double)total;
			reachablityLayer.setValue(z_i, val);
		}
		
		return reachablityLayer;
	}
	
	public static ZoneLayerDouble createDistanceReachabilityTable(ZoneLayerDouble densityLayer) {
		ZoneLayerDouble reachablityLayer = new ZoneLayerDouble(new HashSet<Zone>(densityLayer.getZones()));
		
		for(Zone z_i : densityLayer.getZones()) {
			Point p_i = z_i.getBorder().getCentroid();
			Coord c_i = new CoordImpl(p_i.getX(), p_i.getY());
			double sum = 0;
			double total = 0;
			for(Zone z_j : densityLayer.getZones()) {
				if(z_i != z_j) {
					Point p_j = z_j.getBorder().getCentroid();
					Coord c_j = new CoordImpl(p_j.getX(), p_j.getY());
					double d = CoordUtils.calcDistance(c_i, c_j);
					double inhabitants = densityLayer.getValue(z_j) * z_j.getBorder().getArea()/(1000*1000);
					sum += d * inhabitants;
					total += inhabitants;
				}
			}
			double val = sum/(double)total;
			reachablityLayer.setValue(z_i, val);
		}
		
		return reachablityLayer;
	}
	
	public static TDoubleDoubleHashMap degreeCorrelation(Set<? extends SpatialSparseVertex> vertices, ZoneLayerDouble reachablity) {
		TObjectDoubleHashMap<SpatialSparseVertex> values = new TObjectDoubleHashMap<SpatialSparseVertex>();
		
		for(SpatialSparseVertex v : vertices) {
			values.put(v, v.getNeighbours().size());
		}
		
		return correlation(values, reachablity);
	}
	
	public static TDoubleDoubleHashMap correlation(TObjectDoubleHashMap<? extends SpatialSparseVertex> values, ZoneLayerDouble reachablity) {
		double[] values1 = new double[values.size()];
		double[] values2 = new double[values.size()];
		
		TObjectDoubleIterator<? extends SpatialSparseVertex> it = values.iterator();
		for(int i = 0; i< values.size(); i++) {
			it.advance();
			double val = reachablity.getValue(it.key().getCoordinate());
			if(!Double.isNaN(val)) {
				values1[i] = val;
				values2[i] = it.value();
			}
		}
		return Correlations.correlationMean(values1, values2, 1);
	}
	
	public static <V extends SpatialSparseVertex> TObjectDoubleHashMap<V> distanceReachability(Set<V> vertices, ZoneLayerDouble reachability) {
		TObjectDoubleHashMap<V> values = new TObjectDoubleHashMap<V>();
		
//		FIXME!
		if(SpatialGraphStatistics.zoneCache == null) {
			SpatialGraphStatistics.precacheZones(vertices, reachability);
		}
		
		for(V v_i : vertices) {
			Zone z = SpatialGraphStatistics.zoneCache.get(v_i);
			if(z != null) {
			double r = reachability.getValue(z);//FIXME
//			double r = reachability.getValue(v_i.getCoordinate());
			if(!Double.isNaN(r)) {
				values.put(v_i, r);
			}
			}
		}
		
		return values;
	}
	
	public static TDoubleDoubleHashMap degreeDistanceReachabilityCorrelation(TObjectDoubleHashMap<? extends SpatialSparseVertex> values) {
		double[] values1 = new double[values.size()];
		double[] values2 = new double[values.size()];
		
		TObjectDoubleIterator<? extends SpatialSparseVertex> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			values1[i] = it.value();
			values2[i] = it.key().getNeighbours().size();
		}
		
		return Correlations.correlationMean(values1, values2,5000);
	}
	
	public static Distribution normalizedTravelTimeDistribution(Set<? extends SpatialSparseVertex> vertices, ZoneLayerDouble densityLayer, TravelTimeMatrix matrix) {
		double binsize = 300;
		Distribution distr = new Distribution();
		for (SpatialSparseVertex v : vertices) {
			Zone z_i = densityLayer.getZone(v.getCoordinate());
			if (z_i != null) {

				TIntDoubleHashMap areas = new TIntDoubleHashMap();
				TIntIntHashMap n_i = new TIntIntHashMap();
				for (Zone z_j : densityLayer.getZones()) {
					double tt = matrix.getTravelTime(z_i, z_j);
					double a = z_j.getBorder().getArea() / (1000 * 1000);
					int bin = (int)Math.ceil(tt/binsize);
					
					areas.adjustOrPutValue(bin, a, a);
					
					double rho = densityLayer.getValue(z_j);
					int n = (int)(a * rho);
					n_i.adjustOrPutValue(bin, n, n);
				}

				for (SpatialSparseVertex v2 : v.getNeighbours()) {
					Zone z_j = densityLayer.getZone(v2.getCoordinate());
					if (z_j != null) {
						double tt = matrix.getTravelTime(z_i, z_j);
						int bin = (int)Math.ceil(tt/binsize);
						double a = areas.get(bin);
						distr.add(tt, a / (double)n_i.get(bin));
					}
				}
			} 
		}
		
		return distr;
	}
}
