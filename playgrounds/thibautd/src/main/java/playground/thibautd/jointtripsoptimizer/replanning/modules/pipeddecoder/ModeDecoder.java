/* *********************************************************************** *
 * project: org.matsim.*
 * ModeDecoder.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jgap.IChromosome;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPModeGene;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * For decoding the mode.
 * @author thibautd
 */
public class ModeDecoder implements JointPlanOptimizerDimensionDecoder {
	private static final Logger log =
		Logger.getLogger(ModeDecoder.class);


	// STATIC
	private static final String DRIVER_LABEL = "driverLabel";
	private static final String PASSENGER_LABEL = "passengerLabel";
	private static final String FREE_LABEL = "freeLabel";

	// index and "mode" of the "root" tour (ie, the father of the "from home to
	// home" tour(s))
	private static final int NO_FATHER = Integer.MIN_VALUE;
	// mode of the "root" tour, that is, the (dummy) father tour of home-based
	// tours
	private static final String ROOT_MODE = "rootMode";

	// TODO: let possibility of defining it from the config group
	private static final String[] CHAIN_BASED_MODES = {
		TransportMode.car, 
		TransportMode.bike};

	//other private fields
	/**
	 * remembers the Id of the current "anchor point" were the personal vehicle is.
	 * the anchor point may be a facility of a link (link in this implementation).
	 */
	private final Map<Id, List<Integer>> individualGenesIndices =
		new HashMap<Id, List<Integer>>();
	private final PlanAnalyzeSubtours analyseSubtours =
		new PlanAnalyzeSubtours();
	
	private int[] orderedSubtourIndices = null;

	/*
	 * =========================================================================
	 * constructor
	 * =========================================================================
	 */
	public ModeDecoder(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final int nToggleGenes,
			final int nDurGenes) {
		//this.firstModeGene = nToggleGenes + nDurGenes;
		// initialize the indices of the genes relative to individuals
		int currentIndex = nToggleGenes + nDurGenes;
		int lastIndividualIndex = currentIndex;
		List<Integer> individualIndices;

		this.analyseSubtours.setTripStructureAnalysisLayer(
				configGroup.getTripStructureAnalysisLayer());

		for (Map.Entry<Id, Plan> individualPlan : 
				plan.getIndividualPlans().entrySet()) {
			this.analyseSubtours.run(individualPlan.getValue());
			lastIndividualIndex += this.analyseSubtours.getNumSubtours();
			individualIndices = new ArrayList<Integer>();

			for (; currentIndex < lastIndividualIndex; currentIndex++) {
				individualIndices.add(new Integer(currentIndex));
			}

			this.individualGenesIndices.put(
					individualPlan.getKey(),
					individualIndices);
		}
	}

	/*
	 * =========================================================================
	 * decoding methods
	 * =========================================================================
	 */
	/**
	 * Takes a "toggled" joint plan and returns the same plan, with legs set.
	 */
	@Override
	public JointPlan decode(
			final IChromosome chromosome,
			final JointPlan inputPlan) {
		Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		for (Map.Entry<Id, Plan> planAttributes :
				inputPlan.getIndividualPlans().entrySet()) {
			individualPlans.put(
					planAttributes.getKey(),
					decodeIndividualPlan(
						getIndividualGenes(
							planAttributes.getKey(),
							chromosome),
						planAttributes.getValue()));
		}

		return new JointPlan(
				inputPlan.getClique(),
				individualPlans,
				false, //do not add at individual level
				false, //do not synchronize
				inputPlan.getScoresAggregatorFactory());
	}

	/**
	 * Extracts the genes relative to the individual's mode choice from the
	 * full Chromosome.
	 */
	private List<JointPlanOptimizerJGAPModeGene> getIndividualGenes(
			final Id id, 
			final IChromosome chromosome) {
		List<Integer> toExtract = this.individualGenesIndices.get(id);
		List<JointPlanOptimizerJGAPModeGene> output =
			new ArrayList<JointPlanOptimizerJGAPModeGene>(toExtract.size());

		for (int index : toExtract) {
			output.add((JointPlanOptimizerJGAPModeGene) chromosome.getGene(index));
		}

		return output;
	}

	/**
	 * Given the mode genes and the plan, sets the modes to a feasible chain.
	 */
	private Plan decodeIndividualPlan(
			final List<JointPlanOptimizerJGAPModeGene> modeGenes,
			final Plan individualPlan) {
		this.analyseSubtours.run(individualPlan);
		int[] subtourIndexation = this.analyseSubtours.getSubtourIndexation();
		int legIndex = 0;
		String[] subtourModes;
		// TODO: move at a higher level (ie at the construction)
		boolean hasCar = 
			!"never".equals(((PersonImpl) individualPlan.getPerson()).getCarAvail());

		Plan output = new PlanImpl(individualPlan.getPerson());
		List<PlanElement> planElements = individualPlan.getPlanElements();

		//int subtourIndex;
		String mode;
		JointLeg legToAdd;

		//construct the label and father tables
		String[] subtourLabels = getSubtourLabels(planElements);
		int[] fatherTable = getFatherTable();

		subtourModes = getModeChoice(
				modeGenes,
				subtourLabels,
				fatherTable,
				hasCar);

		// iterate over the plan elements, setting the mode according to the genes.
		for (PlanElement pe : planElements) {
			if (pe instanceof JointActivity) {
				output.addActivity(new JointActivity((JointActivity) pe));
			}
			else if (pe instanceof JointLeg) {
				mode = subtourModes[subtourIndexation[legIndex]];

				legToAdd = new JointLeg((JointLeg) pe);
				if ((legToAdd.getMode() != JointActingTypes.PASSENGER) &&
						(!mode.equals(legToAdd.getMode()))) {
					legToAdd.setMode(mode);
					legToAdd.setRoute(null);
				}
				output.addLeg(legToAdd);

				legIndex++;
			}
			else {
				throw new IllegalArgumentException("unexpected plan element type");
			}
		}

		return output;
	}

	/**
	 * @return a list of the modes, the ith element being the mode for the
	 * subtour with index i
	 */
	private String[] getModeChoice(
			final List<JointPlanOptimizerJGAPModeGene> modeGenes,
			final String[] subtourLabels,
			final int[] fatherTable,
			final boolean hasCar) {
		int nSubtours = this.analyseSubtours.getNumSubtours();
		String[] output = new String[nSubtours];
		List<String> currentGeneValue;
		String fatherMode;

		for (int i : this.orderedSubtourIndices) {
			currentGeneValue = modeGenes.get(i).getListValue();

			//get "father" mode:
			fatherMode = (fatherTable[i] == NO_FATHER ?
					ROOT_MODE :
					output[fatherTable[i]]);

			// affect the first feasible mode in the ordered list
			for (String mode : currentGeneValue) {
				//test mode:
				if (isFeasibleMode(
							mode,
							subtourLabels[i],
							fatherMode,
							hasCar)) {
					output[i] = mode;
					break;
				}
			}
			
			if (output[i]==null) {
				throw new RuntimeException("No feasible mode were found");
			}
		}

		return output;
	}

	private boolean isFeasibleMode(
			final String mode,
			final String subtourLabel,
			final String fatherMode,
			final boolean hasCar) {
		if (!isChainBased(mode)) {
			if ((subtourLabel.equals(FREE_LABEL)) ||
					(subtourLabel.equals(PASSENGER_LABEL))){
				return true;
			}

			return false;
		}

		return isFeasibleChainMode(mode, subtourLabel, fatherMode, hasCar);
	}

	private boolean isFeasibleChainMode(
			final String mode,
			final String subtourLabel,
			final String fatherMode,
			final boolean hasCar) {
		if (subtourLabel.equals(DRIVER_LABEL)) {
			return (mode.equals(TransportMode.car));
		}
		if (mode.equals(TransportMode.car) && (!hasCar)) {
			return false;
		}

		boolean notAPassengerSubtour = (!subtourLabel.equals(PASSENGER_LABEL));
		boolean vehicleAvailable = fatherMode.equals(ROOT_MODE) ||
			mode.equals(fatherMode);
		return notAPassengerSubtour && vehicleAvailable;
	}

	private boolean isChainBased(final String mode) {
		for (String chainedMode : CHAIN_BASED_MODES) {
			if (mode.equals(chainedMode)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Associates a "label" to each subtour. possible values:
	 * 1) driver: the individual is driver in a joint trip in this sub-tour
	 *    or one of its "children".
	 * 2) passenger: the individual is passenger in a shared ride in this
	 *    sub-tour.
	 * 3) free: neither 1) nor 2).
	 *
	 * note that 1) and 2) are incompatible, as 1) requires to use the car in the
	 * subtour, while 2) requires not to use a chain-based mode: if the both are
	 * to be set for the same subtour, an IllegalArgumentException is thrown.
	 */
	private String[] getSubtourLabels(
			final List<PlanElement> planElements) {
		int nSubTours = this.analyseSubtours.getNumSubtours();
		// "origins" of the subtours
		List<Integer> originActs = this.analyseSubtours.getFromIndexOfSubtours();
		// "destinations" of the subtours (in fact, last activity of a subtour: the
		// next link also pertains to the subtour
		List<Integer> destinationActs = this.analyseSubtours.getToIndexOfSubtours();
		List<List<PlanElement>> subtours = this.analyseSubtours.getSubtours();
		List<PlanElement> currentSubtour;
		boolean driver;
		boolean passenger;
		//Map<Integer, String> output = 
		//	new HashMap<Integer, String>(nSubTours);
		String[] output = new String[nSubTours];

		//iterate over subtours
		for (int i=0; i < nSubTours; i++) {
			currentSubtour = subtours.get(i);
			driver = false;
			passenger = false;

			// iterate over subtour legs and its children
			for (int j = originActs.get(i) + 1;
					j < destinationActs.get(i) + 1;
					j += 2) {
				if (isDriver(planElements.get(j))) {
					driver = true;
				}
				if (isPassenger(planElements.get(j), currentSubtour)) {
					passenger = true;
				}
			}

			// check consistency
			if (driver && passenger) {
				throw new IllegalArgumentException("the optimized plan has an "+
						"inconsistent joint structure");
			}
			else if (driver) {
				//output.put(i, DRIVER_LABEL);
				output[i] = DRIVER_LABEL;
			}
			else if (passenger) {
				//output.put(i, PASSENGER_LABEL);
				output[i] = PASSENGER_LABEL;
			}
			else {
				//output.put(i, FREE_LABEL);
				output[i] = FREE_LABEL;
			}
		}

		return output;
	}

	/**
	 * @return true if the leg corresponds to a shared ride driver leg.
	 */
	private boolean isDriver(final PlanElement pe) {
		return ((JointLeg) pe).getIsDriver();
	}

	/**
	 * @return true if the leg is a passenger leg belonging to the given subtour.
	 */
	private boolean isPassenger(
			final PlanElement pe,
			final List<PlanElement> subtour) {
		boolean isPassenger = ( ((JointLeg) pe).getJoint() &&
			(!((JointLeg) pe).getIsDriver()) );
		return isPassenger && subtour.contains(pe);
	}

	/**
	 * Constructs a table of the "father" subtour of every subtour.
	 * By "father" subtour is meant the littlest subtour to which pertains the
	 * anchor point.
	 * The "root" subtours (ie the home-anchored tours) are labeled with
	 * ModeDecoder.NO_FATHER
	 */
	private int[] getFatherTable() {
		int nSubTours = this.analyseSubtours.getNumSubtours();
		int[] output = new int[nSubTours];
		List<Integer> alreadyHasFather = new ArrayList<Integer>(nSubTours);
		int[] subtourStruct = this.analyseSubtours.getSubtourIndexation();
		int currentSubTour;
		int count = 0;

		this.orderedSubtourIndices = new int[nSubTours];

		// TODO: affect NO_FATHER to ALL home-based tours
		output[subtourStruct[0]] = NO_FATHER;
		this.orderedSubtourIndices[count] = subtourStruct[0];
		alreadyHasFather.add(subtourStruct[0]);
		count++;

		for (int i=1; i < subtourStruct.length; i++) {
			if (count == nSubTours) {
				//each subtour has been examined, we can stop
				break;
			}

			currentSubTour = subtourStruct[i];

			if (!(alreadyHasFather.contains(currentSubTour))) {
				// As we go through legs in chronological order, the father
				// of a subtour is the one to which pertains the leg preceding
				// this subtour.
				output[currentSubTour] = subtourStruct[i -1];
				this.orderedSubtourIndices[count] = currentSubTour;
				alreadyHasFather.add(currentSubTour);
				count++;
			}
		}

		return output;
	}
}

