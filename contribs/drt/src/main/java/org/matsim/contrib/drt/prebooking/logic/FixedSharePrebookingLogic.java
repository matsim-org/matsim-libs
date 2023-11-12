package org.matsim.contrib.drt.prebooking.logic;

import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.prebooking.PrebookingManager;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * This class represents a prebooking logic that searches for DRT legs in the
 * population and decides based on a predefiend probability if each trip is
 * prebooked or not. Furthermore, you can configure how much in advance to the
 * planned departure time the request is submitted.
 * 
 * Note that this logic is aimed towards scenarios where each agent has one trip
 * (trip-based simulation). There is no logic that handles the special case
 * where an agent may submit a request in the morning and another one for the
 * afternoon, but the first one is rejected.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class FixedSharePrebookingLogic extends TimedPrebookingLogic {
	private final String mode;

	private final QSim qsim;
	private final TimeInterpretation timeInterpretation;
	private final Population population;

	private final double prebookingProbability;
	private final double submissionSlack;

	private final long randomSeed;

	public FixedSharePrebookingLogic(String mode, double prebookingProbability, double submissionSlack,
			PrebookingManager prebookingManager, Population population, TimeInterpretation timeInterpretation,
			long randomSeed, QSim qsim) {
		super(prebookingManager);

		this.mode = mode;
		this.prebookingProbability = prebookingProbability;
		this.submissionSlack = submissionSlack;
		this.timeInterpretation = timeInterpretation;
		this.randomSeed = randomSeed;
		this.qsim = qsim;
		this.population = population;

	}

	@Override
	protected void scheduleRequests() {
		Random random = new Random(randomSeed);

		PopulationIterator iterator = PopulationIterator.create(population, qsim);

		while (iterator.hasNext()) {
			var item = iterator.next();

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			for (PlanElement element : item.plan().getPlanElements()) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (leg.getMode().equals(mode) && random.nextDouble() < prebookingProbability) {
						double earliestDepartureTime = leg.getDepartureTime().seconds();
						double submissionTime = Double.isFinite(submissionSlack)
								? leg.getDepartureTime().seconds() - submissionSlack
								: timeInterpretation.getSimulationStartTime();
						queue.schedule(submissionTime, item.agent(), leg, earliestDepartureTime);
					}
				}

				timeTracker.addElement(element);
			}
		}
	}

	static public void install(String mode, double prebookingProbability, double submissionSlack,
			Controler controller) {
		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule(mode) {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(getter -> {
					PrebookingManager prebookingManager = getter.getModal(PrebookingManager.class);
					Population population = getter.get(Population.class);
					TimeInterpretation timeInterpretation = TimeInterpretation.create(getConfig());
					QSim qsim = getter.get(QSim.class);

					return new FixedSharePrebookingLogic(mode, prebookingProbability, submissionSlack,
							prebookingManager, population, timeInterpretation, getConfig().global().getRandomSeed(),
							qsim);
				}));
			}
		});
	}
}
