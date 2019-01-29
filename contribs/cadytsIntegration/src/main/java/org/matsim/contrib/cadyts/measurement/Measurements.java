package org.matsim.contrib.cadyts.measurement;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.counts.Count;

public final class Measurements implements LookUpItemFromId<Measurement> {
	private static final Logger log = Logger.getLogger( Measurements.class );
	
	private final Map< Id<Measurement>,Measurement > map = new TreeMap<>() ;

	private final SortedSet<Measurement> set = new TreeSet<>( new Comparator<Measurement>(){
		@Override
		public int compare(Measurement o1, Measurement o2) {
			if ( o1.getLowerBound() < o2.getLowerBound() ) {
				return -1 ;
			} else if ( o1.getLowerBound() == o2.getLowerBound() ) {
				return 0 ;
			} else {
				return 1 ;
			}
		}
	} ) ;
	
	public void add( Count<Measurement> cnt, double lowerBound ) {
		Measurement mea = new Measurement( cnt.getId(), lowerBound ) ;
		map.put( mea.getId(), mea ) ;
		set.add( mea ) ;
	}
	
	@Override 
	public Measurement getItem(Id<Measurement> id) {
		return map.get( id ) ;
	}
	
	Measurement getMeasurementFromTTimeInSeconds( double ttime ) {
		return map.get(getMeasurementIdFromTTimeInSeconds(ttime)) ;
	}

	private Id<Measurement> getMeasurementIdFromTTimeInSeconds( double ttime ) {
		log.warn("ttime=" + ttime  + " set=" + set );
		Measurement prev = null ;
		for ( Measurement mea : set ) {
			log.warn("lowerBound=" + mea.getLowerBound() ) ;
			if ( mea.getLowerBound() > ttime ) {
				if ( prev != null ) {
					return prev.getId() ;
				} else {
					log.warn( "entry below smallest category; adding to smallest category" ) ;
					return mea.getId();
				}
			}
			prev = mea ;
		}
		return prev.getId() ;
	}
	

}