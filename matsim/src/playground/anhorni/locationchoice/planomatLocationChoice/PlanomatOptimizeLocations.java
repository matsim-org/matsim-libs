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

package playground.anhorni.locationchoice.planomatLocationChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;

import playground.anhorni.locationchoice.planomatLocationChoice.costestimators.CharyparNagelFitnessFunction;

/**
 *
 */
public class PlanomatOptimizeLocations implements PlanAlgorithmI {

	private final NetworkLayer network;
	private TravelTimeI travelTimeCalculator = null;
	private TravelCostI travelCostCalculator = null;

	private final TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	private final TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();

	public PlanomatOptimizeLocations(
			final NetworkLayer network,
			final TravelCostI travelCostCalculator,
			final TravelTimeI travelTimeCalculator) {
		super();
		this.network=network;
		this.travelCostCalculator=travelCostCalculator;
		this.travelTimeCalculator=travelTimeCalculator;

		final Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);

		this.shop_facilities.putAll(facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(facilities.getFacilities("leisure_sports"));
	}

	public void run(final Plan plan) {

		// distinguish for optimization tools
		/* TODO:
		final String optiToolboxName = PlanomatConfig.getOptimizationToolboxName();
		if (optiToolboxName.equals("jgap")) {

		*/
			final Genotype initialGAPopulation =
				PlanomatOptimizeLocations.initJGAP(
						plan,
						this.network,
						this.travelCostCalculator,
						this.travelTimeCalculator,
						this.shop_facilities);

			final IChromosome fittest = PlanomatOptimizeLocations
				.evolveAndReturnFittest(initialGAPopulation);
			PlanomatOptimizeLocations.writeChromosome2Plan(
					fittest,
					plan,
					this.shop_facilities,
					this.network,
					this.travelCostCalculator,
					this.travelTimeCalculator);


		// TODO: }
	}

	/**
	 * Configure JGAP for our purposes and generate the initial GA population.
	 *
	 * @param plan The encoding depends on the number of activities,
	 *  this is why we pass the plan to the configuration
	 * @param estimator
	 * @return the initial population of GA individuals ready for evolution
	 */
	private static Genotype initJGAP(final Plan plan,
			final NetworkLayer network,
			final TravelCostI travelCostCalculator,
			final TravelTimeI travelTimeCalculator,
			final TreeMap<Id,Facility> shop_facilities) {

		Genotype jgapGAPopulation = null;

		// TODO: final int popSize = PlanomatConfig.getPopSize();
		final int popSize = 1000;

		// put all the parameters in the config file later
	    final Id [] shop_array = shop_facilities.keySet().toArray(
	    		new Id[shop_facilities.keySet().size()]);
	    final int shop_array_size=shop_array.length;

		final Configuration jgapConfiguration = new Configuration();

		// first and last activity are assumed to be the same
		final int numActs=plan.getActsLegs().size()-1;

		// set up JGAP
		try {
			// the following settings are copied from org.jgap.DefaultCOnfiguration
			// TODO configuration shouldnt be inited for every plan, but once
			// but currently do not know how to deal with threads because cloning doesn't work because jgap.impl.configuration writes System.Properties
			Configuration.reset();
			jgapConfiguration.setRandomGenerator(new StockRandomGenerator());
			jgapConfiguration.setEventManager(new EventManager());
			final BestChromosomesSelector bestChromsSelector =
					new BestChromosomesSelector(jgapConfiguration, 0.95d);
			bestChromsSelector.setDoubletteChromosomesAllowed(false);
			jgapConfiguration.addNaturalSelector(bestChromsSelector, true);
			jgapConfiguration.setKeepPopulationSizeConstant(true);
			jgapConfiguration.setFitnessEvaluator(new DefaultFitnessEvaluator());
			jgapConfiguration.setChromosomePool(new ChromosomePool());
			jgapConfiguration.addGeneticOperator(new CrossoverOperator(jgapConfiguration));
			// Different than org.jgap.DefaultConfiguration: use mutation rate which adapts to chromosome size
			jgapConfiguration.addGeneticOperator(new MutationOperator(jgapConfiguration));

			// use ordinary DoubleGene encoding with simple crossover and
			// random mutation, later maybe use a mutator dependent on var mut prob
			final Gene[] sampleGenes = new Gene[numActs-1];

			//use IntegerGene
			// primary act have a fixed location
			// check that in scoring function
			for (int ii=0; ii < sampleGenes.length; ii++) {
				sampleGenes[ii] = new IntegerGene(jgapConfiguration, 0, shop_array_size-1);
			}

			final IChromosome sampleChromosome = new Chromosome(jgapConfiguration, sampleGenes);
			jgapConfiguration.setSampleChromosome( sampleChromosome );

			// initialize scoring function
			final CharyparNagelScoringFunctionFactory sfFactory = new CharyparNagelScoringFunctionFactory();
			final CharyparNagelScoringFunction sf = (CharyparNagelScoringFunction) sfFactory.getNewScoringFunction(plan);

			final CharyparNagelFitnessFunction fitnessFunction =
				new CharyparNagelFitnessFunction(sf, plan, network,	travelCostCalculator,
						travelTimeCalculator);

			jgapConfiguration.setFitnessFunction( fitnessFunction );

			// elitist selection (DeJong, 1975)
			jgapConfiguration.setPreservFittestIndividual(true);
			jgapConfiguration.setPopulationSize( popSize );
			jgapGAPopulation = Genotype.randomInitialGenotype( jgapConfiguration );

		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return jgapGAPopulation;

	}

	/**
	 * @param population the initial GA population that serves as breed for evolution
	 * @return the fittest individual after evolution
	 */
	private static IChromosome evolveAndReturnFittest(final Genotype population) {

		final double travelPenalty = Math.abs(Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "traveling"))) / 3600;
		// TODO: final double minDiff = travelPenalty * PlanomatConfig.getIndifference();
		final double minDiff = travelPenalty * 0.001;

		IChromosome fittest = null;
		final double avg = 0;
		double max = 0, oldmax = 0;
		boolean cancelEvolution = false;
		int generation = 0;

		// TODO: final int maxNumGenerations = PlanomatConfig.getJgapMaxGenerations();
		final int maxNumGenerations = 10;
		final int percentEvolution = maxNumGenerations / 10;

		while (cancelEvolution == false) {

			population.evolve();

			fittest = population.getFittestChromosome();

			if (generation == 0) {
				oldmax = fittest.getFitnessValue();
			} else if ((generation > 0) && (generation % percentEvolution == 0)) {
				max = fittest.getFitnessValue();
				/* TODO:
				if (PlanomatConfig.isBeVerbose()) {
					avg = PlanomatOptimizeLocations.getAverageFitness(population);
					System.out.println(" [Planomat] Generation " + generation + ":\t" + avg + "\t" + max);
				}
				*/
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
		/* TODO:
		if (PlanomatConfig.isBeVerbose()) {
			final double fitness = fittest.getFitnessValue();
			System.out.println("Currently fittest Chromosome has fitness " + fitness);
		}
		*/
		return fittest;
	}

	private static double getAverageFitness(final Genotype population) {

		double averageFitness = 0;

		final List<Chromosome> chromosomes = population.getPopulation().getChromosomes();

		for (final Chromosome c : chromosomes) {
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
	private static void writeChromosome2Plan(
			final IChromosome a_subject,
			final Plan plan,
			final TreeMap<Id,Facility> shop_facilities,
			final NetworkLayer network,
			final TravelCostI travelCostCalculator,
			final TravelTimeI travelTimeCalculator) {

		// get now routes and times ----------------------------------------------
		// change locations:
		 final Id [] shop_array = shop_facilities.keySet().toArray(
		    		new Id[shop_facilities.keySet().size()]);

		int gene_nr=0;
		final BasicPlanImpl.ActIterator iter_act = plan.getIteratorAct();
		while(iter_act.hasNext()) {
			final BasicAct act = iter_act.next();
			if (act.getType().startsWith("s")) {

				final int shop_index=((IntegerGene)a_subject.getGene(gene_nr)).intValue();
				final Facility facility=shop_facilities.get(
						shop_array[shop_index]);

				exchangeFacility("s",facility, plan);
			}
			gene_nr++;
		}

		// calculate new route --------------
		final PlansCalcRouteLandmarks router=new PlansCalcRouteLandmarks(
				network, null , travelCostCalculator, travelTimeCalculator);
		router.run(plan);
		// invalidate score information
		plan.setScore(Double.NaN);
	}

	private static void exchangeFacility(final String type, final Facility facility, final Plan plan) {
		// modify plan by randomly exchanging a link (facility) in the plan
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				// plans: link, coords
				// facilities: coords
				// => use coords
				act.setCoord(facility.getCenter());
			}
		}

		// loop over all <leg>s, remove route-information
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
	}

}
