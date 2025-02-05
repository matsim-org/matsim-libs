package org.matsim.core.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.messages.ScoringMessage;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationPartition;
import org.matsim.api.core.v01.population.SubsetPopulation;
import org.matsim.core.communication.Communicator;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.serialization.SerializationProvider;

import java.util.List;
import java.util.Map;

/**
 * Performs the functionality of {@link PlansScoringImpl} in a distributed simulation.
 */
public class DistributedScoringListener implements ScoringListener {

	@Inject
	private Communicator comm;

	@Inject
	private ComputeNode computeNode;

	@Inject
	private SerializationProvider serializer;

	@Inject
	private ScoringFunctionsForPopulation scoringFunctionsForPopulation;

	@Inject
	private NewScoreAssigner newScoreAssigner;

	@Inject
	private Population population;

	@Inject
	private PopulationPartition partition;

	@Override
	public double priority() {
		return 100;
	}

	@Override
	public void notifyScoring(ScoringEvent event) {

		if (computeNode.isHeadNode()) {

			// The head node is responsible for scoring the entire population

			scoringFunctionsForPopulation.finishScoringFunctions();
			newScoreAssigner.assignNewScores(event.getIteration(), this.scoringFunctionsForPopulation, this.population);

			ScoringMessage msg = new ScoringMessage();

			for (Person person : population.getPersons().values()) {

				ScoringFunction f = scoringFunctionsForPopulation.getScoringFunctionForAgent(person.getId());
				msg.addScore(person.getId(), f.getScore());
			}

			comm.allGather(msg, 10, serializer);
		} else {

			// Other nodes receive scores from the head node and apply them to their subset of the population

			SubsetPopulation subset = new SubsetPopulation(population, partition::contains);
			List<ScoringMessage> scores = comm.allGather(new ScoringMessage(), 10, serializer);

			for (ScoringMessage msg : scores) {
				for (Map.Entry<Id<Person>, Double> e : msg.getPersonScores().entrySet()) {
					scoringFunctionsForPopulation.putScoringFunctionForAgent(e.getKey(), new FixedScoringFunction(e.getValue()));
				}
			}

			newScoreAssigner.assignNewScores(event.getIteration(), this.scoringFunctionsForPopulation, subset);
		}
	}
}
