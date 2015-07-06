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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Allows to specify parameters useful for any use case
 * @author thibautd
 */
public class SocialNetworkGenerationConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "socialNetworkGeneration";

	private String inputPopulationFile = null;
	private String outputDirectory = null;
	private int stepSizePrimary = 1;
	private int stepSizeSecondary = 1;

	private double targetDegree = 22.0;
	private double targetClustering = 0.206;

	private int iterationsToTarget = 2;

	private double initialPrimaryThreshold = -8.8;
	private double initialSecondaryReduction = 230;

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

	@StringGetter( "stepSizePrimary" )
	public int getStepSizePrimary() {
		return stepSizePrimary;
	}

	@StringSetter( "stepSizePrimary" )
	public void setStepSizePrimary( int stepSizePrimary ) {
		this.stepSizePrimary = stepSizePrimary;
	}

	@StringGetter( "stepSizeSecondary" )
	public int getStepSizeSecondary() {
		return stepSizeSecondary;
	}

	@StringSetter( "stepSizeSecondary" )
	public void setStepSizeSecondary( int stepSizeSecondary ) {
		this.stepSizeSecondary = stepSizeSecondary;
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

	@StringGetter( "iterationsToTarget" )
	public int getIterationsToTarget() {
		return iterationsToTarget;
	}

	@StringSetter( "iterationsToTarget" )
	public void setIterationsToTarget( int iterationsToTarget ) {
		this.iterationsToTarget = iterationsToTarget;
	}

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
	public void setInitialSecondaryReduction( double initialSecondaryThreshold ) {
		this.initialSecondaryReduction = initialSecondaryThreshold;
	}

}

