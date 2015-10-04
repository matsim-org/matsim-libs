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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Data structure that memorizes double values as a function of String keys.  Typically, the key is an aggregation type.  If there 
 * are also bins, we need something else ({@link DatabinsMap}) . 
 * 
 * @author nagel
 */
public class DataMap<K> /*implements Map<K,Double>*/{

	private Map<K,Double> delegate = new TreeMap<K,Double>() ; // so the stuff comes out sorted.

	public int size() {
		return delegate.size();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	public boolean containsDoubleValue(Object value) {
		return delegate.containsValue(value);
	}

	public Double get(Object key) {
		return delegate.get(key);
	}

//	public Double put(K key, Double value) {
//		return delegate.put(key, value);
//	}
	
	private static int addCnt = 0 ;
	public Double addValue( K key, Double value ) {
		if ( addCnt < 10  ) {
			System.out.println( key.toString() + ": adding " + value );
		}
		Double result;
		if ( delegate.get(key)==null ) {
			result = delegate.put(key,value) ;
		} else {
			result = delegate.put( key, Double.valueOf( value.doubleValue() + delegate.get(key).doubleValue() ) ) ;
		}
		if ( addCnt < 10 ) {
			addCnt++ ;
			System.out.println( key.toString() + ": new value " + value );
		}
		return result ;
	}
	
	public Double inc( K key ) {
//		System.out.println( key.toString() + ": incrementing" );
		if ( delegate.get(key)==null ) {
			return delegate.put(key,1.) ;
		} else {
			return delegate.put( key, Double.valueOf( 1. + delegate.get(key).doubleValue() ) ) ;
		}
	}

	public Double remove(Object key) {
		return delegate.remove(key);
	}

//	public void putAll(Map<? extends K, ? extends Double> m) {
//		delegate.putAll(m);
//	}

	public void clear() {
		delegate.clear();
	}

	public Set<K> keySet() {
		return delegate.keySet();
	}

	public Collection<Double> values() {
		return delegate.values();
	}

	public Set<java.util.Map.Entry<K, Double>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

}
