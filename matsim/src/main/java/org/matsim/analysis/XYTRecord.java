package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * class for records of type (time, x, y, attrib1, attrib2) as we often need them (noise, emissions, accessibility, ...).
 */
public class XYTRecord{
	//  yyyyyy I am at this point unsure about times and time interpretation.  Some option:
	// * give only one time.  This could be interpreted as (a) startTime (i.e. when vis moves to this data set); (b) midTime (i.e. when interpolation
	// scheme should display exactly this data set)
	// * give start/endTime.  Problems: (a) what if endTime is not same as startTime of next data set?  (b) how to encode gps trajectories (which
	// would only have a time step?)  Clearly, for gps tracks one could just have endTime=startTime, or leave endTime empty.
	private double startTime ;
	private double endTime ;
	private Id<?> facilityId ;
	private Coord coord ;
	private Map<String,Double> map ;

	private XYTRecord( double startTime, double endTime, Coord coord, Id<?> facilityId, Map<String,Double> map ) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.coord = coord;
		this.facilityId = facilityId;
		this.map = map ;
	}
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder( "NoiseRecord=[ startTime=" + startTime + " | endTime=" + endTime + " | facilityId=" + facilityId
									     + " | coord=" + coord );
		for( Map.Entry<String, Double> entry : map.entrySet() ){
			str.append( " | " ).append( entry.getKey() ).append( "=" ).append(entry.getValue()) ;
		}
		str.append( " ]" ) ;
		return str.toString() ;
	}
	public double getStartTime(){
		return startTime;
	}
	public double getEndTime(){
		return endTime;
	}
	public Id<?> getFacilityId(){
		return facilityId;
	}
	public Coord getCoord(){
		return coord;
	}
	public Map<String,Double> getMap() { return Collections.unmodifiableMap(map ); }

	public static class Builder{
		private double startTime = Double.NEGATIVE_INFINITY;
		private double endTime = Double.POSITIVE_INFINITY;
		private Coord coord = null;
		private Id<?> facilityId = null;
		private Map<String,Double> map = new HashMap<>() ;
		public Builder setStartTime( double startTime ){
			this.startTime = startTime;
			return this;
		}
		public Builder setEndTime( double endTime ){
			this.endTime = endTime;
			return this;
		}
		public Builder setCoord( Coord coord ){
			this.coord = coord;
			return this;
		}
		public Builder setFacilityId( Id<?> facilityId ){
			this.facilityId = facilityId;
			return this;
		}
		public Builder put( String key, double value ) {
			// this is deliberately "double" and not "Double" since
			// * we might want to keep the option to back it by a data structure that allows primitive types
			// * if one wants to set a "null" value, one can alternatively not set the value at all.  (A csv writer may then have to compensate
			// for that.)
			// kai, aug'19
			map.put( key, value ) ;
			return this ;
		}
		public XYTRecord build(){
			return new XYTRecord( startTime , endTime , coord , facilityId, map  );
		}
	}
}
