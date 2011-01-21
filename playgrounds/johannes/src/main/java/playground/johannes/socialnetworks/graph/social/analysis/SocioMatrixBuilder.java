/* *********************************************************************** *
 * project: org.matsim.*
 * SocioMatrixBuilder.java
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
package playground.johannes.socialnetworks.graph.social.analysis;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;

/**
 * @author illenberger
 * 
 */
public class SocioMatrixBuilder {

	public static <K> SocioMatrix<K> countsMatrix(Map<? extends Vertex, K> values) {
		List<K> keyList = createKeyList(values);

		SocioMatrix<K> m = new SocioMatrix<K>(keyList);
		for (Entry<? extends Vertex, K> entry : values.entrySet()) {
			Vertex v = entry.getKey();
			K k1 = entry.getValue();
			if (k1 != null) {
				for (Vertex neighbor : v.getNeighbours()) {
					K k2 = values.get(neighbor);
					if(k2 != null) {
						m.adjustValue(k1, k2, 1.0);
					}
				}
			}
		}
		
		return m;
	}
	
	private static <K> List<K> createKeyList(Map<? extends Vertex, K> values) {
		Set<K> keys = new HashSet<K>();
		for (Entry<? extends Vertex, K> entry : values.entrySet()) {
			if (entry.getValue() != null)
				keys.add(entry.getValue());
		}
		return new ArrayList<K>(keys);
	}
	
	public static <K> void normalizeTotalSum(SocioMatrix<K> matrix) {
		double[][] m = matrix.getMatrix();
		
		double sum = 0;
		for(int i = 0; i < m.length; i++) {
			for(int j = 0; j < m.length; j++) {
				sum += m[i][j];
			}
		}
		
		for(int i = 0; i < m.length; i++) {
			for(int j = 0; j < m.length; j++) {
				m[i][j] /= sum;
			}
		}
	}
	
	public static <K> void normalizeRowSum(SocioMatrix<K> matrix) {
		double[][] m = matrix.getMatrix();
		
		
		for(int i = 0; i < m.length; i++) {
			double sum = 0;
			for(int j = 0; j < m.length; j++) {
				sum += m[i][j];
			}
			
			for(int j = 0; j < m.length; j++) {
				m[i][j] /= sum;
			}
		}
	}
	
	public static <K> SocioMatrix<K> probaMatrix(Map<? extends Vertex, K> values, Map<? extends Vertex, K> alters) {
		List<K> keyList = createKeyList(values);
		
		SocioMatrix<K> avr = new SocioMatrix<K>(keyList);
		SocioMatrix<K> count = new SocioMatrix<K>(keyList);
		for (Entry<? extends Vertex, K> entry : values.entrySet()) {
			Vertex v = entry.getKey();
			K k1 = entry.getValue();
			if (k1 != null) {
				TObjectIntHashMap<K> counts = new TObjectIntHashMap<K>();
				int total = 0;;
				for (Vertex neighbor : v.getNeighbours()) {
					K k2 = alters.get(neighbor);
					if(k2 != null) {
						counts.adjustOrPutValue(k2, 1, 1);
						total++;
					}
				}
				
				if(total > 0) {
					TObjectIntIterator<K> it = counts.iterator();
					for(int i = 0; i < counts.size(); i++) {
						it.advance();
						K k2 = it.key();
						avr.adjustValue(k1, k2, it.value()/(double)total);
						count.adjustValue(k1, k2, 1.0);
					}
				}
			}
		}
		
		double[][] mAvr = avr.getMatrix();
		double[][] mCount = count.getMatrix();
		for(int i = 0; i < mAvr.length; i++) {
			for(int j = 0; j < mAvr.length; j++) {
				mAvr[i][j] /= mCount[i][j];
			}
		}
		
		return avr;
	}
}
