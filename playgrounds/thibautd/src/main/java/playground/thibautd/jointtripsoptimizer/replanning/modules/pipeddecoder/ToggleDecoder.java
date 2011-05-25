/* *********************************************************************** *
 * project: org.matsim.*
 * ToggleDecoder.java
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtripsoptimizer.population.IdLeg;
import playground.thibautd.jointtripsoptimizer.population.JointActing;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Decoder for the engagement/desengagement of individuals in a shared ride.
 * The resulting plan has the following flaws, to be corrected in the following
 * decoders (say, the duration decoder):
 * -no synchronisation
 * -inconsistent origin/destination in legs
 *
 *  the correct parameters are:
 *  -the affectation of activities and legs
 *  -the links between legs (ie legs of unengaged individuals are not referenced)
 * @author thibautd
 */
public class ToggleDecoder implements JointPlanOptimizerDimensionDecoder {
	private static final Logger log =
		Logger.getLogger(ToggleDecoder.class);

	private static String DEFAULT_REMP_MODE = TransportMode.pt;

	private final JointPlan plan;
	private final List<PlanElement> jointPlanElements;
	private final Map<PlanElement, Tuple<PlanElement, PlanElement>> sharedRideOD =
		new HashMap<PlanElement, Tuple<PlanElement, PlanElement>>();
	private final Map<PlanElement, Integer> associatedGene =
		new HashMap<PlanElement, Integer>();
	private final List<IdLeg> unaffectedLegs =
		new ArrayList<IdLeg>();

	//private static Integer NO_GENE = null; 

	/*
	 * =========================================================================
	 * constructor and associated methods
	 * =========================================================================
	 */
	public ToggleDecoder(final JointPlan plan) {
		this.plan = plan;
		this.jointPlanElements = plan.getPlanElements();
		this.constructODAssociation();
		this.constructGeneAssociation();
	}

	// OD association
	/**
	 * associates to each shared leg an origin and a destination.
	 * The current rules are used:
	 * -for the driver, origin and destination are the "real" ones (that is, non
	 *  PU or DO activities).
	 * -for the passenger, the origin is the first PU of the chain, and the destination
	 *  the last DO of the chain.
	 *
	 *  aim: at the end, all PU (resp. DO) that do not correspond to a planned origin
	 *  (resp. destination) must be removed.
	 */
	private void constructODAssociation() {
		PlanElement currentOrigin = null;
		PlanElement currentDestination = null;
		List<PlanElement> toAssociate = new ArrayList<PlanElement>();

		for (PlanElement pe : this.jointPlanElements) {
			if (pe instanceof JointActivity) {
				if (((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP)) {
					currentOrigin = (currentOrigin==null ? pe : currentOrigin);
				}
				if (((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF)) {
					currentDestination = pe;
				}
			}
			else {
				if (((JointLeg) pe).getJoint()) {
					toAssociate.add(pe);
				}
				else if (currentDestination != null) {
					associateOD(toAssociate, currentOrigin, currentDestination);

					currentOrigin = null;
					currentDestination = null;
					toAssociate.clear();
				}
			}
		}
	}

	private void associateOD(
			final List<PlanElement> toAssociate,
			final PlanElement currentOrigin,
			final PlanElement currentDestination) {
		for (PlanElement sharedLeg : toAssociate) {
			this.sharedRideOD.put(
					sharedLeg,
					new Tuple<PlanElement, PlanElement>(
						currentOrigin,
						currentDestination)
					);
		}
	}

	// Gene Association
	/**
	 * Associates a gene to each toggable plan element.
	 * Toggable elements are all elements related to a passenger trip.
	 */
	private void constructGeneAssociation() {
		int i;
		int currentGene = -1;
		List<PlanElement> currentPlanElements;
		PlanElement currentPlanElement;
		boolean inSharedTrip = false;

		for (Plan individualPlan : this.plan.getIndividualPlans().values()) {
			currentPlanElements = individualPlan.getPlanElements();
			for (i = 0; i < currentPlanElements.size(); i++) {
				currentPlanElement = currentPlanElements.get(i);

				if (currentPlanElement instanceof JointLeg) {
					if (isAccessLeg(currentPlanElement, currentPlanElements, i)) {
						// change gene, as we enter a new shared trip
						currentGene++;
						inSharedTrip = true;
						this.associatedGene.put(currentPlanElement, currentGene);
					}
					else if (isPassengerLeg(currentPlanElement)) {
						this.associatedGene.put(currentPlanElement, currentGene);
					}
					else if (isEgressLeg(currentPlanElement, currentPlanElements, i)) {
						this.associatedGene.put(currentPlanElement, currentGene);
						inSharedTrip = false;
					}
				}
				else if (inSharedTrip) {
					this.associatedGene.put(currentPlanElement, currentGene);
				}
			}
		}
	}

	/**
	 * checks if a leg is an access leg to a passenger ride.
	 */
	private boolean isAccessLeg(
			final PlanElement currentPlanElement,
			final List<PlanElement> currentPlanElements,
			int i) {
		return
			// the trip is not shared
			((!((JointLeg) currentPlanElement).getJoint()) &&
			// it goes to a PU activity
			(((JointActivity) currentPlanElements.get(i+1)).getType().equals(
				JointActingTypes.PICK_UP)) &&
			// and the PU is followed by a passenger trip.
			(((JointLeg) currentPlanElements.get(i+2)).getMode().equals(
				JointActingTypes.PASSENGER)));
	}

	private boolean isPassengerLeg(PlanElement pe) {
		return ((JointLeg) pe).getMode().equals(JointActingTypes.PASSENGER);
	}

	/**
	 * checks if a leg is an egress leg from a passenger ride.
	 */
	private boolean isEgressLeg(
			final PlanElement currentPlanElement,
			final List<PlanElement> currentPlanElements,
			int i) {
		return
			// the trip is not shared
			((!((JointLeg) currentPlanElement).getJoint()) &&
			// it comes from a DO activity
			(((JointActivity) currentPlanElements.get(i-1)).getType().equals(
				JointActingTypes.DROP_OFF)) &&
			// and the DO was accessed by a passenger trip.
			(((JointLeg) currentPlanElements.get(i-2)).getMode().equals(
				JointActingTypes.PASSENGER)));
	}

	/*
	 * =========================================================================
	 * decoding methods
	 * =========================================================================
	 */

	/**
	 * {@inheritDoc}
	 * @see JointPlanOptimizerDimensionDecoder#decode(IChromosome,JointPlan)
	 */
	@Override
	public JointPlan decode(
			final IChromosome chromosome,
			final JointPlan inputPlan) {
		//check wether the plan corresponds to the initial plan
		//If it was not the case, there would be no assurance that the genes always
		//have the same sense.
		if (inputPlan != this.plan) {
			throw new IllegalArgumentException("the toggle decoder must be run first");
		}

		//JointPlan outputPlan = createCoherentPlan(decodeToggle(chromosome));
		//return outputPlan;
		return createCoherentPlan(decodeToggle(chromosome));
	}

	/**
	 * Assumes that the boolean genes are at the beginning of the chromosome.
	 */
	private List<Boolean> extractGeneValues(final IChromosome chromosome) {
		Gene[] genes = chromosome.getGenes();
		List<Boolean> output = new ArrayList<Boolean>();

		for (Gene gene : genes) {
			if (gene instanceof BooleanGene) {
				output.add(((BooleanGene) gene).booleanValue());
			}
		}

		return output;
	}

	/**
	 * Simply iterates over plan elements and affect them according to the
	 * value of the related chromosome.
	 * Do not try to make something coherent:
	 * -all legs between 2 activities are removed if the joint leg is untoggled
	 * -PU/leg/DO that do not correspond anymore to a useful O/D are kept
	 */
	private Map<Id, List<PlanElement>> decodeToggle(final IChromosome chromosome) {
		List<Boolean> geneValues = extractGeneValues(chromosome);
		Map<Id, List<PlanElement>> constructedIndividualPlans =
			new HashMap<Id, List<PlanElement>>();
		List<PlanElement> currentList;

		this.unaffectedLegs.clear();

		for (Map.Entry<Id, Plan> individualPlan :
				this.plan.getIndividualPlans().entrySet()) {

			currentList = new ArrayList<PlanElement>();
			constructedIndividualPlans.put(
					individualPlan.getKey(),
					currentList);

			for (PlanElement pe : individualPlan.getValue().getPlanElements()) {
				if (toPlan(pe, geneValues)) {
					currentList.add(pe);
				}
				else if (pe instanceof JointLeg) {
					this.unaffectedLegs.add(((JointLeg) pe).getId());
				}
			}
		}

		return constructedIndividualPlans;
	}

	private boolean toPlan(
			final PlanElement pe,
			final List<Boolean> geneValues) {
		return (this.associatedGene.containsKey(pe) ?
				geneValues.get(this.associatedGene.get(pe)) :
				true);
	}

	/**
	 * corrects the incoherences in the plan created by the decodeToggle method.
	 */
	private JointPlan createCoherentPlan(final Map<Id, List<PlanElement>> incoherentPlan) {
		List<PlanElement> plannedSharedLegs = identifySharedLegs(incoherentPlan);
		Map<Id, PlanImpl> constructedIndividualPlans = new HashMap<Id, PlanImpl>();

		for (Map.Entry<Id, List<PlanElement>> individualPlan :
				incoherentPlan.entrySet()) {
			constructedIndividualPlans.put(
					individualPlan.getKey(), 
					constructPlan(
						individualPlan,
						plannedSharedLegs));
		}

		correctLegLinks(constructedIndividualPlans);

		return new JointPlan(
				this.plan.getClique(),
				constructedIndividualPlans,
				false, //do not add at individual level
				false, //do not synchronize
				this.plan.getScoresAggregatorFactory());
	}

	private List<PlanElement> identifySharedLegs(final Map<Id, List<PlanElement>> incoherentPlan) {
		List<PlanElement> output = new ArrayList<PlanElement>();

		for (List<PlanElement> planElements : incoherentPlan.values()) {
			for (PlanElement pe : planElements) {
				if ( (pe instanceof JointLeg) && (((JointLeg) pe).getJoint()) ) {
					output.add(pe);
				}
			}
		}

		return output;
	}

	private PlanImpl constructPlan(
			final Map.Entry<Id, List<PlanElement>> planEntry,
			final List<PlanElement> plannedSharedLegs) {
		List<PlanElement> correctPlan = new ArrayList<PlanElement>();
		List<PlanElement> incorrectPlan = planEntry.getValue();
		PlanElement currentPlanElement;

		for (int i=0; i < incorrectPlan.size(); i++) {
			currentPlanElement = incorrectPlan.get(i);
			if (currentPlanElement instanceof JointActivity) {
				i += planActivity(
						(JointActivity) currentPlanElement,
						i,
						incorrectPlan,
						correctPlan,
						plannedSharedLegs);
			}
			else {
				i += planLeg(
						(JointLeg) currentPlanElement,
						i,
						incorrectPlan,
						correctPlan,
						plannedSharedLegs);
			}
		}

		return fromPlanElementsToPlan(planEntry.getKey(), correctPlan);
	}

	private PlanImpl fromPlanElementsToPlan(
			final Id id,
			final List<PlanElement> pes) {
		PlanImpl output = new PlanImpl(this.plan.getClique().getMembers().get(id));

		for (PlanElement pe : pes) {
			if (pe instanceof Activity) {
				output.addActivity((Activity) pe);
			}
			else {
				output.addLeg((Leg) pe);
			}
		}

		return output;
	}

	/**
	 * Plans an activity, and the following leg if a shared ride is to reimplace.
	 * The legs reimplacing shared rides are pt legs. Other modes are possible
	 * only if the mode is optimized: this part is left to the ModeDecoder.
	 *
	 * @return the number of plan elements to ignore
	 */
	private int planActivity(
			final JointActivity act, 
			final int i,
			final List<PlanElement> incorrectPlan,
			final List<PlanElement> correctPlan,
			final List<PlanElement> plannedSharedLegs) {
		if (act.getType().equals(JointActingTypes.PICK_UP)) {
			JointLeg leg = (JointLeg) incorrectPlan.get(i+1);
			if (isUsefulPU(
						leg,
						plannedSharedLegs)) {
				correctPlan.add(new JointActivity(act));
				correctPlan.add(new JointLeg(leg));
				return 1;
			}
			else {
				//PU useless: don't plan it nor its associated leg
				return 1;
			}
		}
		else if (act.getType().equals(JointActingTypes.DROP_OFF)) {
			if (isUsefulDO(
						(JointLeg) incorrectPlan.get(i-1),
						plannedSharedLegs)) {
				correctPlan.add(new JointActivity(act));
				correctPlan.add(new JointLeg((JointLeg) incorrectPlan.get(i+1)));
				return 1;
			}
			else {
				//PU useless: don't plan it nor its associated leg
				return 1;
			}
		}
		else if ((i < incorrectPlan.size() - 1) &&
				(incorrectPlan.get(i+1) instanceof JointActivity)) {
			// case of an unaffected shared leg
			correctPlan.add(new JointActivity(act));
			correctPlan.add(new JointLeg(DEFAULT_REMP_MODE, act.getPerson()));
			return 0;
		}
		else {
			correctPlan.add(new JointActivity(act));
			return 0;
		}
	}

	private boolean isUsefulPU(
			final JointLeg leg, 
			final List<PlanElement> plannedSharedLegs) {
		int index;
		PlanElement pickUp;

		//is it useful for this leg?
		index = this.jointPlanElements.indexOf(leg);
		pickUp = this.jointPlanElements.get(index - 1);
		if ((!leg.getIsDriver()) &&
					(pickUp == this.sharedRideOD.get(leg).getFirst())) {
			// the individual to which this PU is useful travels
			return true;
		}

		//The PU does not correspond to this leg. Perhaps another planned leg?
		for (JointLeg linkedLeg : leg.getLinkedElements().values()) {
			if (plannedSharedLegs.contains(linkedLeg)) {
				index = jointPlanElements.indexOf(linkedLeg);
				pickUp = jointPlanElements.get(index - 1);
				if ((!linkedLeg.getIsDriver()) &&
							(pickUp == this.sharedRideOD.get(linkedLeg).getFirst())) {
					// the individual to which this PU is useful travels
					return true;
				}
			}
		}

		// we didn't found anybody departing from this PU
		return false;
	}

	private boolean isUsefulDO(
			final JointLeg leg,
			final List<PlanElement> plannedSharedLegs) {
		int index;
		PlanElement dropOff;

		//is it useful for this leg?
		index = jointPlanElements.indexOf(leg);
		dropOff = jointPlanElements.get(index + 1);
		if ((!leg.getIsDriver()) &&
				(dropOff == this.sharedRideOD.get(leg).getSecond())) {
			return true;
		}

		//The DO does not correspond to this leg. Perhaps another planned leg?
		for (JointLeg linkedLeg : leg.getLinkedElements().values()) {
			if (plannedSharedLegs.contains(linkedLeg)) {
				index = jointPlanElements.indexOf(linkedLeg);
				dropOff = jointPlanElements.get(index + 1);
				if ((!linkedLeg.getIsDriver()) &&
						(dropOff == this.sharedRideOD.get(linkedLeg).getSecond())) {
					// the individual to which this DO is useful travels
					return true;
				}
			}
		}

		// we didn't found anybody going to this DO
		return false;
	}

	private int planLeg(
			final JointLeg leg, 
			final int i,
			final List<PlanElement> incorrectPlan,
			final List<PlanElement> correctPlan,
			final List<PlanElement> plannedSharedLegs) {
		correctPlan.add(new JointLeg(leg));
		return 0;
	}

	private void correctLegLinks(final Map<Id, PlanImpl> individualPlans) {
		List<IdLeg> toRemove = new ArrayList<IdLeg>();

		for (PlanImpl individualPlan : individualPlans.values()) {
			for (PlanElement pe : individualPlan.getPlanElements()) {
				if (((JointActing) pe).getJoint()) {
					toRemove.clear();
					for (IdLeg linkedElement :
							// only legs should be joint
							((JointLeg) pe).getLinkedElementsIds()) {
						if (this.unaffectedLegs.contains(linkedElement)) {
							toRemove.add(linkedElement);
						}
					}
					((JointLeg) pe).getLinkedElementsIds().removeAll(toRemove);
				}
			}
		}
	}

}

