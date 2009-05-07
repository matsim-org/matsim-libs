/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialStatistics.java
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

/**
 * 
 */
package playground.johannes.socialnet.spatial;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.population.Person;

import playground.johannes.graph.GraphStatistics;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.io.SNGraphMLReader;
import playground.johannes.socialnets.DoubleStringSerializer;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class SpatialStatistics {

	public static TDoubleDoubleHashMap getDensityDegreeCorrelation(Collection<Ego<?>> vertices, SpatialGrid<Double> densityGrid) {
		TObjectDoubleHashMap<Ego<?>> vertexValues = new TObjectDoubleHashMap<Ego<?>>();
		for(Ego<?> e : vertices) {
			vertexValues.put(e, e.getEdges().size());
		}
		
		return getDensityCorrelation(vertexValues, densityGrid, 500);
	}
	
	public static TDoubleDoubleHashMap getDensityClusteringCorrelation(Collection<Ego<?>> vertices, SpatialGrid<Double> densityGrid) {
		return getDensityCorrelation((TObjectDoubleHashMap<Ego<?>>) GraphStatistics.getLocalClusteringCoefficients(vertices), densityGrid, 500);
	}
	
	public static TDoubleObjectHashMap<TDoubleArrayList> getDensityCorrelation(TObjectDoubleHashMap<Ego<?>> vertexValues, SpatialGrid<Double> densityGrid) {
		SpatialGrid<TDoubleArrayList> valuesGrid = new SpatialGrid<TDoubleArrayList>(densityGrid.getXmin(), densityGrid.getYmin(), densityGrid.getXmax(), densityGrid.getYmax(), densityGrid.getResolution());
		/*
		 * Create a grid where each cell contains an array with the values of the vertices within a cell.
		 */
		for(Object e : vertexValues.keys()) {
			Coord c = ((Ego<?>) e).getCoord();
			if(valuesGrid.isInBounds(c)) {
				TDoubleArrayList values = valuesGrid.getValue(c);
				if(values == null) {
					values = new TDoubleArrayList();
					valuesGrid.setValue(values, c);
				}
				values.add(vertexValues.get((Ego<?>) e));
			}
		}
		/*
		 * Create a map with a density-to-cell-values-mapping.
		 */
		TDoubleObjectHashMap<TDoubleArrayList> rho_values = new TDoubleObjectHashMap<TDoubleArrayList>();
		for(int row = 0; row < densityGrid.getNumRows(); row++) {
			for(int col = 0; col < densityGrid.getNumCols(row); col++) {
				double rho = densityGrid.getValue(row, col);
				TDoubleArrayList values = rho_values.get(rho);
				if(values == null) {
					values = new TDoubleArrayList();
					rho_values.put(rho, values);
				}
				TDoubleArrayList cellValues = valuesGrid.getValue(row, col);
				if(cellValues != null) {
					for(int i = 0; i < cellValues.size(); i++)
						values.add(cellValues.get(i));
				}
			}
		}
		
		return rho_values;
	}
	
	public static TDoubleDoubleHashMap getDensityCorrelation(TObjectDoubleHashMap<Ego<?>> vertexValues, SpatialGrid<Double> densityGrid, double binsize) {
		TDoubleObjectHashMap<TDoubleArrayList> rho_values = getDensityCorrelation(vertexValues, densityGrid);
		/*
		 * Discretize into density bins.
		 */
		TDoubleObjectHashMap<TDoubleArrayList> rho_values_bin = new TDoubleObjectHashMap<TDoubleArrayList>();
		for(double rho : rho_values.keys()) {
			double rho_bin = Math.floor(rho/binsize) * binsize;
			TDoubleArrayList binValues = rho_values_bin.get(rho_bin);
			if(binValues == null) {
				binValues = new TDoubleArrayList();
				rho_values_bin.put(rho_bin, binValues);
			}
			TDoubleArrayList values = rho_values.get(rho);
			if(values != null) {
				for(int i = 0; i < values.size(); i++)
					binValues.add(values.get(i));
			}
		}
		/*
		 * Calculate mean values.
		 */
		TDoubleDoubleHashMap rho_value_mean = new TDoubleDoubleHashMap();
		for(double rho : rho_values_bin.keys()) {
			double sum = 0;
			TDoubleArrayList k_list = rho_values_bin.get(rho);
			for(int i = 0; i < k_list.size(); i++)
				sum += k_list.get(i);
			rho_value_mean.put(rho, sum/(double)k_list.size());
		}
		
		return rho_value_mean;
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		SocialNetwork<Person> socialnet = SNGraphMLReader.loadFromConfig(args[0], args[1]);
		SpatialGrid<Double> densityGrid = SpatialGrid.readFromFile(args[2], new DoubleStringSerializer());
		
//		Set<Ego<Person>> anonymous = SNGraphMLReader.readAnonymousVertices(socialnet, args[3]); 
		Set<Ego<?>> vertices = new HashSet<Ego<?>>();
		for(Ego<Person> e : socialnet.getVertices()) {
//			if(!anonymous.contains(e))
				vertices.add(e);
		}
		TDoubleDoubleHashMap densityDegreeHist = getDensityDegreeCorrelation(vertices, densityGrid);
		WeightedStatistics.writeHistogram(densityDegreeHist, args[4]);
		TDoubleDoubleHashMap densityClusteringHist = getDensityClusteringCorrelation(vertices, densityGrid);
		WeightedStatistics.writeHistogram(densityClusteringHist, args[5]);
	}
}
