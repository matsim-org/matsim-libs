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
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

public class FixedSharePrebookingLogic implements MobsimBeforeSimStepListener {
	private final String mode;

	private final PrebookingManager prebookingManager;
	private final Population population;
	private final TimeInterpretation timeInterpretation;

	private final double prbookingProbability;
	private final double submissionSlack;

	private final long randomSeed;

	private boolean submissionDone = false;

	public FixedSharePrebookingLogic(String mode, double prbookingProbability, double submissionSlack,
			PrebookingManager prebookingManager, Population population, TimeInterpretation timeInterpretation,
			long randomSeed) {
		this.mode = mode;
		this.prbookingProbability = prbookingProbability;
		this.submissionSlack = submissionSlack;
		this.prebookingManager = prebookingManager;
		this.population = population;
		this.timeInterpretation = timeInterpretation;
		this.randomSeed = randomSeed;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		if (!submissionDone) {
			Random random = new Random(randomSeed);

			for (Person person : population.getPersons().values()) {
				Plan plan = person.getSelectedPlan();

				TimeTracker timeTracker = new TimeTracker(timeInterpretation);

				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						if (leg.getMode().equals(mode) && random.nextDouble() < prbookingProbability) {
							double earliestDepartureTime = leg.getDepartureTime().seconds();
							double submissionTime = leg.getDepartureTime().seconds() - submissionSlack;
							prebookingManager.prebook(person, leg, earliestDepartureTime, submissionTime);
						}
					}

					timeTracker.addElement(element);
				}
			}

			submissionDone = true;
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
