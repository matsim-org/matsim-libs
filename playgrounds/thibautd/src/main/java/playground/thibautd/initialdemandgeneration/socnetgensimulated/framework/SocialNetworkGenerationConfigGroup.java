/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkGenerationConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;

/**
 * Allows to specify parameters useful for any use case
 * @author thibautd
 */
public class SocialNetworkGenerationConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "socialNetworkGeneration";

	private String inputPopulationFile = null;
	private String outputDirectory = null;

	private double targetDegree = 22.0;
	private double targetClustering = 0.206;

	public SocialNetworkGenerationConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter( "inputPopulationFile" )
	public String getInputPopulationFile() {
		return inputPopulationFile;
	}

	@StringSetter( "inputPopulationFile" )
	public void setInputPopulationFile( String inputPopulationFile ) {
		this.inputPopulationFile = inputPopulationFile;
	}

	@StringGetter( "outputDirectory" )
	public String getOutputDirectory() {
		return outputDirectory;
	}

	@StringSetter( "outputDirectory" )
	public void setOutputDirectory( String outputDirectory ) {
		this.outputDirectory = outputDirectory;
	}

	@StringGetter( "targetDegree" )
	public double getTargetDegree() {
		return targetDegree;
	}

	@StringSetter( "targetDegree" )
	public void setTargetDegree( double targetDegree ) {
		this.targetDegree = targetDegree;
	}

	@StringGetter( "targetClustering" )
	public double getTargetClustering() {
		return targetClustering;
	}

	@StringSetter( "targetClustering" )
	public void setTargetClustering( double targetClustering ) {
		this.targetClustering = targetClustering;
	}

	@Override
	public ConfigGroup createParameterSet( final String type ) {
		if ( !type.equals( InitialPointParameterSet.SET_TYPE ) ) throw new IllegalArgumentException( type );
		return new InitialPointParameterSet();
	}

	public Collection<Thresholds> getInitialPoints() {
		final List<Thresholds> ts = new ArrayList< >();
		for ( InitialPointParameterSet set : (Collection<InitialPointParameterSet>) getParameterSets( InitialPointParameterSet.SET_TYPE ) ) {
			ts.add( new Thresholds( set.getInitialPrimaryThreshold() , set.getInitialSecondaryReduction() ) );
		}
		return ts;
	}

	public static class InitialPointParameterSet extends ReflectiveConfigGroup {
		public static final String SET_TYPE = "initialPoint";

		public InitialPointParameterSet( ) {
			super( SET_TYPE );
		}

		private double initialPrimaryThreshold = -8.8;
		private double initialSecondaryReduction = 230;

		@StringGetter( "initialPrimaryThreshold" )
		public double getInitialPrimaryThreshold() {
			return initialPrimaryThreshold;
		}

		@StringSetter( "initialPrimaryThreshold" )
		public void setInitialPrimaryThreshold( double initialPrimaryThreshold ) {
			this.initialPrimaryThreshold = initialPrimaryThreshold;
		}

		@StringGetter( "initialSecondaryReduction" )
		public double getInitialSecondaryReduction() {
			return initialSecondaryReduction;
		}

		@StringSetter( "initialSecondaryReduction" )
		public void setInitialSecondaryReduction( double initialSecondaryReduction ) {
			if ( initialSecondaryReduction < 0 ) throw new IllegalArgumentException( "secondary reduction must be positive, got "+initialSecondaryReduction );
			this.initialSecondaryReduction = initialSecondaryReduction;
		}

	}
}

