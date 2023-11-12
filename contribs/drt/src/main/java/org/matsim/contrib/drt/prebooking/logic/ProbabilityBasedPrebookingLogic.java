package org.matsim.contrib.drt.prebooking.logic;

import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.prebooking.logic.helpers.PopulationIterator;
import org.matsim.contrib.drt.prebooking.logic.helpers.PopulationIterator.PopulationIteratorFactory;
import org.matsim.contrib.drt.prebooking.logic.helpers.PrebookingQueue;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Preconditions;

/**
 * This class represents a prebooking logic that searches for DRT legs in the
 * population and decides based on a predefiend probability if each trip is
 * prebooked or not. Furthermore, you can configure how much in advance to the
 * planned departure time the request is submitted using the submissionSlack
 * parameter.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ProbabilityBasedPrebookingLogic implements PrebookingLogic, MobsimInitializedListener {
	private final PrebookingQueue prebookingQueue;
	private final PopulationIteratorFactory populationIteratorFactory;
	private final TimeInterpretation timeInterpretation;
	private final Random random;

	private final String mode;
	private final double probability;
	private final double submissionSlack;

	private ProbabilityBasedPrebookingLogic(String mode, PrebookingQueue prebookingQueue,
			PopulationIteratorFactory populationIteratorFactory, TimeInterpretation timeInterpretation, Random random,
			double probability, double submissionSlack) {
		this.prebookingQueue = prebookingQueue;
		this.populationIteratorFactory = populationIteratorFactory;
		this.mode = mode;
		this.timeInterpretation = timeInterpretation;
		this.random = random;
		this.probability = probability;
		this.submissionSlack = submissionSlack;
	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {
		PopulationIterator populationIterator = populationIteratorFactory.create();

		while (populationIterator.hasNext()) {
			var personItem = populationIterator.next();

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			for (PlanElement element : personItem.plan().getPlanElements()) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (mode.equals(leg.getMode()) && random.nextDouble() <= probability) {
						double departureTime = timeTracker.getTime().seconds();
						double submissionTime = departureTime - submissionSlack;

						prebookingQueue.schedule(submissionTime, personItem.agent(), leg, departureTime);
					}
				}

				timeTracker.addElement(element);
			}
		}

		prebookingQueue.performInitialSubmissions();
	}

	static public AbstractDvrpModeQSimModule createModule(DrtConfigGroup drtConfig, double probability,
			double submissionSlack) {
		return new AbstractDvrpModeQSimModule(drtConfig.getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(ProbabilityBasedPrebookingLogic.class).toProvider(modalProvider(getter -> {
					Preconditions.checkState(drtConfig.getPrebookingParams().isPresent());

					return new ProbabilityBasedPrebookingLogic(drtConfig.getMode(),
							getter.getModal(PrebookingQueue.class), getter.getModal(PopulationIteratorFactory.class),
							getter.get(TimeInterpretation.class), new Random(getConfig().global().getRandomSeed()),
							probability, submissionSlack);
				}));
				addModalQSimComponentBinding().to(modalKey(ProbabilityBasedPrebookingLogic.class));
			}
		};
	}

	static public void install(Controler controller, DrtConfigGroup drtConfig, double probability,
			double prebookingSlack) {
		controller.addOverridingQSimModule(createModule(drtConfig, probability, prebookingSlack));
	}
}
