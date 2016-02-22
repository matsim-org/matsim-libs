/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRouteConfigGroup
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
package org.matsim.core.config.groups;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * Config Module for PlansCalcRoute class.
 * Here you can specify the scale factors of freespeed travel time which are used
 * as travel time for not microsimulated modes.
 *
 * @author dgrether
 * @author mrieser
 */
public final class PlansCalcRouteConfigGroup extends ConfigGroup {
	// yy There is a certain degree of messiness in this class because of retrofitting, e.g. making beelineDistance mode-specific while
	// being backwards compatible.  This could eventually be cleaned up, maybe about a year after introducing it.  kai, jun'15
	
	
	public static final String GROUP_NAME = "planscalcroute";

	private static final String BEELINE_DISTANCE_FACTOR = "beelineDistanceFactor";
	private static final String NETWORK_MODES = "networkModes";
	private static final String TELEPORTED_MODE_SPEEDS = "teleportedModeSpeed_";
	private static final String TELEPORTED_MODE_FREESPEED_FACTORS = "teleportedModeFreespeedFactor_";

	public static final String UNDEFINED = "undefined";
	
	// For config file backward compatibility.
	// These are just hardcoded versions of the options above.
	private static final String PT_SPEED_FACTOR = "ptSpeedFactor";
	private static final String PT_SPEED = "ptSpeed";
	private static final String WALK_SPEED = "walkSpeed";
	private static final String BIKE_SPEED = "bikeSpeed";
	private static final String UNDEFINED_MODE_SPEED = "undefinedModeSpeed";
	
	private Collection<String> networkModes = Arrays.asList(TransportMode.car);

	private boolean acceptModeParamsWithoutClearing = false;
	
	private Double beelineDistanceFactor = 1.3 ;

	private boolean insertingAccessEgressWalk = false ;

	public static class ModeRoutingParams extends ReflectiveConfigGroup implements MatsimParameters {
		public static final String SET_TYPE = "teleportedModeParameters";

		private String mode = null;
		private Double teleportedModeSpeed = null;
		private Double teleportedModeFreespeedFactor = null;

		private Double beelineDistanceFactorForMode = null ;

		public ModeRoutingParams(final String mode) {
			super( SET_TYPE );
			setMode( mode );
		}

		public ModeRoutingParams() {
			super( SET_TYPE );
		}

		@Override
		public void checkConsistency() {
			if ( mode == null ) throw new RuntimeException( "mode for parameter set "+this+" is null!" );

			if ( teleportedModeSpeed == null && teleportedModeFreespeedFactor == null ) {
				throw new RuntimeException( "no teleported mode speed nor freespeed factor defined for mode "+mode );
			}

			if ( teleportedModeSpeed != null && teleportedModeFreespeedFactor != null ) {
				// this should not happen anyway as the setters forbid it
				throw new RuntimeException( "both teleported mode speed or freespeed factor are set for mode "+mode );
			}
		}

		@Override
		public Map<String, String> getComments() {
			final Map<String, String> map = super.getComments();

			map.put( "teleportedModeSpeed" ,
					"Speed for a teleported mode. " +
					"Travel time = (<beeline distance> * beelineDistanceFactor) / teleportedModeSpeed. Insert a line like this for every such mode.");
			map.put( "teleportedModeFreespeedFactor",
					"Free-speed factor for a teleported mode. " +
					"Travel time = teleportedModeFreespeedFactor * <freespeed car travel time>. Insert a line like this for every such mode. " +
					"Please do not set teleportedModeFreespeedFactor as well as teleportedModeSpeed for the same mode, but if you do, +" +
					"teleportedModeFreespeedFactor wins over teleportedModeSpeed.");

			return map;
		}

		@StringGetter( "mode" )
		public String getMode() {
			return mode;
		}

		@StringSetter( "mode" )
		public void setMode(String mode) {
			testForLocked() ;
			this.mode = mode;
		}

		@StringGetter( "teleportedModeSpeed" )
		public Double getTeleportedModeSpeed() {
			return teleportedModeSpeed;
		}

		@StringSetter( "teleportedModeSpeed" )
		public void setTeleportedModeSpeed(Double teleportedModeSpeed) {
			testForLocked() ;
			if ( getTeleportedModeFreespeedFactor() != null && teleportedModeSpeed != null ) {
				throw new IllegalStateException( "cannot set both speed and freespeed factor for "+getMode() );
			}
			this.teleportedModeSpeed = teleportedModeSpeed;
		}

		@StringGetter( "teleportedModeFreespeedFactor" )
		public Double getTeleportedModeFreespeedFactor() {
			return teleportedModeFreespeedFactor;
		}

		@StringSetter( "teleportedModeFreespeedFactor" )
		public void setTeleportedModeFreespeedFactor(
				Double teleportedModeFreespeedFactor) {
			testForLocked() ;
			if ( getTeleportedModeSpeed() != null && teleportedModeFreespeedFactor != null ) {
				throw new IllegalStateException( "cannot set both speed and freespeed factor for "+getMode() );
			}
			this.teleportedModeFreespeedFactor = teleportedModeFreespeedFactor;
		}
		
		@StringSetter("beelineDistanceFactor")
		public void setBeelineDistanceFactor( Double val ) {
			testForLocked() ;
			this.beelineDistanceFactorForMode = val ;
		}
		@StringGetter("beelineDistanceFactor")
		public Double getBeelineDistanceFactor() {
			return this.beelineDistanceFactorForMode ;
		}
		
	}
	
	public PlansCalcRouteConfigGroup() {
		super(GROUP_NAME);

		acceptModeParamsWithoutClearing = true;
		{
			final ModeRoutingParams bike = new ModeRoutingParams( TransportMode.bike );
			bike.setTeleportedModeSpeed( 15.0 / 3.6 ); // 15.0 km/h --> m/s
			addParameterSet( bike );
		}

		{
			final ModeRoutingParams walk = new ModeRoutingParams( TransportMode.walk );
			walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
			addParameterSet( walk );
		}
		
		// the following two are deliberately different from "walk" since "walk" may become a network routing mode, but these two
		// will not. kai, dec'15
		{
			final ModeRoutingParams walk = new ModeRoutingParams( TransportMode.access_walk );
			walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
			addParameterSet( walk );
		}
		{
			final ModeRoutingParams walk = new ModeRoutingParams( TransportMode.egress_walk );
			walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
			addParameterSet( walk );
		}

		// I'm not sure if anyone needs the "undefined" mode. In particular, it doesn't do anything for modes which are
		// really unknown, it is just a mode called "undefined". michaz 02-2012
		//
		// The original design idea was that some upstream module would figure out expected travel times and travel distances
		// for any modes, and the simulation would teleport all those modes it does not know anything about.
		// With the travel times and travel distances given by the mode.  In practice, it seems that people can live better
		// with the concept that mobsim figures it out by itself.  Although it is a much less flexible design.  kai, jun'2012
		{
			final ModeRoutingParams undefined = new ModeRoutingParams( UNDEFINED );
			undefined.setTeleportedModeSpeed( 50. / 3.6 ); // 50.0 km/h --> m/s
			addParameterSet( undefined );
		}

		{
			final ModeRoutingParams ride = new ModeRoutingParams( TransportMode.ride );
			ride.setTeleportedModeFreespeedFactor(1.0);
			addParameterSet( ride );
		}

		{
			final ModeRoutingParams pt = new ModeRoutingParams( TransportMode.pt );
			pt.setTeleportedModeFreespeedFactor( 2.0 );
			addParameterSet( pt );
		}

		//		{
//			final ModeRoutingParams transit_walk = new ModeRoutingParams( TransportMode.transit_walk ) ;
//			transit_walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
//			addParameterSet( transit_walk );
//		}
		// one might add the above but it was not added in the original design.  Not sure about the reason. kai, feb'15
		
		this.acceptModeParamsWithoutClearing = false;
	}

	@Override
	public ConfigGroup createParameterSet( final String type ) {
		switch ( type ) {
			case ModeRoutingParams.SET_TYPE:
				return new ModeRoutingParams();
			default:
				throw new IllegalArgumentException( type );
		}
	}

	@Override
	protected void checkParameterSet( final ConfigGroup module ) {
		switch ( module.getName() ) {
			case ModeRoutingParams.SET_TYPE:
				if ( !(module instanceof ModeRoutingParams) ) {
					throw new RuntimeException( "unexpected class for module "+module );
				}
				break;
			default:
				throw new IllegalArgumentException( module.getName() );
		}
	}

	@Override
	public void addParameterSet(final ConfigGroup set) {
		if ( set.getName().equals( ModeRoutingParams.SET_TYPE ) && !this.acceptModeParamsWithoutClearing ) {
			clearParameterSetsForType( set.getName() );
			this.acceptModeParamsWithoutClearing = true;
			
		}
		ModeRoutingParams pars = (ModeRoutingParams) set ;
		// for the time being pushing the "global" factor into the local ones if they are not initialized by
		// themselves.  Necessary for some tests; maybe we should eventually disable them.  kai, feb'15
		if ( pars.getBeelineDistanceFactor()== null ) {
			pars.setBeelineDistanceFactor( this.beelineDistanceFactor );
		}
		super.addParameterSet( set );
	}

	public void addModeRoutingParams(final ModeRoutingParams pars) {
		testForLocked() ;
		addParameterSet( pars );
	}

	public Map<String, ModeRoutingParams> getModeRoutingParams() {
		final Map<String, ModeRoutingParams> map = new LinkedHashMap< >();

		for ( ConfigGroup pars : getParameterSets( ModeRoutingParams.SET_TYPE ) ) {
			if ( this.isLocked() ) {
				pars.setLocked(); 
			}
			final String mode = ((ModeRoutingParams) pars).getMode();
			final ModeRoutingParams old =
				map.put( mode ,
					(ModeRoutingParams)	pars );
			if ( old != null ) throw new IllegalStateException( "several parameter sets for mode "+mode );
		}

		return map;
	}

	public ModeRoutingParams getOrCreateModeRoutingParams(final String mode) {
		ModeRoutingParams pars = getModeRoutingParams().get( mode );

		if ( pars == null ) {
			pars = (ModeRoutingParams) createParameterSet( ModeRoutingParams.SET_TYPE );
			pars.setMode( mode );
			addParameterSet( pars );
		}
		if ( this.isLocked() ) {
			pars.setLocked(); 
		}

		return pars;
	}

	@Override
	public String getValue(final String key) {
		throw new IllegalArgumentException(key + ": getValue access disabled; use direct getter");
	}

	@Override
	public void addParam(final String key, final String value) {
		if( value.equals( "null" ) ) return; // old behavior of reader: keep defaults if null
		if (PT_SPEED_FACTOR.equals(key)) {
			setTeleportedModeFreespeedFactor(TransportMode.pt, Double.parseDouble(value));
		} else if (BEELINE_DISTANCE_FACTOR.equals(key)) {
			setBeelineDistanceFactor(Double.parseDouble(value));
		} else if (PT_SPEED.equals(key)) {
			setTeleportedModeSpeed(TransportMode.pt, Double.parseDouble(value));
		} else if (WALK_SPEED.equals(key)) {
			setTeleportedModeSpeed(TransportMode.walk, Double.parseDouble(value));
		} else if (BIKE_SPEED.equals(key)) {
			setTeleportedModeSpeed(TransportMode.bike, Double.parseDouble(value));
		} else if (UNDEFINED_MODE_SPEED.equals(key)) {
			setTeleportedModeSpeed(UNDEFINED, Double.parseDouble(value));
		} else if (NETWORK_MODES.equals(key)) {
			setNetworkModes(Arrays.asList(CollectionUtils.stringToArray(value)));
		} else if (key.startsWith(TELEPORTED_MODE_SPEEDS)) {
			setTeleportedModeSpeed(key.substring(TELEPORTED_MODE_SPEEDS.length()), Double.parseDouble(value));
		} else if (key.startsWith(TELEPORTED_MODE_FREESPEED_FACTORS)) {
			setTeleportedModeFreespeedFactor(key.substring(TELEPORTED_MODE_FREESPEED_FACTORS.length()), Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final Map<String, String> getParams() {
		Map<String, String> map = super.getParams();
		map.put( NETWORK_MODES, CollectionUtils.arrayToString(this.networkModes.toArray(new String[this.networkModes.size()])));

		//		map.put( BEELINE_DISTANCE_FACTOR, Double.toString(this.getBeelineDistanceFactor()) );

//		for ( ModeRoutingParams param : this.getModeRoutingParams().values() ) {
//			if ( !param.getBeelineDistanceFactor().equals( this.beelineDistanceFactor ) ) {
//				log.error( "beeline distance factor varies by mode; this cannot be accessed by getParams()" ) ;
//			}
//		}
//		map.put( BEELINE_DISTANCE_FACTOR, Double.toString( this.beelineDistanceFactor ) ) ;
//
// if we uncomment the above, then this is also written into config v2 fmt, which we don't want.  kai, feb'15
		
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		map.put(BEELINE_DISTANCE_FACTOR, "factor with which beeline distances (and therefore times) " +
				"are multiplied in order to obtain an estimate of the network distances/times.  Default is something like 1.3") ;
		map.put(NETWORK_MODES, "All the modes for which the router is supposed to generate network routes (like car)") ;

		return map;
	}

	public Collection<String> getNetworkModes() {
		return this.networkModes;
	}

	public void setNetworkModes(Collection<String> networkModes) {
		this.networkModes = networkModes;
	}

	public Map<String, Double> getTeleportedModeSpeeds() {
		final Map<String, Double> map = new LinkedHashMap< >();
		for ( ModeRoutingParams pars : getModeRoutingParams().values() ) {
			if ( this.isLocked() ) {
				pars.setLocked(); 
			}

			final Double speed = pars.getTeleportedModeSpeed();
			if ( speed != null ) map.put( pars.getMode() , speed );
		}
		return map;
	}

	public Map<String, Double> getTeleportedModeFreespeedFactors() {
		final Map<String, Double> map = new LinkedHashMap< >();
		for ( ModeRoutingParams pars : getModeRoutingParams().values() ) {
			if ( this.isLocked() ) {
				pars.setLocked(); 
			}

			final Double speed = pars.getTeleportedModeFreespeedFactor();
			if ( speed != null ) map.put( pars.getMode() , speed );
		}
		return map;
	}
	
	public Map<String,Double> getBeelineDistanceFactors() {
		final Map<String,Double> map = new LinkedHashMap<>() ;
		for ( ModeRoutingParams pars : getModeRoutingParams().values() ) {
			if ( this.isLocked() ) {
				pars.setLocked(); 
			}

			final Double val = pars.getBeelineDistanceFactor() ;
			if ( val != null ) map.put( pars.getMode() , val ) ;
		}
		return map ;
	}
	
	@Deprecated // use mode-specific factors.  kai, apr'15
	public void setTeleportedModeFreespeedFactor(String mode, double freespeedFactor) {
		testForLocked() ;
		// re-create, to trigger erasing of defaults (normally forbidden, see acceptModeParamsWithoutClearing)
		final ModeRoutingParams pars = new ModeRoutingParams( mode );
		pars.setTeleportedModeFreespeedFactor( freespeedFactor );
		addParameterSet( pars );
	}

	@Deprecated // use mode-specific factors.  kai, apr'15
	public void setTeleportedModeSpeed(String mode, double speed) {
		testForLocked() ;
		// re-create, to trigger erasing of defaults (normally forbidden, see acceptModeParamsWithoutClearing)
		final ModeRoutingParams pars = new ModeRoutingParams( mode );
		pars.setTeleportedModeSpeed( speed );
		addParameterSet( pars );
	}
	
	@Deprecated // use mode-specific beeline distance factors! kai, apr'15
	public void setBeelineDistanceFactor(double val) {
		testForLocked() ;
		// yyyy thinking about this: this should in design maybe not be different from the other teleportation factors (reset everything
		// if one is set; or possibly disallow setting it at all). kai, feb'15
		
		// memorize the global factor for ModeRoutingParams that are added later:
		this.beelineDistanceFactor = val ;
		
		// push the global factor to the local ones for all ModeRoutingParams that are already there:
		for ( ModeRoutingParams params : this.getModeRoutingParams().values() ) {
			params.setBeelineDistanceFactor( val );
		}
	}

	public boolean isInsertingAccessEgressWalk() {
		return this.insertingAccessEgressWalk ;
	}
	public void setInsertingAccessEgressWalk( boolean val ) {
		this.insertingAccessEgressWalk = val ;
	}


}
