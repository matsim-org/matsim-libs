/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.jhackney.scoring;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlanImpl.ActLegIterator;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentMoneyEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentMoneyEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.Population;
import org.matsim.scoring.ScoringFunction;

/**
 * Continuously calculates the score of the selected plans of a given population
 * based on events, tracks the score components, and if "report" is set,
 * writes two tab-delimited output files to the output directory containing (1)
 * the marginal utility components and (2) the utility components of each activity
 * and leg. It is recommended to analyze only the final iteration this way,
 * due to the large output file.<br>
 * Departure- and Arrival-Events *must* be provided to calculate the score,
 * AgentStuck-Events are used if available to add a penalty to the score.
 * The final score are written to the selected plans of each person in the
 * population.
 *
 * @author mrieser
 * @author jhackney
 */
public class EventsToScoreAndReport implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler {

	private Population population = null;
	private playground.jhackney.scoring.EventSocScoringFactory sfFactory = null;
	private final TreeMap<String, playground.jhackney.scoring.EventSocScoringFunction> agentScorers = new TreeMap<String, playground.jhackney.scoring.EventSocScoringFunction>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private final double learningRate;
	boolean report=true;

	public EventsToScoreAndReport(final Population population, final playground.jhackney.scoring.EventSocScoringFactory factory) {
		this(population, factory, Gbl.getConfig().charyparNagelScoring().getLearningRate());
	}

	public EventsToScoreAndReport(final Population population, final playground.jhackney.scoring.EventSocScoringFactory factory, final double learningRate) {
		super();
		this.population = population;
		this.sfFactory = factory;
		this.learningRate = learningRate;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.startLeg(event.time, event.leg);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.endLeg(event.time);
	}

	public void handleEvent(final AgentStuckEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.agentStuck(event.time);
	}

	public void handleEvent(final AgentMoneyEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.addMoney(event.amount);
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {

		for (Map.Entry<String, playground.jhackney.scoring.EventSocScoringFunction> entry : this.agentScorers.entrySet()) {
			String agentId = entry.getKey();
			playground.jhackney.scoring.EventSocScoringFunction sf = entry.getValue();
			sf.finish();

			double score = sf.getScore();
			Plan plan = this.population.getPerson(agentId).getSelectedPlan();
			double oldScore = plan.getScore();
			if (Double.isNaN(oldScore)) {
				plan.setScore(score);
			} else {
				plan.setScore(this.learningRate * score + (1-this.learningRate) * oldScore);
			}

			this.scoreSum += score;
			this.scoreCount++;
		}

		if(report){
			//open marginal utility outfile
			//open utility outfile
			String ufname=Gbl.getConfig().socnetmodule().getInDirName()+"_UtilityComponents.txt";
			BufferedWriter uout = null;

			String mufname=Gbl.getConfig().socnetmodule().getInDirName()+"_MarginalUtilityComponents.txt";
			BufferedWriter muout = null;
			try {
				uout = new BufferedWriter(new FileWriter(ufname));
				muout = new BufferedWriter(new FileWriter(mufname));
				uout.write("agentId\tUlegt\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc");
				muout.write("agentId\tUlegt\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (Map.Entry<String, playground.jhackney.scoring.EventSocScoringFunction> entry : this.agentScorers.entrySet()) {
				String agentId = entry.getKey();
				playground.jhackney.scoring.EventSocScoringFunction sf = entry.getValue();
				Plan plan = this.population.getPerson(agentId).getSelectedPlan();
				ActLegIterator actLegIter = plan.getIterator();
				Act act = (Act) actLegIter.nextAct();


				int actNumber=0;
				int legNumber=-1;

				while(actLegIter.hasNextLeg()){//alternates Act-Leg-Act-Leg and ends with Act

					Leg leg = (Leg) actLegIter.nextLeg();
					legNumber++;

					try {
						muout.write(agentId+"\t"+sf.getDulegt(leg));
						uout.write(agentId+"\t"+sf.getDulegt(leg));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					act = (Act) actLegIter.nextAct();
					actNumber++;

					try {
						muout.write("\t"+actNumber+"\t\""+act.getType()+"\"\t"+sf.getDudur(act)+"\t"+sf.getDued(act)+"\t"+sf.getDula(act)+"\t"+sf.getDuld(act)+"\t"+sf.getDus(act)+"\t"+sf.getDuw(act)+"\t"+sf.getDusoc(act));
						uout.write("\t"+actNumber+"\t\""+act.getType()+"\"\t"+sf.getUdur(act)+"\t"+sf.getUed(act)+"\t"+sf.getUla(act)+"\t"+sf.getUld(act)+"\t"+sf.getUs(act)+"\t"+sf.getUw(act)+"\t"+sf.getUsoc(act));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Returns the actual average plans' score before it was assigned to the plan
	 * and possibility mixed with old scores (learningrate).
	 *
	 * @return the average score of the plans before mixing with the old scores (learningrate)
	 */
	public double getAveragePlanPerformance() {
		if (this.scoreSum == 0) return BasicPlan.UNDEF_SCORE;
		return (this.scoreSum / this.scoreCount);
	}

	/**
	 * Returns the score of a single agent. This method only returns useful values
	 * if the method {@link #finish() } was called before.
	 * description
	 *
	 * @param agentId The id of the agent the score is requested for.
	 * @return The score of the specified agent.
	 */
	public double getAgentScore(final Id agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId.toString());
		if (sf == null) return BasicPlan.UNDEF_SCORE;
		return sf.getScore();
	}

	public void reset(final int iteration) {
		this.agentScorers.clear();
		this.scoreCount = 0;
		this.scoreSum = 0.0;
	}

	/**
	 * Returns the scoring function for the specified agent. If the agent already
	 * has a scoring function, that one is returned. If the agent does not yet
	 * have a scoring function, a new one is created and assigned to the agent
	 * and returned.
	 *
	 * @param agentId The id of the agent the scoring function is requested for.
	 * @return The scoring function for the specified agent.
	 */
	private playground.jhackney.scoring.EventSocScoringFunction getScoringFunctionForAgent(final String agentId) {
		playground.jhackney.scoring.EventSocScoringFunction sf = this.agentScorers.get(agentId);
		if (sf == null) {
			sf = this.sfFactory.getNewScoringFunction(this.population.getPerson(agentId).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}

}
