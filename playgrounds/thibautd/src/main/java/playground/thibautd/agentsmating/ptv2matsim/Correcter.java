/* *********************************************************************** *
 * project: org.matsim.*
 * Correcter.java
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
package playground.thibautd.agentsmating.ptv2matsim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;

/**
 * Aims at correcting inconsistencies in the mode chain of plans generated with
 * PTV data.
 *
 * @author thibautd
 */
public class Correcter {
	private static final Logger log =
		Logger.getLogger(Correcter.class);

	private static final String DEFAULT_MODE = TransportMode.pt;

	private final PopulationWithCliques population;
	private final Map<Id, List<Id>> cliques;
	private final PlanAnalyzeSubtours subtoursAnalysis = new PlanAnalyzeSubtours();

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public Correcter(final PopulationWithCliques population) {
		this.population = population;
		this.cliques = extractCliques(population);
	}

	private Map<Id, List<Id>> extractCliques(final PopulationWithCliques population) {
		Map<Id, List<Id>> out = new HashMap<Id, List<Id>>();
		List<Id> members;

		for (Map.Entry<Id, ? extends Clique> entry : population.getCliques().getCliques().entrySet()) {
			members = new ArrayList<Id>(entry.getValue().getMembers().keySet());
			out.put(entry.getKey(), members);
		}

		return out;
	}

	// /////////////////////////////////////////////////////////////////////////
	// correction methods
	// /////////////////////////////////////////////////////////////////////////
	public void run() {
		JointPlan plan;
		List<JointLeg> legsToRemove = new ArrayList<JointLeg>();
		List<Id> cliquesToCorrect = new ArrayList<Id>();
		boolean alreadyAdded;
		int count = 0;

		for (Clique clique : this.population.getCliques().getCliques().values()) {
			alreadyAdded = false;
			plan = (JointPlan) clique.getSelectedPlan();

			// while (!isValidPlan(plan)) {
			while(removeFirstWrongLegFound(plan)) {
				if (!alreadyAdded) {
					cliquesToCorrect.add(clique.getId());
					alreadyAdded = true;
				}

				count++;
			}

			legsToRemove.clear();
		}

		log.info(count+" joint trips removed");
		correctCliques(cliquesToCorrect);
	}

	//////////////////////////// correction helpers \\\\\\\\\\\\\\\\\\\\\\\\\\\
	private boolean isValidPlan(final JointPlan plan) {
		for (Plan indivPlan : plan.getIndividualPlans().values()) {
			if (!isValidIndividualPlan(indivPlan)) {
				return false;
			}
		}

		return true;
	}

	private boolean isValidIndividualPlan(final Plan indivPlan) {
		this.subtoursAnalysis.run(indivPlan);
		int nSubTours = this.subtoursAnalysis.getNumSubtours();
		// "origins" of the subtours
		List<Integer> originActs = this.subtoursAnalysis.getFromIndexOfSubtours();
		// "destinations" of the subtours (in fact, last activity of a subtour: the
		// next link also pertains to the subtour
		List<Integer> destinationActs = this.subtoursAnalysis.getToIndexOfSubtours();
		List<List<PlanElement>> subtours = this.subtoursAnalysis.getSubtours();
		List<PlanElement> currentSubtour;
		List<PlanElement> planElements = indivPlan.getPlanElements();
		boolean driver;
		boolean passenger;

		//iterate over subtours
		for (int i=0; i < nSubTours; i++) {
			currentSubtour = subtours.get(i);
			driver = false;
			passenger = false;

			// iterate over subtour legs and its children's
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
				return false;
			}
		}

		return true;
	}

	/**
	 * @return true if an invalid leg was found and removed, false otherwise
	 */
	private boolean removeFirstWrongLegFound(final JointPlan plan) {
		JointLeg invalidSharedLeg = null;

		for (Plan indivPlan : plan.getIndividualPlans().values()) {
			invalidSharedLeg = getInvalidSharedLeg(indivPlan);

			if (invalidSharedLeg != null) {
				break;
			}
		}

		if (invalidSharedLeg != null) {
			removeLeg(plan, invalidSharedLeg);
			return true;
		}

		return false;
	}

	private void removeLeg(
			final JointPlan plan,
			final JointLeg invalidLeg) {
		Map<Id, JointLeg> jointLegsToRemove = invalidLeg.getLinkedElements();
		Id id;
		JointLeg leg;
		List<PlanElement> planElements;
		int index;

		for (Map.Entry<Id, JointLeg> entry : jointLegsToRemove.entrySet()) {
			id = entry.getKey();
			leg = entry.getValue();

			planElements = plan.getIndividualPlan(id).getPlanElements();

			index = planElements.indexOf(leg);

			planElements.set(index + 2, new JointLeg(DEFAULT_MODE, leg.getPerson()));
			planElements.subList(index - 2, index + 2).clear();
		}

		planElements = plan.getIndividualPlan(invalidLeg.getPerson().getId()).getPlanElements();

		index = planElements.indexOf(invalidLeg);

		planElements.set(index + 2, new JointLeg(DEFAULT_MODE, invalidLeg.getPerson()));
		planElements.subList(index - 2, index + 2).clear();
	}

	private JointLeg getInvalidSharedLeg(final Plan indivPlan) {
		this.subtoursAnalysis.run(indivPlan);
		int nSubTours = this.subtoursAnalysis.getNumSubtours();
		// "origins" of the subtours
		List<Integer> originActs = this.subtoursAnalysis.getFromIndexOfSubtours();
		// "destinations" of the subtours (in fact, last activity of a subtour: the
		// next link also pertains to the subtour
		List<Integer> destinationActs = this.subtoursAnalysis.getToIndexOfSubtours();
		List<List<PlanElement>> subtours = this.subtoursAnalysis.getSubtours();
		List<PlanElement> currentSubtour;
		List<PlanElement> planElements = indivPlan.getPlanElements();
		boolean driver;
		boolean passenger;
		JointLeg leg;

		//iterate over subtours
		for (int i=0; i < nSubTours; i++) {
			currentSubtour = subtours.get(i);
			driver = false;
			passenger = false;

			// iterate over subtour legs and its children's
			for (int j = originActs.get(i) + 1;
					j < destinationActs.get(i) + 1;
					j += 2) {
				leg = (JointLeg) planElements.get(j);
				if (isDriver(leg)) {
					if (passenger) {
						// inconsistency!
						return leg;
					}
					driver = true;
				}
				if (isPassenger(leg, currentSubtour)) {
					if (driver) {
						//incosistency!
						return leg;
					}
					passenger = true;
				}
			}
		}

		return null;
	}

	private void correctCliques(final List<Id> cliquesToCorrect) {
		IdFactory idFactory = new IdFactory(this.cliques);
		List<List<Id>> correctedCliques;

		for (Id id : cliquesToCorrect) {
			correctedCliques = getConnexComponents(this.cliques.remove(id));

			for (List<Id> clique : correctedCliques) {
				this.cliques.put(idFactory.createId(), clique);
			}
		}
	}

	private List<List<Id>> getConnexComponents(final List<Id> clique) {
		List<List<Id>> out = new ArrayList<List<Id>>();

		while (clique.size() > 0) {
			out.add(inDepthSearch(clique.get(0), clique));
		}

		return out;
	}

	/**
	 * Performs an in depth search to construct cliques.
	 */
	private List<Id> inDepthSearch(final Id member, final List<Id> clique) {
		List<Id> out = new ArrayList<Id>();
		clique.remove(member);
		
		for (Id neighboor : getMates(member)) {
			if (clique.contains(neighboor)) {
				out = inDepthSearch(neighboor, clique);
			}
		}
		
		out.add(member);
		return out;
	}

	private List<Id> getMates(final Id person) {
		List<Id> out = new ArrayList<Id>();
		List<PlanElement> indivPlan = population.getPersons().get(person).getSelectedPlan().getPlanElements();
		JointLeg leg;

		for (int i=1; i < indivPlan.size(); i+=2) {
			leg = (JointLeg) indivPlan.get(i);

			if (leg.getJoint()) {
				for (Id mate : leg.getLinkedElements().keySet()) {
					if (!out.contains(mate)) {
						out.add(mate);
					}
				}
			}
		}

		return out;
	}

	/////////////////////// subtour analysis methods \\\\\\\\\\\\\\\\\\\\\\\\\\\
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

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public Map<Id, List<Id>> getCliques() {
		return this.cliques;
	}

	// /////////////////////////////////////////////////////////////////////////
	// various helpers
	// /////////////////////////////////////////////////////////////////////////
	private class IdFactory {
		private long currentId;

		public IdFactory(Map<Id, ? extends Object> cliques) {
			Id maxId = Collections.max(cliques.keySet(), new IdComparator());
			currentId = Long.parseLong(maxId.toString());
		}

		public Id createId() {
			currentId++;
			return new IdImpl(currentId);
		}
	}

	private class IdComparator implements Comparator<Id> {
		@Override
		public int compare(final Id arg0, final Id arg1) {
			long long0 = Long.parseLong(arg0.toString());
			long long1 = Long.parseLong(arg1.toString());
			return (int) (long0 - long1);
		}
	}
}

