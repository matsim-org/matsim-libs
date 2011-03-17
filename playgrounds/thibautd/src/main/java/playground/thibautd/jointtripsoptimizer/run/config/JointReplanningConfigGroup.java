/* *********************************************************************** *
 * project: org.matsim.*
 * JointReplanningConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.run.config;

import java.lang.String;

import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.core.config.Module;

/**
 * @author thibautd
 */
public class JointReplanningConfigGroup extends Module {

	private static final Logger log = Logger.getLogger(JointReplanningConfigGroup.class);

	private static final long serialVersionUID = 1L;
	public static final String GROUP_NAME = "JointReplanning";

	//parameter names
	private static final String NUM_TIME_INTERVALS = "numTimeIntervals";
	private static final String POP_SIZE = "gaPopulationSize";
	/**
	 * the drop-off activity duration, in time intervals
	 */
	private static final String DO_DUR = "dropOffDuration";
	private static final String MUTATION_PROB = "mutationProbability";
	private static final String CO_PROB = "crossOverProbability";
	private static final String ITER_NUM = "maxNumberOfGAIterations";
	private static final String NON_UNIFORMITY_PARAM = "mutationNonUniformity";

	//parameter values
	private int numTimeIntervals;
	private int populationSize;
	private int dropOffDuration;
	private double mutationProb;
	private double crossOverProb;
	private int numberOfIterations;
	private double betaNonUniformity;

	public JointReplanningConfigGroup() {
		super(GROUP_NAME);
		log.debug("joint replanning config group initialized");
	}

	/*
	 * =========================================================================
	 * base class methods
	 * =========================================================================
	 */
	@Override
	public void addParam(String param_name, String value) {
		if (param_name.equals(NUM_TIME_INTERVALS)) {
			this.setNumTimeIntervals(value);
		}
		else if (param_name.equals(POP_SIZE)) {
			this.setPopulationSize(value);
		}
		else if (param_name.equals(DO_DUR)) {
			this.setDropOffDuration(value);
		}
		else if (param_name.equals(MUTATION_PROB)) {
			this.setMutationProbability(value);
		}
		else if (param_name.equals(CO_PROB)) {
			this.setCrossOverProbability(value);
		}
		else if (param_name.equals(ITER_NUM)) {
			this.setMaxIterations(value);
		}
		else if (param_name.equals(NON_UNIFORMITY_PARAM)) {
			this.setMutationNonUniformity(value);
		}
	}

	@Override
	public String getValue(String param_name) {
		if (param_name.equals(NUM_TIME_INTERVALS)) {
			return String.valueOf(this.getNumTimeIntervals());
		}
		else if (param_name.equals(POP_SIZE)) {
			return String.valueOf(this.getPopulationSize());
		}
		else if (param_name.equals(DO_DUR)) {
			return String.valueOf(this.getPopulationSize());
		}
		else if (param_name.equals(MUTATION_PROB)) {
			return String.valueOf(this.getMutationProbability());
		}
		else if (param_name.equals(CO_PROB)) {
			return String.valueOf(this.getCrossOverProbability());
		}
		else if (param_name.equals(ITER_NUM)) {
			return String.valueOf(this.getMaxIterations());
		}
		else if (param_name.equals(NON_UNIFORMITY_PARAM)) {
			this.getMutationNonUniformity();
		}
		return null;
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		this.addParameterToMap(map, NUM_TIME_INTERVALS);
		this.addParameterToMap(map, POP_SIZE);
		this.addParameterToMap(map, DO_DUR);
		this.addParameterToMap(map, MUTATION_PROB);
		this.addParameterToMap(map, CO_PROB);
		this.addParameterToMap(map, ITER_NUM);
		this.addParameterToMap(map, NON_UNIFORMITY_PARAM);
		return map;
	}

	/*
	 * =========================================================================
	 * getters/setters
	 * =========================================================================
	 */

	public int getNumTimeIntervals() {
		return this.numTimeIntervals;
	}

	public void setNumTimeIntervals(String numTimeIntervals) {
		this.numTimeIntervals = Integer.parseInt(numTimeIntervals);
	}

	public int getPopulationSize() {
		return this.populationSize;
	}

	public void setPopulationSize(String populationSize) {
		this.populationSize = Integer.parseInt(populationSize);
	}

	public int getDropOffDuration() {
		return this.dropOffDuration;
	}
	
	public void setDropOffDuration(String dropOffDuration) {
		this.dropOffDuration = Integer.parseInt(dropOffDuration);
	}

	public double getMutationProbability() {
		return this.mutationProb;
	}

	public void setMutationProbability(String mutationProb) {
		this.mutationProb = Double.valueOf(mutationProb);

		if ((this.mutationProb < 0)||(this.mutationProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public double getCrossOverProbability() {
		return this.crossOverProb;
	}

	public void setCrossOverProbability(String coProb) {
		this.crossOverProb = Double.valueOf(coProb);

		if ((this.crossOverProb < 0)||(this.crossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public int getMaxIterations() {
		return this.numberOfIterations;
	}

	public void setMaxIterations(String iterations) {
		this.numberOfIterations = Integer.parseInt(iterations);

		if (this.crossOverProb < 0) {
			throw new IllegalArgumentException("number of iterations must be positive");
		}
	}

	public double getMutationNonUniformity() {
		return this.betaNonUniformity;
	}

	public void setMutationNonUniformity(String beta) {
		this.betaNonUniformity = Double.valueOf(beta);

		if (this.betaNonUniformity <= 0d) {
			throw new IllegalArgumentException("non uniformity mutation parameter"+
					" must be positive");
		}
	}
}

