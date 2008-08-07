/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcSocialNet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.deprecated;

import org.matsim.config.groups.SocNetConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PersonAlgorithmI;
import org.matsim.population.algorithms.PlansAlgorithm;

public class PlansCalcSocialNet extends PlansAlgorithm implements
		PersonAlgorithmI {
	private String sNAlgorithmName_;
	private String linkStrengthAlgorithm_;
	private String linkRemovalCondition_;
	
	public PlansCalcSocialNet() {
		// TODO Auto-generated constructor stub
		super();
	}

	@Override
	public void run(Population plans) {
		// TODO lots: need to iterate as follows: build, remove, build, remove, ...
		// Maybe put in a call to "build" in which the parameters for the
		// construction algorithm are sorted out and in which the algorithm
		// is executed. But how to determine the number of iterations and how to call remove?
		// Also, put in a method called "remove" which is only the chosen removal algorithm
		// (overloads? or if statements?)

		// Establish which algorithm to use to build the social network.
		// Note these are construction, not microscopic, algorithms, though some
		// may iterate.
		// Evaluate the config parameters and call method.
		sNAlgorithmName_ = Gbl.getConfig().socnetmodule().getSocNetAlgo();
		if (sNAlgorithmName_.equals("random")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			randomSocialNetwork(plans);
		} else if (sNAlgorithmName_.equals("wattssmallworld")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			wattsSocialNetwork(plans);
		} else if (sNAlgorithmName_.equals("jingirnew")) {
			System.out.println("Setting up the " + sNAlgorithmName_
					+ " algorithm.");
			jGNSocialNetwork(plans);
		} else {
			Gbl
					.errorMsg(this.getClass()
							+ ".run(). Social Network Algorithm > "
							+ sNAlgorithmName_
							+ " < is not known. Poor choice of input parameter in module "
							+ SocNetConfigGroup.GROUP_NAME
							+ ". Check spelling or choose from: random, wattssmallworld, jingirnew");
		}
		// TODO at this point add a distinction between construction and
		// behavioral (microscopic) social network algorithms. The microscopic
		// algorithms will change activity plans and iterate over the results of the
		// route choices (travel costs) and link removal policy. This is a much more
		// complicated step.

		// Establish the link strength function from config parameters and call
		// method (move this to a method that is called from each of the construction
		// algorithms).
		linkStrengthAlgorithm_ = Gbl.getConfig().socnetmodule().getSocNetLinkStrengthAlgo();
		System.out
				.println(this.getClass() + ".run() " + linkStrengthAlgorithm_);

		// Establish the link removal policy from config parameters and call
		// method
		linkRemovalCondition_ = Gbl.getConfig().socnetmodule().getSocNetLinkRemovalAlgo();
		System.out.println(this.getClass() + ".run() " + linkRemovalCondition_);

		// From here to is for a routine which chooses agents randomly
		Object[] a = plans.getPersons().values().toArray();
		// Object[] b = plans.getPersons().keySet().toArray();

		int numPersons = a.length;
		double pctToUse = 0.05;
		int numToUse = (int) (pctToUse * numPersons);
		System.out.println(" " + numToUse + " &&&&&&&&&&&&&&");
		// for (int i = 0; i < numToUse; i++) {
		// Person myRandomPerson = (Person) a[Gbl.random.nextInt(a.length)];
		// // Each person has just 1 plan but the loop is here just in case
		// // this
		// // changes
		// ArrayList personPlans_ = myRandomPerson.getPlans();
		// int numPersonPlans_ = (int) personPlans_.size();
		// System.out.println("Person " + myRandomPerson.getId() + " has "
		// + numPersonPlans_ + " plans.");
		// for (int j = 0; j < numPersonPlans_; j++) {
		// Plan plan = (Plan) myRandomPerson.getPlans().get(j);
		// System.out.println("Plan " + j + " has "
		// + plan.getActsLegs().size() + " legs.");
		// int max = plan.getActsLegs().size();
		//
		// // Calculate and set the activity duration where possible
		//
		// Act firstAct = (Act) (plan.getActsLegs().get(0)); // a special
		// // treatment for
		// // the first act
		// if (firstAct.getStartTime() == Gbl.UNDEFINED_TIME) {
		// firstAct.setStartTime(0); // start at midnight
		// }
		// for (int jj = 0; jj < max; jj += 2) {
		// Act act_ = (Act) (plan.getActsLegs().get(jj));
		// double dur = act_.getDur();
		// if (dur == Gbl.UNDEFINED_TIME) {
		// double start = act_.getStartTime();
		// double end = act_.getEndTime();
		// if (start != Gbl.UNDEFINED_TIME
		// && end != Gbl.UNDEFINED_TIME) {
		// act_.setDur(end - start);
		// }
		// }
		// System.out.println("\t " + act_.getType() + " at "
		// + act_.getLink().getId() + " lasts "
		// + act_.getDur() / 3600. + " from "
		// + act_.getStartTime() / 3600. + " to "
		// + act_.getEndTime() / 3600.);
		// }
		// }
		// }

	}

	private void jGNSocialNetwork(Population plans) {
		// TODO test the parameters and construct a Jin Girvan Newman social
		// network with a link probability between agents of pBernoulli and a
		// probability of friend-of-friend interaction pFoF:
		// Parameter pBernoulli and p Friend of Friend, distance between homes
		// if "geo" param
		System.out.println(this.getClass()
				+ ".jGNSocialNetwork is not written yet.");
	}

	private void wattsSocialNetwork(Population plans) {
		// TODO test the parameters and construct a Watts (1999) small world
		// social
		// network with a link probability between agents and average degree (?)
		// Parameters p, z, distance between homes if "geo" param
		System.out.println(this.getClass()
				+ ".wattsSocialNetwork is not written yet.");
	}

	private void randomSocialNetwork(Population plans) {
		// TODO test the parameters and construct a random (bernoulli) social
		// network with a link probability between agents of pBernoulli:
		// Parameter pBernoulli, no distance consideration possible
		System.out.println(this.getClass()
				+ ".randomSocialNetwork is not written yet.");
	}

	public void run(Person person) {
		// TODO Auto-generated method stub
		// Implementation of PersonAlgorithm

	}
}
