/* *********************************************************************** *
 * project: org.matsim.*
 * ExpBetaPlanChanger.java
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

package playground.meisterk.eaptus2010;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * Changes to another plan if that plan is better.  Probability to change depends on score difference.
 *
 * @author kn based on mrieser
 */
public class ExpBetaPlanChanger2 implements PlanSelector {
	private static final Logger log = Logger.getLogger(ExpBetaPlanChanger2.class);

	private final double beta;
	static boolean betaFlag = true;

	/**
	 * stores the agents and their decicions for one iteration:
	 * 1 stands for the decision of the person of changing the plan, 0 stands for not changing the plan
	 */
	private HashMap<Person, Double> tableOfAgentsAndDecisions = new HashMap<Person, Double>();

	public Map<Person, Double> getTableOfAgentsAndDecisions() {
		return Collections.unmodifiableMap(this.tableOfAgentsAndDecisions);
	}


	public void clearTableOfAgentsAndDecisions(){
		tableOfAgentsAndDecisions.clear();
	}


	public ExpBetaPlanChanger2(double beta) {
		this.beta = beta;
	}

	/**
	 * Changes to another plan with a probability proportional to exp( Delta scores ).
	 * Need to think through if this goes to Nash Equilibrium or to SUE !!!
	 */
	@Override
	public Plan selectPlan(final Person person) {
		// current plan and random plan:
		Plan currentPlan = person.getSelectedPlan();
		Plan otherPlan = ((PersonImpl) person).getRandomPlan();

		if (currentPlan == null) {
			// this case should only happen when the agent has no plans at all
			return null;
		}

		double currentScore = currentPlan.getScore();
		double otherScore = otherPlan.getScore();

		if ( betaFlag ) {
//			System.err.println( "ExpBetaPlanChanger: The following beta should be replaced by beta/2.  Not fatal.") ; // ask kai.  Jul08
//			System.err.println( "(This has now been done.  If you have used expBetaPlanChanger before, double the beta in your config file.)") ; // ask kai.  Jul08
			log.warn("Would make sense to revise this once more.  See comments in code.  kai, nov08") ;
			/*** Gunnar says, rightly I think, that what is below hits the "0.01*weight > 1" threshold fairly quickly.
			 *   An alternative might be to divide by exp(0.5*beta*oS)+exp(0.5*beta*cS), or the max of these two numbers.  But:
			 *   (1) someone would need to go through the theory to make sure that we remain within what we have said before
			 *       (convergence to logit and proba of jump between equal options = 0.01
			 *   (2) someone would need to test if the "traffic" results are similar
			 */
			betaFlag = false ;
		}
		double weight = Math.exp( 0.5 * this.beta * (otherScore - currentScore) );
		// (so far, this is >1 if otherScore>currentScore, and <=1 otherwise)
		// (beta is the slope (strength) of the operation: large beta means strong reaction)

		// The change probability is
		double changeProb = weight*0.01;



		if (MatsimRandom.getRandom().nextDouble() < 0.01*weight ) { // as of now, 0.01 is hardcoded (proba to change when both
			                                           // scores are the same)

			//If the person changes the plan, assign 1 to this person
			tableOfAgentsAndDecisions.put(person, new Double(1));


			return otherPlan;


		}

		tableOfAgentsAndDecisions.put(person, new Double(0));

		return currentPlan;
	}



}
