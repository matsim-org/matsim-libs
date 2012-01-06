/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatFitnessFunctionImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.planomat.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.jgap.InvalidConfigurationException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.PlanomatJGAPConfiguration;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatChromosome;
import playground.thibautd.planomat.api.PlanomatFitnessFunction;

/**
 * The default implementation of a {@link PlanomatFitnessFunction}.
 *
 * It optimises activity durations and subtour-level mode, roughly in the same way
 * that the v1 (with modifications to make compatibility with experimental/
 * non-standard modules easier to achieve).
 *
 * <br>
 * When given a chromosome, it scores it in the following way:
 *
 * <ul>
 *   <li> it executes an arbitrary number of {@link PlanAlgorithm}s
 *   on the plan with wich it was initialised. This may be used to clean
 *   the plan (for example, removing transit activities).
 *
 *   <li> it modifies back the resulting plan so that <u>activity end times</u> and
 *   <u>leg</u> modes reflect the genotype. All other attributes are leaved unchanged
 *   (in particular, <b>activity start times are left untouched</b>).
 *
 *   <li> it executes an arbitrary number of {@link PlanAlgorithm}s
 *   on the resulting plan. <b>At least one of those algorithms must
 *   be an algorithm which sets expected travel time and routes, as well
 *   as activity start time as expected from arrival times</b>.
 *   This is not enforced nor checked, and the procedure will not mean anything if it is
 *   not the case. The algorithms are executed in the order they were added.
 *   This procedure allows to handle appropriately "non-trivial" modes,
 *   such as public transport.
 *
 *   <li> it parses the resulting plan, passing its elements to the matsim
 *   scoring function, and returns the score.
 * </ul>
 *
 * Please note that at initialisation, <b>the algorithm lists are empty</b>.
 * It is expected that {@link playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory}s
 * returning instances of this class take care of setting algorithms appropriately.
 * <br>
 * Please also be aware that this function is unsuitable to score several chromosomes
 * encoding for the same plan from different parallel threads, as it modifies
 * the plan instance it was passed at initialisation for scoring. It is OK to
 * run several instances of planomat in parallel.
 * <br>
 * Note that the algorithms beeing executed at each fitness evaluation, efficiency
 * is a key issue.
 *
 * @author thibautd
 */
public class PlanomatFitnessFunctionImpl extends PlanomatFitnessFunction {
	private static final long serialVersionUID = 1L;

	private final PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourLevel;
	private final Plan plan;
	private final String[] possibleModes;
	private final ScoringFunction scoringFunction;
	private final Configuration jgapConfig;
	private final ActivityWhiteList whiteList;
	private final List<PlanAlgorithm> preAlgos = new ArrayList<PlanAlgorithm>();
	private final List<PlanAlgorithm> postAlgos = new ArrayList<PlanAlgorithm>();
	private final double scenarioDuration;

	// initialised at sample chromosome construction
	private final Map<Activity, Double> enforcedDurations = new HashMap<Activity, Double>();
	private double timeToFill;
	private final List<List<Leg>> subtours = new ArrayList<List<Leg>>();

	/**
	 * Initialises a fitness function.
	 *
	 * @param scoringFunctionFactory a {@link ScoringFunctionFactory} for creating
	 * the matsim scoring function to use.
	 * @param configuration the jgap configuration object. This should be an instance
	 * of {@link PlanomatJGAPConfiguration}
	 * @param scenarioDuration the total duration of the plan, in seconds
	 * @param whiteList an {@link ActivityWhiteList} giving information about
	 * the modifiable activities. Activities of unmodifiable types will not be touched;
	 * that is, the time between the end of the previous activity and the end of the 
	 * unmodifiable activity will be kept constant.
	 * @param allowedModes a set containing all modes planomat is allowed to affect.
	 * Car will be removed if the agent have no car.
	 * @param subtourLevel the level to use to analyse subtours
	 * @param plan the plan to optimise
	 */
	public PlanomatFitnessFunctionImpl(
			final ScoringFunctionFactory scoringFunctionFactory,
			final Configuration configuration,
			final double scenarioDuration,
			final ActivityWhiteList whiteList,
			final Set<String> allowedModes,
			final PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourLevel,
			final Plan plan) {
		this.plan = plan;
		this.whiteList = whiteList;
		this.jgapConfig = configuration;
		this.subtourLevel = subtourLevel;
		this.scenarioDuration = scenarioDuration;

		scoringFunction = scoringFunctionFactory.createNewScoringFunction( plan );
		this.possibleModes = getPossibleModes( plan , allowedModes );
	}

	private static String[] getPossibleModes(
			final Plan plan,
			final Set<String> allowedModes) {
		// remove car option for agents that have no car available
		List<String> possibleModes = new ArrayList<String>( allowedModes );

		String carAvail = ((PersonImpl) plan.getPerson()).getCarAvail();
		if ("never".equals(carAvail)) {
			possibleModes.remove(TransportMode.car);
		}

		return possibleModes.toArray( new String[0] );
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public PlanomatChromosome getSampleChomosome() throws InvalidConfigurationException {
		PlanAnalyzeSubtours subtourAnalyser = new PlanAnalyzeSubtours();
		subtourAnalyser.setTripStructureAnalysisLayer( subtourLevel );
		subtourAnalyser.run( plan );
		
		subtours.clear();
		for (List<PlanElement> subtour : subtourAnalyser.getSubtours()) {
			List<Leg> list = new ArrayList<Leg>();

			for (PlanElement pe : subtour) {
				if (pe instanceof Leg) {
					list.add( (Leg) pe );
				}
			}

			subtours.add( list );
		}

		List<Gene> genes = new ArrayList<Gene>();
		// count genes and add them, and store a map gene->planElement
		// start with mode genes
		int maxIntGene = possibleModes.length - 1;
		for (int i=0; i < subtourAnalyser.getNumSubtours(); i++) {
			genes.add( new IntegerGene( jgapConfig, 0, maxIntGene ) );
		}

		// then, durations
		double untouchableTime = 0;
		double now = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				if (whiteList.isModifiableType( act.getType() )) {
					genes.add( new DoubleGene( jgapConfig , 0 , 1 ) );
				}
				else {
					// duration includes leg(s)
					double dur =  act.getEndTime() - now;
					untouchableTime += dur;
					enforcedDurations.put( act , dur );
				}

				now = act.getEndTime();
			}
		}
		// TODO: check for validity
		timeToFill = scenarioDuration - untouchableTime;

		return new PlanomatChromosome( jgapConfig , genes );
	}

	@Override
	public void modifyBackPlan(final IChromosome chromosome) {
		Gene[] genes = chromosome.getGenes();

		// counter to step in genes array
		int count = 0;

		// mode
		for (List<Leg> subtour : subtours) {
			int modeIndex = ((IntegerGene) genes[ count ]).intValue();
			String mode = possibleModes[ modeIndex ];

			for (Leg leg : subtour) {
				leg.setMode( mode );
			}

			count++;
		}

		// duration
		double sumDoubleGenes = 0;
		double[] geneValues = new double[ genes.length - count ];

		for (int i=0; count < genes.length; count++ , i++) {
			double val = ((DoubleGene) genes[ count ]).doubleValue();
			sumDoubleGenes += val;
			geneValues[ i ] = val;
		}

		for (int i=0; i < geneValues.length; i++) {
			geneValues[ i ] = (geneValues[ i ] / sumDoubleGenes) * timeToFill;
		}

		double now = 0;
		int currentGene = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				Double enforcedDuration = enforcedDurations.get( act );
				double duration;

				if (enforcedDuration == null) {
					duration = geneValues[ currentGene ];
					currentGene++;
				}
				else {
					duration = enforcedDuration;
				}

				act.setEndTime( now + duration );
				now += duration;
			}
		}
	}

	@Override
	protected double evaluate(final IChromosome chromosome) {
		// decode
		// ---------------------------------------------------------------------
		for (PlanAlgorithm algo : preAlgos) {
			algo.run( plan );
		}

		modifyBackPlan( chromosome );

		for (PlanAlgorithm algo : postAlgos) {
			algo.run( plan );
		}

		// score
		// ---------------------------------------------------------------------
		scoringFunction.reset();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				scoringFunction.handleActivity( (Activity) pe );
			}
			else if (pe instanceof Leg) {
				scoringFunction.handleLeg( (Leg) pe );
			}
			else {
				throw new RuntimeException( "unhandled PlanElement type "+pe.getClass().getName() );
			}
		}
		scoringFunction.finish();

		return plan.getScore();
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Adds an algorithm to execute on the plan after activity durations and
	 * modes were set.
	 *
	 * @param algo the algorithm to add
	 */
	public void addPostDecodingPlanAlgorithm(final PlanAlgorithm algo) {
		postAlgos.add( algo );
	}

	/**
	 * Removes an algorithm from the list of algorithms to execute after activity
	 * durations and modes were set. More rigorously, this methods removes the
	 * first encountered algorithm <tt>A</tt> for which <tt>algo.equals( A )</tt>
	 * is true.
	 *
	 * @param algo the algorithm to remove
	 * @return true if a element was actually removed from the list.
	 */
	public boolean removePostDecodingPlanAlgo(final PlanAlgorithm algo) {
		return postAlgos.remove( algo );
	}

	/**
	 * Returns a view of the list of plan algorithms
	 * @return an immutable view of the algorithm list.
	 */
	public List<PlanAlgorithm> getPostDecodingPlanAlgorithms() {
		return Collections.unmodifiableList( postAlgos );
	}

	/**
	 * Adds an algorithm to execute on the plan before activity durations and
	 * modes are set.
	 *
	 * @param algo the algorithm to add
	 */
	public void addPreDecodingPlanAlgorithm(final PlanAlgorithm algo) {
		preAlgos.add( algo );
	}

	/**
	 * Removes an algorithm from the list of algorithms to execute before activity
	 * durations and modes are set. More rigorously, this methods removes the
	 * first encountered algorithm <tt>A</tt> for which <tt>algo.equals( A )</tt>
	 * is true.
	 *
	 * @param algo the algorithm to remove
	 * @return true if a element was actually removed from the list.
	 */
	public boolean removePreDecodingPlanAlgo(final PlanAlgorithm algo) {
		return preAlgos.remove( algo );
	}

	/**
	 * Returns a view of the list of plan algorithms
	 * @return an immutable view of the algorithm list.
	 */
	public List<PlanAlgorithm> getPreDecodingPlanAlgorithms() {
		return Collections.unmodifiableList( preAlgos );
	}
}
