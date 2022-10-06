package org.matsim.contrib.analysis.kai;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 
 * Data structure that allows to register data boundaries, and then to count data in the thereby defined categories. 
 * The data structure allows multiple keys, so it is, for example, possible to count different trip purposes into the same 
 * categories.
 * 
 * @author nagel
 *
 * @param <K>
 */
public final class Databins<K> {
	private static final Logger log = LogManager.getLogger(Databins.class) ;
	private Map<K,double[]> delegate = new TreeMap<>() ;
	private double[] dataBoundaries ;
	private final String typeName;
	public Databins( String type, double[] dataBoundaries ) {
		this.typeName = type ;
		this.dataBoundaries = dataBoundaries ;
	}
	public double getValue(K key, int idx) {
		if ( delegate.get(key)==null ) {
			instantiate(key) ;
		}
		return this.delegate.get(key)[idx] ;
	}
	public Set<Entry<K, double[]>> entrySet() {
		return this.delegate.entrySet() ;
	}
	public double[] getDataBoundaries() {
		return dataBoundaries ;
	}
	public double[] getValues(K key) {
		if ( delegate.get(key)==null ) {
			instantiate(key) ;
		}
		return delegate.get(key) ;
	}
	public void instantiate(K key) {
		double[] array = new double[ dataBoundaries.length] ;
		delegate.put( key, array ) ;
	}
	public void addValue(K key, int idx, Double val) {
		if ( delegate.get(key)==null ) {
			instantiate(key) ;
		}
		delegate.get(key)[idx] += val ;
	}
	public void inc(K key, int idx) {
		if ( delegate.get(key)==null ) {
			instantiate(key) ;
		}
		delegate.get(key)[idx] ++ ;
	}
	public int getIndex(double dblVal) {
		int ii = dataBoundaries.length-1 ;
		for ( ; ii>=0 ; ii-- ) {
			if ( dataBoundaries[ii] <= dblVal ) 
				return ii ;
		}
		log.warn("statistics contains value that smaller than the smallest category; adding it to smallest category" ) ;
		log.warn("statType: " + typeName + "; val: " + dblVal ) ;
		return 0 ;
	}
	public void clear() {
		this.delegate.clear();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}