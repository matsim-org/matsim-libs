/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.offlinecoalition;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.ExtraPlanRemover;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector;
import org.matsim.core.utils.misc.Counter;
import playground.thibautd.negotiation.framework.AlternativesGenerator;
import playground.thibautd.negotiation.framework.NegotiatingAgents;
import playground.thibautd.negotiation.framework.NegotiationAgent;
import playground.thibautd.negotiation.framework.Proposition;
import playground.thibautd.negotiation.framework.PropositionUtility;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author thibautd
 */
public class CoalitionChoiceIterator<P extends Proposition> {
	private static final Logger log = Logger.getLogger( CoalitionChoiceIterator.class );
	private final NegotiatingAgents<P> agents;
	private final Population population;
	private final CoalitionSelector selector;
	private final OfflineCoalitionConfigGroup configGroup;

	private final JointPlans jointPlans = new JointPlans();

	@Inject
	public CoalitionChoiceIterator(
			final AlternativesGenerator<P> alternativesGenerator,
			final NegotiatingAgents<P> agents,
			final JointPlanCreator<P> jointPlanCreator,
			final PropositionUtility<P> utility,
			final Population population,
			final CoalitionSelector selector,
			final ExtraPlanRemover extraPlanRemover,
			final OfflineCoalitionConfigGroup configGroup ) {
		this.agents = agents;
		this.population = population;
		this.selector = selector;
		this.configGroup = configGroup;

		log.info( "Initialize joint plans" );
		final Counter counter = new Counter( "Initialize joint plan # " );
		final Counter agentCounter = new Counter( "Look at agent # " , " / "+agents.getAllAgents().size() );
		final ReplanningGroup group = new ReplanningGroup();
		agents.getAllAgents().stream()
				.map( NegotiationAgent::getId )
				.map( population.getPersons()::get )
				.forEach( group::addPerson );

		int i = 0;
		for ( NegotiationAgent<P> agent : agents.getAllAgents() ) {
			agentCounter.incCounter();
			for ( P proposition : alternativesGenerator.generateAlternatives( agent ) ) {
				counter.incCounter();
				final JointPlan jp = jointPlanCreator.apply( proposition );
				if ( jp.getIndividualPlans().size() > 1 ) {
					jointPlans.addJointPlan( jp );
				}
				else if ( jointPlans.contains( jp ) ){
					jointPlans.removeJointPlan( jp );
				}

				for ( Plan p : jp.getIndividualPlans().values() ) {
					p.setScore(
							utility.utility(
									agents.get(
											p.getPerson().getId() ),
									proposition ) );
				}
			}
			if ( ++i % configGroup.getRemovalPeriod() == 0 ) {
				log.info( "Run plan remover..." );
				extraPlanRemover.removePlansInGroup( jointPlans , group );
				log.info( "Run plan remover... DONE" );
			}
		}
		agentCounter.printCounter();
		counter.printCounter();
		log.info( "Run plan remover..." );
		extraPlanRemover.removePlansInGroup( jointPlans , group );
		log.info( "Run plan remover... DONE" );
	}

	public GroupPlans doIteration() {
		log.info( "Creating Group for Coalition Selection" );

		final ReplanningGroup group = new ReplanningGroup();
		agents.getAllAgents().stream()
				.map( NegotiationAgent::getId )
				.map( population.getPersons()::get )
				.forEach( group::addPerson );

		log.info( "Running Selector" );
		return selector.selectPlans( jointPlans , group );
	}

	public void runIterations( Consumer<GroupPlans> callback ) {
		for ( int i=0; i < configGroup.getIterations(); i++ ) {
			callback.accept( doIteration() );
		}
	}

	public interface JointPlanCreator<P extends Proposition> extends Function<P,JointPlan> {}
}

