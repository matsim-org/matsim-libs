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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scoring.ScoringFunction;

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

	private PopulationImpl population = null;
	private playground.jhackney.scoring.EventSocScoringFactory sfFactory = null;
	private final TreeMap<String, playground.jhackney.scoring.EventSocScoringFunction> agentScorers = new TreeMap<String, playground.jhackney.scoring.EventSocScoringFunction>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private final double learningRate;
	boolean report=true;

	public EventsToScoreAndReport(final PopulationImpl population, final playground.jhackney.scoring.EventSocScoringFactory factory) {
		this(population, factory, Gbl.getConfig().charyparNagelScoring().getLearningRate());
	}

	public EventsToScoreAndReport(final PopulationImpl population, final playground.jhackney.scoring.EventSocScoringFactory factory, final double learningRate) {
		super();
		this.population = population;
		this.sfFactory = factory;
		this.learningRate = learningRate;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.getPersonId().toString());
		sf.endActivity(event.getTime());
		sf.startLeg(event.getTime(), null);//event.getLeg();
	}

	public void handleEvent(final AgentArrivalEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.getPersonId().toString());
		sf.endLeg(event.getTime());
		sf.startActivity(event.getTime(), null);
	}

	public void handleEvent(final AgentStuckEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.getPersonId().toString());
		sf.agentStuck(event.getTime());
	}

	public void handleEvent(final AgentMoneyEvent event) {
		playground.jhackney.scoring.EventSocScoringFunction sf = getScoringFunctionForAgent(event.getPersonId().toString());
		sf.addMoney(event.getAmount());
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
			Plan plan = this.population.getPersons().get(new IdImpl(agentId)).getSelectedPlan();
			Double oldScore = plan.getScore();
			if (oldScore == null) {
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
				uout.write("agentId\tUlegt\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc\r\n");
				muout.write("agentId\tUlegt\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc\r\n");
//				uout.write("agentId\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc\tUlegt\r\n");
//				muout.write("agentId\tactNumber\tactType\tUdur\tUed\tUla\tUld\tUs\tUw\tUsoc\tUlegt\r\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (Map.Entry<String, playground.jhackney.scoring.EventSocScoringFunction> entry : this.agentScorers.entrySet()) {
				String agentId = entry.getKey();
				playground.jhackney.scoring.EventSocScoringFunction sf = entry.getValue();
				Plan plan = this.population.getPersons().get(new IdImpl(agentId)).getSelectedPlan();
				Iterator actLegIter = plan.getPlanElements().iterator();
				ActivityImpl act = (ActivityImpl) actLegIter.next(); // assume first plan element is always an Activity

				int actNumber=0;
				int legNumber=-1;

//				try {
////					muout.write("\t"+actNumber+"\t"+act.getType()+"\t"+sf.getDudur(act)+"\t"+sf.getDued(act)+"\t"+sf.getDula(act)+"\t"+sf.getDuld(act)+"\t"+sf.getDus(act)+"\t"+sf.getDuw(act)+"\t"+sf.getDusoc(act));
////					uout.write("\t"+actNumber+"\t"+act.getType()+"\t"+sf.getUdur(act)+"\t"+sf.getUed(act)+"\t"+sf.getUla(act)+"\t"+sf.getUld(act)+"\t"+sf.getUs(act)+"\t"+sf.getUw(act)+"\t"+sf.getUsoc(act));
//					muout.write(agentId+"\t"+actNumber+"\t"+act.getType()+"\t"+sf.getDudur(act)+"\t"+sf.getDued(act)+"\t"+sf.getDula(act)+"\t"+sf.getDuld(act)+"\t"+sf.getDus(act)+"\t"+sf.getDuw(act)+"\t"+sf.getDusoc(act));
//					uout.write(agentId+"\t"+actNumber+"\t"+act.getType()+"\t"+sf.getUdur(act)+"\t"+sf.getUed(act)+"\t"+sf.getUla(act)+"\t"+sf.getUld(act)+"\t"+sf.getUs(act)+"\t"+sf.getUw(act)+"\t"+sf.getUsoc(act));
//
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				while(actLegIter.hasNext()){//alternates Act-Leg-Act-Leg and ends with Act
					Object o = actLegIter.next();
					if (o instanceof LegImpl ) {
						LegImpl leg = (LegImpl) o;
//						if(act.equals(plan.getFirstActivity())){
//							try {
//								muout.write("\t"+0.0);
//								uout.write("\t"+0.0);
//								muout.newLine();
//								uout.newLine();
//							} catch (IOException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//
//						} else {
							try {
								muout.write(agentId+"\t"+sf.getDulegt(leg));
								uout.write(agentId+"\t"+sf.getUlegt(leg));
//								uout.write("\t"+sf.getUlegt(leg));
//								uout.newLine();
//								muout.write("\t"+sf.getDulegt(leg));
//								muout.newLine();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							legNumber++;
					}else if (o instanceof ActivityImpl && !(act.equals(((PlanImpl) plan).getLastActivity()))) {
						act = (ActivityImpl) o;
						try {
							muout.write("\t"+actNumber+"\t"+act.getType()+"\t"+sf.getDudur(act)+"\t"+sf.getDued(act)+"\t"+sf.getDula(act)+"\t"+sf.getDuld(act)+"\t"+sf.getDus(act)+"\t"+sf.getDuw(act)+"\t"+sf.getDusoc(act));
							muout.newLine();
							uout.write("\t"+actNumber+"\t"+act.getType()+"\t"+sf.getUdur(act)+"\t"+sf.getUed(act)+"\t"+sf.getUla(act)+"\t"+sf.getUld(act)+"\t"+sf.getUs(act)+"\t"+sf.getUw(act)+"\t"+sf.getUsoc(act));
							uout.newLine();
//							muout.write(agentId+"\t"+actNumber+"\t"+act.getType()+"\t"+sf.getDudur(act)+"\t"+sf.getDued(act)+"\t"+sf.getDula(act)+"\t"+sf.getDuld(act)+"\t"+sf.getDus(act)+"\t"+sf.getDuw(act)+"\t"+sf.getDusoc(act));
//							uout.write(agentId+"\t"+actNumber+"\t"+act.getType()+"\t"+sf.getUdur(act)+"\t"+sf.getUed(act)+"\t"+sf.getUla(act)+"\t"+sf.getUld(act)+"\t"+sf.getUs(act)+"\t"+sf.getUw(act)+"\t"+sf.getUsoc(act));

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						actNumber++;

					}
				}
//				try{
//				muout.write("\r\n");
//				uout.write("\r\n");
//				} catch (IOException e){
//				e.printStackTrace();
//				}
			}
			try {
				muout.close();
				uout.close();
			} catch (IOException e) {
				e.printStackTrace();
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
		if (this.scoreSum == 0) return Double.NaN;
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
		if (sf == null) return Double.NaN;
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
			sf = this.sfFactory.getNewScoringFunction(this.population.getPersons().get(new IdImpl(agentId)).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}

}
