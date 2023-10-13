package org.matsim.contrib.drt.extension.prebooking.logic;

import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.prebooking.dvrp.PrebookingManager;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Verify;

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

	private final Population population;
	private final TimeInterpretation timeInterpretation;

	private final double prbookingProbability;
	private final double submissionSlack;

	private final long randomSeed;

	public FixedSharePrebookingLogic(String mode, double prbookingProbability, double submissionSlack,
			PrebookingManager prebookingManager, Population population, TimeInterpretation timeInterpretation,
			long randomSeed) {
		super(prebookingManager);

		this.mode = mode;
		this.prbookingProbability = prbookingProbability;
		this.submissionSlack = submissionSlack;
		this.population = population;
		this.timeInterpretation = timeInterpretation;
		this.randomSeed = randomSeed;
	}

	@Override
	protected void scheduleRequests() {
		Random random = new Random(randomSeed);

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);
			boolean foundLeg = false;

			for (PlanElement element : plan.getPlanElements()) {
				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (leg.getMode().equals(mode) && random.nextDouble() < prbookingProbability) {
						Verify.verify(!foundLeg, "Person " + person.getId().toString()
								+ " has at least two drt legs. Please make use of a different PrebookingLogic.");
						foundLeg = true;

						double earliestDepartureTime = leg.getDepartureTime().seconds();
						double submissionTime = leg.getDepartureTime().seconds() - submissionSlack;
						queue.schedule(submissionTime, person, leg, earliestDepartureTime);
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

					return new FixedSharePrebookingLogic(mode, prebookingProbability, submissionSlack,
							prebookingManager, population, timeInterpretation, getConfig().global().getRandomSeed());
				}));
			}
		});
	}
}
