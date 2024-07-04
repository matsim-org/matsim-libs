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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.config.ReflectiveConfigGroup.StringSetter;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.*;

/**
 * Config Module for PlansCalcRoute class.
 * Here you can specify the scale factors of freespeed travel time which are used
 * as travel time for not microsimulated modes.
 *
 * @author dgrether
 * @author mrieser
 */
public final class RoutingConfigGroup extends ConfigGroup {
	// yy There is a certain degree of messiness in this class because of retrofitting, e.g. making beelineDistance mode-specific while
	// being backwards compatible.  This could eventually be cleaned up, maybe about a year after introducing it.  kai, jun'15

	public static final String GROUP_NAME = "routing";

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

	private static final Logger log = LogManager.getLogger(RoutingConfigGroup.class) ;

	private Collection<String> networkModes = Collections.singletonList( TransportMode.car );

	private boolean acceptModeParamsWithoutClearing;

	private Double beelineDistanceFactor = 1.3 ;

	public enum AccessEgressType {
		@Deprecated none,

		/**
		 * Euclidian distance from facility to nearest point on link; then teleported walk.  In normal cases, all activities that belong to the
		 * same link are mapped into the same facility; in consequence, in that situation all will have the same walk time.  kai, may'23
		 */
		accessEgressModeToLink,

		/**
		 * The walk time to the link is taken from a link attribute, and in consequence the same for all agents walking to that link.
		 */
		walkConstantTimeToLink,

		/**
		 * The sum of the two above.  I.e. walking the Euklidean distance PLUS a time taken from the link attribute.
		 */
		accessEgressModeToLinkPlusTimeConstant
	}

	private static final String ACCESSEGRESSTYPE = "accessEgressType";
	private static final String ACCESSEGRESSTYPE_CMT = "Defines how access and egress to main mode is simulated. Either of [none, accessEgressModeToLink, walkConstantTimeToLink, accessEgressModeToLinkPlusTimeConstant], Current default=none which means no access or egress trips are simulated.";
	private AccessEgressType accessEgressType = AccessEgressType.none;

	// ---
	private static final String RANDOMNESS = "routingRandomness" ;
	private double routingRandomness = 3. ;
	// ---
	private static final String CLEAR_MODE_ROUTING_PARAMS = "clearDefaultTeleportedModeParams";
	private static final String CLEAR_MODE_ROUTING_PARAMS_CMT = "Some typical teleportation routing params are set by default, such as for walk and bike.  " +
																		"Setting this switch to \"true\" will clear them.  Note that this will also clear " +
																		"settings for helper modes such as for " + TransportMode.non_network_walk;
	private boolean clearingDefaultModeRoutingParams = false ;

	private static final String NETWORK_ROUTE_CONSISTENCY_CHECK = "networkRouteConsistencyCheck";
	private NetworkRouteConsistencyCheck networkRouteConsistencyCheck = NetworkRouteConsistencyCheck.abortOnInconsistency;

	public enum NetworkRouteConsistencyCheck {
		disable, abortOnInconsistency
	}

	/**
	 * @deprecated -- use {@link TeleportedModeParams} to be consistent with xml config.  kai, jun'23
	 */
	public final static class ModeRoutingParams extends TeleportedModeParams{
		public ModeRoutingParams( String mode ){
			super( mode );
		}

		// repeating the setters so that they return the right type with chaining.  kai, jan'23
		@Override public ModeRoutingParams setMode( String mode ) {
			super.setMode( mode );
			return this;
		}
		@Override public ModeRoutingParams setTeleportedModeSpeed( Double teleportedModeSpeed ) {
			super.setTeleportedModeSpeed( teleportedModeSpeed );
			return this;
		}
		@Override public ModeRoutingParams setTeleportedModeFreespeedLimit( Double teleportedModeFreespeedLimit ) {
			super.setTeleportedModeFreespeedLimit( teleportedModeFreespeedLimit );
			return this;
		}
		@Override public ModeRoutingParams setTeleportedModeFreespeedFactor( Double teleportedModeFreespeedFactor ) {
			super.setTeleportedModeFreespeedFactor( teleportedModeFreespeedFactor );
			return this;
		}
		@Override public ModeRoutingParams setBeelineDistanceFactor( Double beelineDistanceFactor ) {
			super.setBeelineDistanceFactor( beelineDistanceFactor );
			return this;
		}
	}
	public static class TeleportedModeParams extends ReflectiveConfigGroup implements MatsimParameters {
		public static final String SET_TYPE = "teleportedModeParameters";
		public static final String MODE = "mode";
		public static final String TELEPORTED_MODE_FREESPEED_FACTOR = "teleportedModeFreespeedFactor";

		private String mode = null;

		// beeline teleportation:
		private Double teleportedModeSpeed = null;
		private Double beelineDistanceFactorForMode = null ;

		// route computed on network:
		private Double teleportedModeFreespeedFactor = null;
		private Double teleportedModeFreespeedLimit = Double.POSITIVE_INFINITY ;

		private static final String TELEPORTED_MODE_FREESPEED_FACTOR_CMT = "Free-speed factor for a teleported mode. " +
		"Travel time = teleportedModeFreespeedFactor * <freespeed car travel time>. Insert a line like this for every such mode. " +
		"Please do not set teleportedModeFreespeedFactor as well as teleportedModeSpeed for the same mode, but if you do, +" +
		"teleportedModeFreespeedFactor wins over teleportedModeSpeed.";

		private static final String TELEPORTED_MODE_FREESPEED_LIMIT_CMT = "When using freespeed factor, a speed limit on the free speed. "
				+ "Link travel time will be $= factor * [ min( link_freespeed, freespeed_limit) ]" ;

		public TeleportedModeParams( final String mode ) {
			super( SET_TYPE );
			setMode( mode );
		}

		public TeleportedModeParams() {
			super( SET_TYPE );
		}

		@Override
		public void checkConsistency(Config config) {
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
			map.put( TELEPORTED_MODE_FREESPEED_FACTOR, TELEPORTED_MODE_FREESPEED_FACTOR_CMT);

			return map;
		}

		/**
		 * Currently not in xml interface.
		 *
		 * @return teleportedModeFreespeedLimit -- {@value #TELEPORTED_MODE_FREESPEED_LIMIT_CMT}
		 */
		public final Double getTeleportedModeFreespeedLimit() {
			return this.teleportedModeFreespeedLimit;
		}

		/**
		 * Currently not in xml interface.
		 *
		 * @param teleportedModeFreespeedLimit -- {@value #TELEPORTED_MODE_FREESPEED_LIMIT_CMT}
		 */
		public TeleportedModeParams setTeleportedModeFreespeedLimit( Double teleportedModeFreespeedLimit ) {
			this.teleportedModeFreespeedLimit = teleportedModeFreespeedLimit;
			return this;
		}

		@StringGetter(MODE)
		public String getMode() {
			return mode;
		}

		@StringSetter(MODE)
		public TeleportedModeParams setMode( String mode ) {
			testForLocked() ;
			this.mode = mode;
			return this ;
		}

		@StringGetter( "teleportedModeSpeed" )
		public Double getTeleportedModeSpeed() {
			return teleportedModeSpeed;
		}

		@StringSetter( "teleportedModeSpeed" )
		public TeleportedModeParams setTeleportedModeSpeed( Double teleportedModeSpeed ) {
			testForLocked() ;
			if ( getTeleportedModeFreespeedFactor() != null && teleportedModeSpeed != null ) {
				throw new IllegalStateException( "cannot set both speed and freespeed factor for "+getMode() );
			}
			this.teleportedModeSpeed = teleportedModeSpeed;
			return this;
		}

		/**
		 * @return {@value #TELEPORTED_MODE_FREESPEED_FACTOR_CMT}
		 */
		@StringGetter(TELEPORTED_MODE_FREESPEED_FACTOR)
		public Double getTeleportedModeFreespeedFactor() {
			return teleportedModeFreespeedFactor;
		}

		/**
		 * @param teleportedModeFreespeedFactor -- {@value #TELEPORTED_MODE_FREESPEED_FACTOR_CMT}
		 */
		@StringSetter(TELEPORTED_MODE_FREESPEED_FACTOR)
		public TeleportedModeParams setTeleportedModeFreespeedFactor(
				Double teleportedModeFreespeedFactor ) {
			testForLocked() ;
			if ( getTeleportedModeSpeed() != null && teleportedModeFreespeedFactor != null ) {
				throw new IllegalStateException( "cannot set both speed and freespeed factor for "+getMode() );
			}
			this.teleportedModeFreespeedFactor = teleportedModeFreespeedFactor;
			return this;
		}

		@StringSetter("beelineDistanceFactor")
		public TeleportedModeParams setBeelineDistanceFactor( Double val ) {
			testForLocked() ;
			this.beelineDistanceFactorForMode = val ;
			return this ;
		}
		@StringGetter("beelineDistanceFactor")
		public Double getBeelineDistanceFactor() {
			return this.beelineDistanceFactorForMode ;
		}

	}

	public RoutingConfigGroup() {
		super(GROUP_NAME);

		acceptModeParamsWithoutClearing = true;
		{
			final TeleportedModeParams bike = new TeleportedModeParams( TransportMode.bike );
			bike.setTeleportedModeSpeed( 15.0 / 3.6 ); // 15.0 km/h --> m/s
			addParameterSet( bike );
		}

		{
			final TeleportedModeParams walk = new TeleportedModeParams( TransportMode.walk );
			walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
			addParameterSet( walk );
		}

		// the following two are deliberately different from "walk" since "walk" may become a network routing mode, but these two
		// will not. kai, dec'15
		{
			final TeleportedModeParams walk = new TeleportedModeParams( TransportMode.non_network_walk );
			walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
			addParameterSet( walk );
		}
//		{
//			final ModeRoutingParams walk = new ModeRoutingParams( TransportMode.egress_walk );
//			walk.setTeleportedModeSpeed( 3.0 / 3.6 ); // 3.0 km/h --> m/s
//			addParameterSet( walk );
//		}

		// I'm not sure if anyone needs the "undefined" mode. In particular, it doesn't do anything for modes which are
		// really unknown, it is just a mode called "undefined". michaz 02-2012
		//
		// The original design idea was that some upstream module would figure out expected travel times and travel distances
		// for any modes, and the simulation would teleport all those modes it does not know anything about.
		// With the travel times and travel distances given by the mode.  In practice, it seems that people can live better
		// with the concept that mobsim figures it out by itself.  Although it is a much less flexible design.  kai, jun'2012
//		{
//			final ModeRoutingParams undefined = new ModeRoutingParams( UNDEFINED );
//			undefined.setTeleportedModeSpeed( 50. / 3.6 ); // 50.0 km/h --> m/s
//			addParameterSet( undefined );
//		}

		{
			final TeleportedModeParams ride = new TeleportedModeParams( TransportMode.ride );
			ride.setTeleportedModeFreespeedFactor(1.0);
			addParameterSet( ride );
		}

		{
			final TeleportedModeParams pt = new TeleportedModeParams( TransportMode.pt );
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
			case TeleportedModeParams.SET_TYPE:
				return new TeleportedModeParams();
			default:
				throw new IllegalArgumentException( type );
		}
	}

	@Override
	protected void checkParameterSet( final ConfigGroup module ) {
		switch ( module.getName() ) {
			case TeleportedModeParams.SET_TYPE:
				if ( !(module instanceof TeleportedModeParams) ) {
					throw new RuntimeException( "unexpected class for module "+module );
				}
				break;
			default:
				throw new IllegalArgumentException( module.getName() );
		}
	}
	/**
	 * {@value CLEAR_MODE_ROUTING_PARAMS_CMT}
	 */
	public void setClearingDefaultModeRoutingParams( boolean val ) {
		if ( val ) {
			clearModeRoutingParams();
		} else if ( clearingDefaultModeRoutingParams ) {
			throw new RuntimeException( "you cannot set the clearing of the default mode routing (= teleportation mode) params to false after you have already cleared once." ) ;
		}
	}
	public void clearTeleportedModeParams() {
		this.clearModeRoutingParams();
	}
	/**
	 * @deprecated -- use {@link #clearTeleportedModeParams()} to be consistent with naming in xml config.
	 */
	public void clearModeRoutingParams( ) {
		// This is essentially a config switch, except that it cannot be set back to false once it was set to true.

		// from now on, we will accept without clearing:
		this.acceptModeParamsWithoutClearing = true;

		// do the clearing:
		clearParameterSetsForType( TeleportedModeParams.SET_TYPE );

		// memorize that we have cleared; this is what will be written into the output config (once we have one):
		this.clearingDefaultModeRoutingParams = true ;
	}

	@Override
	public void addParameterSet(final ConfigGroup set) {
		if ( set.getName().equals( TeleportedModeParams.SET_TYPE ) && !this.acceptModeParamsWithoutClearing ) {
			clearParameterSetsForType( set.getName() );
			this.acceptModeParamsWithoutClearing = true;
			log.warn( "The first mode routing (= teleported mode) params that are explicitly defined clear the default mode routing (= teleported mode) params.  If you want to avoid this " );
			log.warn( "    warning, use clearTeleportedModeParams(true) in code, and \"" + CLEAR_MODE_ROUTING_PARAMS + "\"=true in xml config.");

//						  "This functionality was removed for " );
//			log.warn( "    some weeks in the development head, after release 11.x, and before release 12.x; it is now back.  " +

			// A bit more info:
			//
			// (1) I wanted to keep the default teleportation routers ... since for novice users I find it better if they all use the same teleportation
			// speeds.  kai, nov'19
			//
			// (2) The result of "add" and "remove" in code evidently depends on the sequence. In contrast, our "config" object
			// is a state ... as one notices when we write it out, since there we cannot play back additions and removal.  So if
			// we add to and remove from the default entries, and write the final result to file, then re-reading these entries
			// needs to trigger removal of the defaults since otherwise they will exist in addition. kai, nov'19
		}
		TeleportedModeParams pars = (TeleportedModeParams) set ;
		// for the time being pushing the "global" factor into the local ones if they are not initialized by
		// themselves.  Necessary for some tests; maybe we should eventually disable them.  kai, feb'15
		if ( pars.getBeelineDistanceFactor()== null ) {
			pars.setBeelineDistanceFactor( this.beelineDistanceFactor );
		}
		super.addParameterSet( set );
	}

	public void addTeleportedModeParams( final TeleportedModeParams pars ) {
		testForLocked() ;
		addParameterSet( pars );
	}
	/**
	 * @deprecated -- use {@link #addTeleportedModeParams(TeleportedModeParams)} instead.
	 */
	public void addModeRoutingParams(final TeleportedModeParams pars ) {
		this.addTeleportedModeParams( pars );
	}
	public void removeTeleportedModeParams( String key ){
		this.removeModeRoutingParams( key );
	}
	/**
	 * @deprecated -- use {@link #removeTeleportedModeParams(String)} instead.
	 */
	public void removeModeRoutingParams( String key ) {
		testForLocked() ;
		for ( ConfigGroup pars : getParameterSets( TeleportedModeParams.SET_TYPE ) ) {
			final String mode = ((TeleportedModeParams) pars).getMode();
			if ( key.equals(mode) ) {
				this.removeParameterSet(pars) ;
				break ;
			}
		}
		if ( getParameterSets( TeleportedModeParams.SET_TYPE ).isEmpty() ) {
			log.warn( "You have removed the last mode routing (= teleported mode) parameter with the removeModeRoutingParams method.  If you wrote the resulting config to " ) ;
			log.warn("    file, and read it back in, all default teleported modes would be resurrected.  The code will therefore also call  " );
			log.warn( "    \"clearTeleportedModeParams()\".  It would be better if you did this yourself." ) ;
			this.clearModeRoutingParams();
		}
	}
	public Map<String, TeleportedModeParams> getTeleportedModeParams() {
		return getModeRoutingParams();
	}
	/**
	 * @deprecated -- use {@link #getTeleportedModeParams()} instead.
	 */
	public Map<String, TeleportedModeParams> getModeRoutingParams() {
		final Map<String, TeleportedModeParams> map = new LinkedHashMap< >();

		for ( ConfigGroup pars : getParameterSets( TeleportedModeParams.SET_TYPE ) ) {
			if ( this.isLocked() ) {
				pars.setLocked();
			}
			final String mode = ((TeleportedModeParams) pars).getMode();
			final TeleportedModeParams old = map.put( mode , (TeleportedModeParams)	pars );
			if ( old != null ) throw new IllegalStateException( "several parameter sets for mode "+mode );
		}

		return map;
	}

	public TeleportedModeParams getOrCreateModeRoutingParams( final String mode ) {
		TeleportedModeParams pars = getModeRoutingParams().get( mode );

		if ( pars == null ) {
			pars = (TeleportedModeParams) createParameterSet( TeleportedModeParams.SET_TYPE );
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
		} else if (key.startsWith(TELEPORTED_MODE_FREESPEED_FACTORS)){
			setTeleportedModeFreespeedFactor( key.substring( TELEPORTED_MODE_FREESPEED_FACTORS.length() ), Double.parseDouble( value ) );
		} else if ( CLEAR_MODE_ROUTING_PARAMS.equals( key ) ){
			this.setClearingDefaultModeRoutingParams( Boolean.parseBoolean( value ) );
		} else if (RANDOMNESS.equals( key ) ) {
			this.setRoutingRandomness( Double.parseDouble( value ) );
		}
		else if (ACCESSEGRESSTYPE.equals( key ) ) {
			this.setAccessEgressType(AccessEgressType.valueOf(value));
		} else if (NETWORK_ROUTE_CONSISTENCY_CHECK.equals(key)){
			this.setNetworkRouteConsistencyCheck(NetworkRouteConsistencyCheck.valueOf(value));
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final Map<String, String> getParams() {
		Map<String, String> map = super.getParams();
		map.put( NETWORK_MODES, CollectionUtils.arrayToString(this.networkModes.toArray( new String[0] ) ) );
		map.put(  CLEAR_MODE_ROUTING_PARAMS, Boolean.toString( this.clearingDefaultModeRoutingParams ) ) ;
		map.put(  RANDOMNESS, Double.toString( this.routingRandomness ) ) ;
		map.put(  ACCESSEGRESSTYPE, getAccessEgressType().toString()) ;
		map.put(NETWORK_ROUTE_CONSISTENCY_CHECK, NetworkRouteConsistencyCheck.abortOnInconsistency.toString());
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();

		map.put(BEELINE_DISTANCE_FACTOR, "factor with which beeline distances (and therefore times) " +
				"are multiplied in order to obtain an estimate of the network distances/times.  Default is something like 1.3") ;
		map.put(NETWORK_MODES, "All the modes for which the router is supposed to generate network routes (like car)") ;
		map.put(RANDOMNESS, "strength of the randomness for the utility of money in routing under toll.  "
	          		+ "Leads to Pareto-optimal route with randomly drawn money-vs-other-attributes tradeoff. "
	          		+ "Technically the width parameter of a log-normal distribution. 3.0 seems to be a good value. " ) ;
		map.put( CLEAR_MODE_ROUTING_PARAMS, CLEAR_MODE_ROUTING_PARAMS_CMT ) ;
		map.put(ACCESSEGRESSTYPE, ACCESSEGRESSTYPE_CMT);
		map.put(NETWORK_ROUTE_CONSISTENCY_CHECK, "Defines whether the network consistency should be checked.");
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
		for ( TeleportedModeParams pars : getModeRoutingParams().values() ) {
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
		for ( TeleportedModeParams pars : getModeRoutingParams().values() ) {
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
		for ( TeleportedModeParams pars : getModeRoutingParams().values() ) {
			if ( this.isLocked() ) {
				pars.setLocked();
			}

			final Double val = pars.getBeelineDistanceFactor() ;
			if ( val != null ) map.put( pars.getMode() , val ) ;
		}
		return map ;
	}

	@Deprecated // rather use addModeRoutingParams(...), since that allows further params (e.g. beeline distance factor). kai, nov'19
	public void setTeleportedModeFreespeedFactor(String mode, double freespeedFactor) {
		testForLocked() ;
		// re-create, to trigger erasing of defaults (see acceptModeParamsWithoutClearing)
		final TeleportedModeParams pars = new TeleportedModeParams( mode );
		pars.setTeleportedModeFreespeedFactor( freespeedFactor );
		addParameterSet( pars );
	}

	@Deprecated // rather use addModeRoutingParams(...), since that allows further params (e.g. beeline distance factor). kai, nov'19
	public void setTeleportedModeSpeed(String mode, double speed) {
		testForLocked() ;
		// re-create, to trigger erasing of defaults (see acceptModeParamsWithoutClearing)
		final TeleportedModeParams pars = new TeleportedModeParams( mode );
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
		for ( TeleportedModeParams params : this.getModeRoutingParams().values() ) {
			params.setBeelineDistanceFactor( val );
		}
	}


	@StringGetter(ACCESSEGRESSTYPE)
	public AccessEgressType getAccessEgressType() {
		return this.accessEgressType;
	}

	@StringSetter(ACCESSEGRESSTYPE)
	public void setAccessEgressType(AccessEgressType accessEgressType) {
		this.accessEgressType = accessEgressType;
	}

	@StringGetter(RANDOMNESS)
	public double getRoutingRandomness() {
		return routingRandomness;
	}
	@StringSetter(RANDOMNESS)
	public void setRoutingRandomness(double routingRandomness) {
		this.routingRandomness = routingRandomness;
	}

	@StringGetter(NETWORK_ROUTE_CONSISTENCY_CHECK)
	public NetworkRouteConsistencyCheck getNetworkRouteConsistencyCheck() {
		return networkRouteConsistencyCheck;
	}

	@StringSetter(NETWORK_ROUTE_CONSISTENCY_CHECK)
	public void setNetworkRouteConsistencyCheck(NetworkRouteConsistencyCheck networkRouteConsistencyCheck) {
		this.networkRouteConsistencyCheck = networkRouteConsistencyCheck;
	}

	@Override protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Set<String> modesRoutedAsTeleportation = this.getModeRoutingParams().keySet();
		Collection<String> modesRoutedAsNetworkModes = this.getNetworkModes();

		for( String mode : modesRoutedAsTeleportation ){
			if ( modesRoutedAsNetworkModes.contains( mode ) ) {
				throw new RuntimeException( "mode \"" + mode + "\" is defined both as teleportation (mode routing param) and for network routing.  You need to remove " +
										"one or the other.") ;
			}
		}

	}

	public void printModeRoutingParams(){
		for( Map.Entry<String, TeleportedModeParams> entry : this.getModeRoutingParams().entrySet() ){
			log.warn( "key=" + entry.getKey() + "; value=" + entry.getValue() );
		}
	}

}
