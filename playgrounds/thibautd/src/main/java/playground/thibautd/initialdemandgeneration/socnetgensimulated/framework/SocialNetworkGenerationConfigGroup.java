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

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Allows to specify parameters useful for any use case
 * @author thibautd
 */
public class SocialNetworkGenerationConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "socialNetworkGeneration";

	private String inputPopulationFile = null;
	private String outputDirectory = null;

	private double initialPrimaryThreshold = Double.NaN;
	private double initialSecondaryReduction = Double.NaN;

	private double precisionClustering = 1E-3;
	private double precisionDegree = 1E-2;

	private double targetDegree = 22.0;
	private double targetClustering = 0.206;

	private double powellMinAbsoluteChange = 1E-9;
	private double powellMinRelativeChange = 1E-9;

	private int maxIterations = 10000;

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

	@StringGetter( "maxIterations" )
	public int getMaxIterations() {
		return maxIterations;
	}

	@StringSetter( "maxIterations" )
	public void setMaxIterations( int maxIterations ) {
		this.maxIterations = maxIterations;
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
	public void setInitialSecondaryReduction( double initialSecondaryReduction ) {
		this.initialSecondaryReduction = initialSecondaryReduction;
	}

	@StringGetter( "powellMinAbsoluteChange" )
	public double getPowellMinAbsoluteChange() {
		return powellMinAbsoluteChange;
	}

	@StringSetter( "powellMinAbsoluteChange" )
	public void setPowellMinAbsoluteChange( double powellMinAbsoluteChange ) {
		this.powellMinAbsoluteChange = powellMinAbsoluteChange;
	}

	@StringGetter( "powellMinRelativeChange" )
	public double getPowellMinRelativeChange() {
		return powellMinRelativeChange;
	}

	@StringSetter( "powellMinRelativeChange" )
	public void setPowellMinRelativeChange( double powellMinRelativeChange ) {
		this.powellMinRelativeChange = powellMinRelativeChange;
	}
}

