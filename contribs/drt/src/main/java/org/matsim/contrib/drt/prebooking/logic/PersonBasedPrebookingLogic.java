package org.matsim.contrib.drt.prebooking.logic;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
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
 * This is a prebooking logic that detects a person-level attribute which
 * indicates whether requests to a certain DRT mode for that person are
 * prebooked. It can be installed by calling
 * 
 * PersonBasedPrebookingLogic.install
 * 
 * on the controller with the respective DRT mode and a value indicating the
 * prebooking slack, i.e., the time between the expected departure and when the
 * request is submitted.
 */
public class PersonBasedPrebookingLogic implements PrebookingLogic, MobsimInitializedListener {
	static private final String ATTRIBUTE_PREFIX = "prebooking:";

	static public String getPersonAttribute(String mode) {
		return ATTRIBUTE_PREFIX + mode;
	}

	static public String getPersonAttribute(DrtConfigGroup drtConfig) {
		return getPersonAttribute(drtConfig.getMode());
	}

	static public boolean isPrebooked(String mode, Person person) {
		Boolean isPrebooked = (Boolean) person.getAttributes().getAttribute(getPersonAttribute(mode));
		return isPrebooked == true;
	}

	private final PrebookingQueue prebookingQueue;
	private final PopulationIteratorFactory populationIteratorFactory;
	private final TimeInterpretation timeInterpretation;

	private final String mode;
	private final double submissionSlack;

	private PersonBasedPrebookingLogic(String mode, PrebookingQueue prebookingQueue,
			PopulationIteratorFactory populationIteratorFactory, TimeInterpretation timeInterpretation,
			double submissionSlack) {
		this.prebookingQueue = prebookingQueue;
		this.populationIteratorFactory = populationIteratorFactory;
		this.mode = mode;
		this.timeInterpretation = timeInterpretation;
		this.submissionSlack = submissionSlack;

	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {
		PopulationIterator populationIterator = populationIteratorFactory.create();

		while (populationIterator.hasNext()) {
			var personItem = populationIterator.next();

			if (isPrebooked(mode, personItem.plan().getPerson())) {
				TimeTracker timeTracker = new TimeTracker(timeInterpretation);

				for (PlanElement element : personItem.plan().getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getMode().equals(mode)) {
							double earliestDepartureTime = leg.getDepartureTime().seconds();
							double submissionTime = earliestDepartureTime - submissionSlack;

							prebookingQueue.schedule(submissionTime, personItem.agent(), leg, earliestDepartureTime);
						}
					}

					timeTracker.addElement(element);
				}
			}
		}

		prebookingQueue.performInitialSubmissions();
	}

	static public AbstractDvrpModeQSimModule createModule(DrtConfigGroup drtConfig, double prebookingSlack) {
		return new AbstractDvrpModeQSimModule(drtConfig.getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PersonBasedPrebookingLogic.class).toProvider(modalProvider(getter -> {
					Preconditions.checkState(drtConfig.getPrebookingParams().isPresent());

					return new PersonBasedPrebookingLogic(drtConfig.getMode(), getter.getModal(PrebookingQueue.class),
							getter.getModal(PopulationIteratorFactory.class), getter.get(TimeInterpretation.class),
							prebookingSlack);
				}));
				addModalQSimComponentBinding().to(modalKey(PersonBasedPrebookingLogic.class));
			}
		};
	}

	static public void install(Controler controller, DrtConfigGroup drtConfig, double prebookingSlack) {
		controller.addOverridingQSimModule(createModule(drtConfig, prebookingSlack));
	}
}
