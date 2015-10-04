/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.analysis.kai;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * A data structure that allows to set categories (i.e. data boundaries) per type, and then allows multiple keys per type.
 * <br/>
 * Typical examples of types are travel times, travel distances, etc., where one may collect the information in bins.
 * <br/>
 * Typical examples of keys are trip purposes, e.g. one wants to collect travel times, travel distances etc. separately for
 * each trip purpose, <i> but within the same categories. </i>
 * 
 * @author nagel
 */
class DatabinsMap<T,K> {
	static class Databins<K> {
		private Map<K,double[]> delegate = new TreeMap<>() ;
		private double[] dataBoundaries ;
		public double[] getDataBoundaries() {
			return dataBoundaries ;
		}
		public void setDataBoundaries( double[] bnd ) {
			dataBoundaries = bnd ;
		}
		public double[] getValues(K key) {
			return delegate.get(key) ;
		}
		public void instantiate(K key) {
			double[] array = new double[ dataBoundaries.length] ;
			delegate.put( key, array ) ;
		}
		public void addValue(K key, int idx, Double val) {
			delegate.get(key)[idx] += val ;
		}
		public void inc(K key, int idx) {
			delegate.get(key)[idx] ++ ;
		}
	}
	
	private static final Logger log = Logger.getLogger( DatabinsMap.class ) ;
	
	private Map< T, Databins<K>> delegate = new TreeMap<>() ;
	public final  Databins<K> get( T type ) {
		return delegate.get(type) ;
	}
	public final double[] getDataBoundaries( T type ) {
		return delegate.get(type).getDataBoundaries() ;
	}
	public final void reset() {
		delegate.clear();
		// dataBoundaries.clear() ; // no
	}
	public final void putDataBoundaries( T type, double[] tmp ) {
		delegate.get(type).setDataBoundaries( tmp ) ;
	}
	public final int getIndex( T type, double dblVal ) {
		double[] dataBoundariesTmp = delegate.get(type).getDataBoundaries() ;
		int ii = dataBoundariesTmp.length-1 ;
		for ( ; ii>=0 ; ii-- ) {
			if ( dataBoundariesTmp[ii] <= dblVal ) 
				return ii ;
		}
		log.warn("statistics contains value that smaller than the smallest category; adding it to smallest category" ) ;
		log.warn("statType: " + type + "; val: " + dblVal ) ;
		return 0 ;
	}
	public final void addValue(  T type, K key, int idx, Double val ) {
		instantiateIfNecessary(type, key);
//		delegate.get(type).get(key)[idx] += val ;
		delegate.get(type).addValue( key, idx, val ) ;
	}
	public final void inc( T type, K key, int idx ) {
		instantiateIfNecessary(type,key) ;
		delegate.get(type).inc(key, idx ) ;
	}
	private void instantiateIfNecessary(T type, K key) {
		// if type does not yet exist:
		if ( delegate.get(type) == null ) {
			delegate.put( type, new Databins<K>() );
		}
		
		// if key (e.g. trip purpose) does not yet exist:
		if ( delegate.get(type).getValues(key) == null  ) {
			delegate.get(type).instantiate(key) ;
		}
	}
}
