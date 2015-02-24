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

	private double initialPrimaryStep = 10;
	private double initialSecondaryStep = 10;

	private double precisionClustering = 1E-3;
	private double precisionDegree = 1E-2;

	private double targetDegree = 22.0;
	private double targetClustering = 0.206;

	private double samplingRateForClusteringEstimation = 0.1;

	private int stagnationLimit = 10;
	private int maxIterations = 500;

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

	@StringGetter( "samplingRateForClusteringEstimation" )
	public double getSamplingRateForClusteringEstimation() {
		return samplingRateForClusteringEstimation;
	}

	@StringSetter( "samplingRateForClusteringEstimation" )
	public void setSamplingRateForClusteringEstimation( double rate ) {
		if ( rate < 0 || rate > 1 ) throw new IllegalArgumentException( rate+" is not in [0;1]" );
		this.samplingRateForClusteringEstimation = rate;
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

	@StringGetter( "precisionClustering" )
	public double getPrecisionClustering() {
		return precisionClustering;
	}

	@StringSetter( "precisionClustering" )
	public void setPrecisionClustering( double precisionClustering ) {
		this.precisionClustering = precisionClustering;
	}

	@StringGetter( "precisionDegree" )
	public double getPrecisionDegree() {
		return precisionDegree;
	}

	@StringSetter( "precisionDegree" )
	public void setPrecisionDegree( double precisionDegree ) {
		this.precisionDegree = precisionDegree;
	}

	@StringGetter( "initialPrimaryStep" )
	public double getInitialPrimaryStep() {
		return initialPrimaryStep;
	}

	@StringSetter( "initialPrimaryStep" )
	public void setInitialPrimaryStep( double initialPrimaryStep ) {
		this.initialPrimaryStep = initialPrimaryStep;
	}

	@StringGetter( "initialSecondaryStep" )
	public double getInitialSecondaryStep() {
		return initialSecondaryStep;
	}

	@StringSetter( "initialSecondaryStep" )
	public void setInitialSecondaryStep( double initialSecondaryStep ) {
		this.initialSecondaryStep = initialSecondaryStep;
	}

	@StringGetter( "stagnationLimit" )
	public int getStagnationLimit() {
		return stagnationLimit;
	}

	@StringSetter( "stagnationLimit" )
	public void setStagnationLimit( int stagnationLimit ) {
		this.stagnationLimit = stagnationLimit;
	}

	@StringGetter( "maxIterations" )
	public int getMaxIterations() {
		return maxIterations;
	}

	@StringSetter( "maxIterations" )
	public void setMaxIterations( int maxIterations ) {
		this.maxIterations = maxIterations;
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
			this.initialSecondaryReduction = initialSecondaryReduction;
		}

	}
}

