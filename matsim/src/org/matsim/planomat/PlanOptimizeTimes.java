/* *********************************************************************** *
 * project: org.matsim.*
 * PlanOptimizeTimes.java
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

package org.matsim.planomat;

import java.util.List;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.DefaultFitnessEvaluator;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.EventManager;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.ChromosomePool;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.scoring.ScoringFunction;
import org.matsim.utils.misc.Time;
import org.matsim.world.Location;

/**
 * The "heart" of the planomat external strategy module:
 * Optimize a the departure times and activity durations
 * of a given <code>Plan</code>
 * <ul>
 * <li> according to a <code>ScoringFunction</code>
 * <li> with respect to time-of-day dependent travel costs as perceived
 *   by a <code>LegtravelTimeEstimator</code>.
 * </ul>
 * @author meisterk
 *
 */
public class PlanOptimizeTimes implements PlanAlgorithmI {

	private LegTravelTimeEstimator legTravelTimeEstimator = null;

	public PlanOptimizeTimes(final LegTravelTimeEstimator legTravelTimeEstimator) {

		super();
		this.legTravelTimeEstimator = legTravelTimeEstimator;

	}

	public void run(final Plan plan) {

		// distinguish for optimization tools
		String optiToolboxName = Gbl.getConfig().planomat().getOptimizationToolbox();
		if (optiToolboxName.equals("jgap")) {

			Genotype initialGAPopulation = PlanOptimizeTimes.initJGAP(plan, this.legTravelTimeEstimator);
			IChromosome fittest = PlanOptimizeTimes.evolveAndReturnFittest(initialGAPopulation);
			PlanOptimizeTimes.writeChromosome2Plan(fittest, plan, this.legTravelTimeEstimator);

		}

	}

	/**
	 * Configure JGAP for our purposes and generate the initial GA population.
	 *
	 * @param plan The encoding depends on the number of activities, this is why we pass the plan to the configuration
	 * @param estimator
	 * @return the initial population of GA individuals ready for evolution
	 */
	private static Genotype initJGAP(final Plan plan, final LegTravelTimeEstimator estimator) {

		Genotype jgapGAPopulation = null;

		int popSize = Gbl.getConfig().planomat().getPopSize();
		// put all the parameters in the config file later
		double planLength = 24.0 * 3600;

		Configuration jgapConfiguration = new Configuration();

		// analyze / modify plan for our purposes:
		// 1, how many activities do we have?
		// 2, clean all time information
		int numActs = 0;
		for (Object o : plan.getActsLegs()) {

			if (o.getClass().equals(Act.class)) {
				((Act) o).setDur(Time.UNDEFINED_TIME);
				((Act) o).setEndTime(Time.UNDEFINED_TIME);
				numActs++;
			} else if (o.getClass().equals(Leg.class)) {
				((Leg) o).setTravTime(Time.UNDEFINED_TIME);
			}

		}
		// first and last activity are assumed to be the same
		numActs -= 1;

		// set up JGAP
		try {
			// the following settings are copied from org.jgap.DefaultCOnfiguration
			// TODO configuration shouldnt be inited for every plan, but once
			// but currently do not know how to deal with threads because cloning doesn't work because jgap.impl.configuration writes System.Properties
			Configuration.reset();
			jgapConfiguration.setRandomGenerator(new StockRandomGenerator());
			jgapConfiguration.setEventManager(new EventManager());
			BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(jgapConfiguration, 0.95d);
//			BestChromosomesSelector bestChromsSelector = new BestChromosomesSelector(jgapConfiguration, 1 - (1 / popSize));
			bestChromsSelector.setDoubletteChromosomesAllowed(false);
			jgapConfiguration.addNaturalSelector(bestChromsSelector, true);
			//jgapConfiguration.setMinimumPopSizePercent(0);
			jgapConfiguration.setKeepPopulationSizeConstant(true);
			jgapConfiguration.setFitnessEvaluator(new DefaultFitnessEvaluator());
			jgapConfiguration.setChromosomePool(new ChromosomePool());
			jgapConfiguration.addGeneticOperator(new CrossoverOperator(jgapConfiguration));
			// Different than org.jgap.DefaultConfiguration: use mutation rate which adapts to chromosome size
			jgapConfiguration.addGeneticOperator(new MutationOperator(jgapConfiguration));

			//   use ordinary DoubleGene encoding with simple crossover and random mutation, later maybe use a mutator dependent on var mut prob
			//   genes: 1 for start time, one for time allocation of each activity except the last one, makes nAct double genes in the range between 0 and 24
			Gene[] sampleGenes = new Gene[numActs];
			sampleGenes[0] = new DoubleGene(jgapConfiguration, 0.0, planLength);
			for (int ii=1; ii < sampleGenes.length; ii++) {
				sampleGenes[ii] = new DoubleGene(jgapConfiguration, 0.0, planLength);
			}

			IChromosome sampleChromosome = new Chromosome(jgapConfiguration, sampleGenes);
			jgapConfiguration.setSampleChromosome( sampleChromosome );

			// initialize scoring function
			ScoringFunction sf = Gbl.getConfig().planomat().getScoringFunctionFactory().getNewScoringFunction(plan);
			
			PlanomatFitnessFunctionWrapper fitnessFunction = new PlanomatFitnessFunctionWrapper( sf, plan, estimator );
			jgapConfiguration.setFitnessFunction( fitnessFunction );

			// elitist selection (DeJong, 1975)
			jgapConfiguration.setPreservFittestIndividual(true);
			jgapConfiguration.setPopulationSize( popSize );
			jgapGAPopulation = Genotype.randomInitialGenotype( jgapConfiguration );

		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return jgapGAPopulation;

	}

	/**
	 * @param population the initial GA population that serves as breed for evolution
	 * @return the fittest individual after evolution
	 */
	private static IChromosome evolveAndReturnFittest(final Genotype population) {

		double travelPenalty = Math.abs(Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "traveling"))) / 3600;
		double minDiff = travelPenalty * Gbl.getConfig().planomat().getIndifference();
//		System.out.println(minDiff);

		IChromosome fittest = null;
		double avg = 0, max = 0, oldmax = 0;
		boolean cancelEvolution = false;
		int generation = 0;

		int maxNumGenerations = Gbl.getConfig().planomat().getJgapMaxGenerations();
		int percentEvolution = maxNumGenerations / 10;

		while (cancelEvolution == false) {

			population.evolve();

			fittest = population.getFittestChromosome();

			if (generation == 0) {
				oldmax = fittest.getFitnessValue();
			} else if ((generation > 0) && (generation % percentEvolution == 0)) {
				max = fittest.getFitnessValue();
				if (Gbl.getConfig().planomat().isBeVerbose()) {
					avg = PlanOptimizeTimes.getAverageFitness(population);
					System.out.println(" [Planomat] Generation " + generation + ":\t" + avg + "\t" + max);
				}
				if ((max - oldmax) < minDiff) {
					cancelEvolution = true;
				}
				oldmax = max;
			}

			generation++;
			if (generation == maxNumGenerations) {
				cancelEvolution = true;
			}

		}

		fittest = population.getFittestChromosome();
		if (Gbl.getConfig().planomat().isBeVerbose()) {
			double fitness = fittest.getFitnessValue();
			System.out.println("Currently fittest Chromosome has fitness " + fitness);
		}
		return fittest;
	}

	private static double getAverageFitness(final Genotype population) {

		double averageFitness = 0;

		List<Chromosome> chromosomes = population.getPopulation().getChromosomes();

		for (Chromosome c : chromosomes) {
			averageFitness += c.getFitnessValue();
		}

		averageFitness = averageFitness / chromosomes.size();

		return averageFitness;

	}

	/**
	 * Writes a JGAP chromosome back to matsim plan object.
	 *
	 * @param individual the GA individual (usually the fittest after evolution) whose values will be written back to a plan object
	 * @param plan the plan that will be altered
	 */
	private static void writeChromosome2Plan(final IChromosome individual, final Plan plan, final LegTravelTimeEstimator estimator) {

		Act activity = null;
		Leg leg = null;

		Gene[] fittestGenes = individual.getGenes();

		int max = plan.getActsLegs().size();
		double now = 0;

		for (int ii = 0; ii < max; ii++) {

			Object o = plan.getActsLegs().get(ii);

			if (o.getClass().equals(Act.class)) {

				activity = ((Act) o);

				// handle first activity
				if (ii == 0) {
					// set start to midnight
					activity.setStartTime(now);
					// set end time of first activity
					activity.setEndTime(((DoubleGene) fittestGenes[ii / 2]).doubleValue());
					// calculate resulting duration
					activity.setDur(activity.getEndTime() - activity.getStartTime());
					// move now pointer to activity end time
					now += activity.getEndTime();

					// handle middle activities
				} else if ((ii > 0) && (ii < (max - 1))) {

					// assume that there will be no delay between arrival time and activity start time
					activity.setStartTime(now);
					// set duration middle activity
					activity.setDur(((DoubleGene) fittestGenes[ii / 2]).doubleValue());
					// move now pointer by activity duration
					now += activity.getDur();
					// set end time accordingly
					activity.setEndTime(now);

					// handle last activity
				} else if (ii == (max - 1)) {

					// assume that there will be no delay between arrival time and activity start time
					activity.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					activity.setDur(Time.UNDEFINED_TIME);
					activity.setEndTime(Time.UNDEFINED_TIME);

				}

			} else if (o.getClass().equals(Leg.class)) {

				leg = ((Leg) o);

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepTime(now);
				// set arrival time to estimation
				Location origin = ((Act) plan.getActsLegs().get(ii - 1)).getLink();
				Location destination = ((Act) plan.getActsLegs().get(ii + 1)).getLink();

				double travelTimeEstimation = estimator.getLegTravelTimeEstimation(
						plan.getPerson().getId(),
						0.0,
						origin,
						destination,
						leg.getRoute(),
				"car");
				leg.setTravTime(travelTimeEstimation);
				now += leg.getTravTime();
				// set planned arrival time accordingly
				leg.setArrTime(now);

			}
		}

		// invalidate score information
		plan.setScore(Double.NaN);

	}

}
