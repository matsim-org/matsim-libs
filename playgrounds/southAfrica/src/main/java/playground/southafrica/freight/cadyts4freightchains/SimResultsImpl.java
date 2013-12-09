package playground.southafrica.freight.cadyts4freightchains;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Identifiable;

import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/**
 * Cadyts needs to have access to sim results.  This is achieved by giving Cadyts an object with interface SimResults.  
 * Often, this is just a wrapper around some other observational class which exists anyways in MATSim, e.g. 
 * flow counts on links, or occupancy counts between pt stations.  In case there is no such observational
 * class, then this class here can be used.  If it is not powerful enough, it can be extended.
 * <p/>
 * yy For the time being, it does not deal with the time periods.  Thus, it cannot be published in its current form. 
 * 
 * @author nagel
 *
 * @param <T>
 */
final class SimResultsImpl<T extends Identifiable> implements SimResults<T> {
	private static final long serialVersionUID = 1L;
	final static Logger log = Logger.getLogger(SimResultsImpl.class);
	Map<T,Double> cnts = new TreeMap<T,Double>() ;
	
	SimResultsImpl( Collection<T> items ) {
		for ( T item : items ) {
			cnts.put( item, 0.) ;
			log.info("initialized sim results container for item with id: " + item.getId() ) ;
		}
	}

	@Override
	public double getSimValue(T item, int time1, int time2, TYPE arg3) {
		switch( arg3 ) {
		case COUNT_VEH:
			return cnts.get(item) ;
		case FLOW_VEH_H:
			return cnts.get(item) ;
		default:
			throw new RuntimeException("not implemented") ;
		}
	}
	void incCnt( T item ) {
//		log.warn("adding entry for item with id: " + item.getId() ) ;
		cnts.put( item, 1. + cnts.get(item) ) ;
	}
	void incCnt( T item, double val ) {
		cnts.put( item, val + cnts.get(item) ) ;
	}

	void reset() {
		for ( T item : cnts.keySet() ) {
			cnts.put(item, 0.) ;
		}
	}
	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder() ;
		for ( Entry<T,Double> entry : cnts.entrySet() ) {
			strb.append( entry.getKey().getId().toString() + " " + entry.getValue().toString() + "\n") ;
		}
		return strb.toString() ;
	}
}