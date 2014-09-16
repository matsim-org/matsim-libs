package org.matsim.contrib.matrixbasedptrouter.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HeaderParser {
	
	/**
	 * This is used to parse a header line from a tab-delimited urbansim header and generate a Map that allows to look up column
	 * numbers (starting from 0) by giving the header line.
	 *
	 * I.e. if you have a header "from_id <tab> to_id <tab> travel_time", then idxFromKey.get("to_id") will return "1".
	 *
	 * This makes the reading of column-oriented files independent from the sequence of the columns.
	 *
	 * @param line
	 * @return idxFromKey as described above (mapping from column headers into column numbers)
	 *
	 * @author nagel
	 */
	public static Map<String,Integer> createIdxFromKey( String line, String seperator ) {
		String[] keys = line.split( seperator ) ;

		Map<String,Integer> idxFromKey = new ConcurrentHashMap<String, Integer>() ;
		for ( int i=0 ; i<keys.length ; i++ ) {
			idxFromKey.put(keys[i], i ) ;
		}
		return idxFromKey ;
	}
	
	/**
	 * See describtion from createIdxFromKey( String line, String seperator )
	 * 
	 * @param keys
	 * @return idxFromKey
	 */
	public static Map<Integer,Integer> createIdxFromKey( int keys[]) {

		Map<Integer,Integer> idxFromKey = new ConcurrentHashMap<Integer, Integer>() ;
		for ( int i=0 ; i<keys.length ; i++ ) {
			idxFromKey.put(keys[i], i ) ;
		}
		return idxFromKey ;
	}

}
