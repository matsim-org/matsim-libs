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

import java.lang.reflect.Field;
import java.lang.String;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.TransportMode;
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
	 * the drop-off activity duration, in seconds
	 * NOT USED
	 */
	private static final String DO_DUR = "dropOffDuration";
	private static final String MUTATION_PROB = "mutationProbability";
	private static final String WHOLE_CO_PROB = "WholeArithmeticalCrossOverProbability";
	private static final String SIMPLE_CO_PROB = "SimpleArithmeticalCrossOverProbability";
	private static final String SINGLE_CO_PROB = "SingleArithmeticalCrossOverProbability";
	private static final String ITER_NUM = "maxNumberOfGAIterations";
	private static final String NON_UNIFORMITY_PARAM = "mutationNonUniformity";
	private static final String OPTIMIZE_TOGGLE = "toggleToOptimize";
	private static final String SELECTION_THRESHOLD = "bestSelectionThreshold";
	private static final String PLOT_FITNESS = "plotFitnessEvolution";
	private static final String OPTIMIZE_MODE = "modeToOptimize";
	private static final String AVAIL_MODES = "availableModes";
	private static final String ITER_MIN_NUM = "minNumberOfGAIterations";
	private static final String MONITORING_PERIOD = "fitnessMonitoringPeriod";
	private static final String DO_MONITOR = "fitnessToMonitor";
	private static final String MIN_IMPROVEMENT = "minimumFitnessImprovementCHF";

	//parameter values, initialized to defaults.
	private int numTimeIntervals;
	private int populationSize = 10;
	private double dropOffDuration = 0;
	private double mutationProb = 0.1;
	private double wholeCrossOverProb = 0.5;
	private double simpleCrossOverProb = 0.5;
	private double singleCrossOverProb = 0.5;
	private int numberOfIterations = 100;
	private double betaNonUniformity = 1;
	private boolean optimizeToggle = false;
	private double selectionThreshold = 0.1d;
	private boolean plotFitness = false;
	private boolean optimizeMode = false;
	private List<String> availableModes = null;
	private int minNumberOfIterations = 0;
	private int monitoringPeriod = 5;
	private boolean doMonitor = true;
	private double minImprovement = 1d;

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
		else if (param_name.equals(WHOLE_CO_PROB)) {
			this.setWholeCrossOverProbability(value);
		}
		else if (param_name.equals(SIMPLE_CO_PROB)) {
			this.setSimpleCrossOverProbability(value);
		}
		else if (param_name.equals(SINGLE_CO_PROB)) {
			this.setSingleCrossOverProbability(value);
		}
		else if (param_name.equals(ITER_NUM)) {
			this.setMaxIterations(value);
		}
		else if (param_name.equals(NON_UNIFORMITY_PARAM)) {
			this.setMutationNonUniformity(value);
		}
		else if (param_name.equals(OPTIMIZE_TOGGLE)) {
			this.setOptimizeToggle(value);
		}
		else if (param_name.equals(SELECTION_THRESHOLD)) {
			this.setSelectionThreshold(value);
		}
		else if (param_name.equals(PLOT_FITNESS)) {
			this.setPlotFitness(value);
		}
		else if (param_name.equals(OPTIMIZE_MODE)) {
			this.setModeToOptimize(value);
		}
		else if (param_name.equals(AVAIL_MODES)) {
			this.setAvailableModes(value);
		}
		else if (param_name.equals(ITER_MIN_NUM)) {
			this.setMinIterations(value);
		}
		else if (param_name.equals(MONITORING_PERIOD)) {
			this.setMonitoringPeriod(value);
		}
		else if (param_name.equals(DO_MONITOR)) {
			this.setFitnessToMonitor(value);
		}
		else if (param_name.equals(MIN_IMPROVEMENT)) {
			this.setMinImprovement(value);
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
		else if (param_name.equals(WHOLE_CO_PROB)) {
			return String.valueOf(this.getWholeCrossOverProbability());
		}
		else if (param_name.equals(SIMPLE_CO_PROB)) {
			return String.valueOf(this.getSimpleCrossOverProbability());
		}
		else if (param_name.equals(SINGLE_CO_PROB)) {
			return String.valueOf(this.getSingleCrossOverProbability());
		}
		else if (param_name.equals(ITER_NUM)) {
			return String.valueOf(this.getMaxIterations());
		}
		else if (param_name.equals(NON_UNIFORMITY_PARAM)) {
			return String.valueOf(this.getMutationNonUniformity());
		}
		else if (param_name.equals(OPTIMIZE_TOGGLE)) {
			return String.valueOf(this.getOptimizeToggle());
		}
		else if (param_name.equals(SELECTION_THRESHOLD)) {
			return String.valueOf(this.getSelectionThreshold());
		}
		else if (param_name.equals(PLOT_FITNESS)) {
			return String.valueOf(this.getPlotFitness());
		}
		else if (param_name.equals(OPTIMIZE_MODE)) {
			return String.valueOf(this.getModeToOptimize());
		}
		else if (param_name.equals(AVAIL_MODES)) {
			//TODO: do not produce an "inputable" value
			return String.valueOf(this.getAvailableModes());
		}
		else if (param_name.equals(ITER_MIN_NUM)) {
			return String.valueOf(this.getMinIterations());
		}
		else if (param_name.equals(MONITORING_PERIOD)) {
			return String.valueOf(this.getMonitoringPeriod());
		}
		else if (param_name.equals(DO_MONITOR)) {
			return String.valueOf(this.getFitnessToMonitor());
		}
		else if (param_name.equals(MIN_IMPROVEMENT)) {
			return String.valueOf(this.getMinImprovement());
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
		this.addParameterToMap(map, WHOLE_CO_PROB);
		this.addParameterToMap(map, SIMPLE_CO_PROB);
		this.addParameterToMap(map, SINGLE_CO_PROB);
		this.addParameterToMap(map, ITER_NUM);
		this.addParameterToMap(map, NON_UNIFORMITY_PARAM);
		this.addParameterToMap(map, OPTIMIZE_TOGGLE);
		this.addParameterToMap(map, SELECTION_THRESHOLD);
		this.addParameterToMap(map, PLOT_FITNESS);
		this.addParameterToMap(map, OPTIMIZE_MODE);
		this.addParameterToMap(map, AVAIL_MODES);
		this.addParameterToMap(map, ITER_MIN_NUM);
		this.addParameterToMap(map, MONITORING_PERIOD);
		this.addParameterToMap(map, DO_MONITOR);
		this.addParameterToMap(map, MIN_IMPROVEMENT);
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

	public double getDropOffDuration() {
		return this.dropOffDuration;
	}
	
	public void setDropOffDuration(String dropOffDuration) {
		this.dropOffDuration = Double.valueOf(dropOffDuration);
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

	public double getWholeCrossOverProbability() {
		return this.wholeCrossOverProb;
	}

	public void setWholeCrossOverProbability(String coProb) {
		this.wholeCrossOverProb = Double.valueOf(coProb);

		if ((this.wholeCrossOverProb < 0)||(this.wholeCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public double getSingleCrossOverProbability() {
		return this.singleCrossOverProb;
	}

	public void setSingleCrossOverProbability(String coProb) {
		this.singleCrossOverProb = Double.valueOf(coProb);

		if ((this.singleCrossOverProb < 0)||(this.singleCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public double getSimpleCrossOverProbability() {
		return this.simpleCrossOverProb;
	}

	public void setSimpleCrossOverProbability(String coProb) {
		this.simpleCrossOverProb = Double.valueOf(coProb);

		if ((this.simpleCrossOverProb < 0)||(this.simpleCrossOverProb > 1)) {
			throw new IllegalArgumentException("probability values must in [0,1]");
		}
	}

	public int getMaxIterations() {
		return this.numberOfIterations;
	}

	public void setMaxIterations(String iterations) {
		this.numberOfIterations = Integer.parseInt(iterations);

		if (this.numberOfIterations < 0) {
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

	public boolean getOptimizeToggle() {
		return this.optimizeToggle;
	}

	public void setOptimizeToggle(String value) {
		if (value.toLowerCase().equals("true")) {
			this.optimizeToggle = true;
		}
		else if (value.toLowerCase().equals("false")) {
			this.optimizeToggle = false;
		}
		else {
			throw new IllegalArgumentException("value for "+
					OPTIMIZE_TOGGLE+" must be \"true\" or \"false\"");
		}
	}

	public double getSelectionThreshold() {
		return this.selectionThreshold;
	}

	public void setSelectionThreshold(String value) {
		this.selectionThreshold = Double.parseDouble(value);

		if ((this.selectionThreshold < 0d)||(this.selectionThreshold > 1d)) {
			throw new IllegalArgumentException("the selection threshold must belong"
					+" to [0,1]!");
		}
	}

	public boolean getPlotFitness() {
		return this.plotFitness;
	}

	public void setPlotFitness(String value) {
		if (value.toLowerCase().equals("true")) {
			this.plotFitness = true;
		}
		else if (value.toLowerCase().equals("false")) {
			this.plotFitness = false;
		}
		else {
			throw new IllegalArgumentException("value for "+
					OPTIMIZE_TOGGLE+" must be \"true\" or \"false\"");
		}
	}

	public List<String> getAvailableModes() {
		if (this.availableModes == null) {
			log.warn("modes available for the optimisation initialized to the "+
					"set of all available values");
			this.availableModes = getAllModes();
		}
		return this.availableModes;
	}

	public void setAvailableModes(String value) {
		String[] modes = value.split(",");

		//List<String> allModes = getAllModes();
		this.availableModes = new ArrayList<String>();

		for (String mode : modes) {
		//	if (allModes.contains(mode)) {
			try {
				this.availableModes.add((String)
						TransportMode.class.getField(mode).get(null));
			}
			//else {
			catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("unrecognized mode: "+mode);
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("error while iterating over "+
						"TransportMode fields");
			}
		}
	}

	private static List<String> getAllModes() {
		List<String> out = new ArrayList<String>();

		//iterate over all public fields of transport mode
		//TODO: more precise catches.
		for (Field field : TransportMode.class.getFields()) {
			try {
				out.add((String) field.get(null));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("error while iterating over "+
						"TransportMode fields");
			}
		}

		return out;
	}

	public void setModeToOptimize(String value) {
		this.optimizeMode = Boolean.valueOf(value);
	}

	public boolean getModeToOptimize() {
		return this.optimizeMode;
	}

	public void setFitnessToMonitor(String value) {
		this.doMonitor = Boolean.parseBoolean(value);
	}

	public boolean getFitnessToMonitor() {
		return this.doMonitor;
	}

	public void setMinIterations(String value) {
		this.minNumberOfIterations = Integer.valueOf(value);
	}

	public int getMinIterations() {
		return this.minNumberOfIterations;
	}

	public void setMonitoringPeriod(String value) {
		this.monitoringPeriod = Integer.valueOf(value);
	}

	public int getMonitoringPeriod() {
		return this.monitoringPeriod;
	}

	public void setMinImprovement(String value) {
		this.minImprovement = Double.parseDouble(value);
	}

	public double getMinImprovement() {
		return this.minImprovement;
	}
}

