package org.matsim.contrib.drt.extension.prebooking.logic;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.prebooking.dvrp.PrebookingManager;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Verify;

/**
 * This class represents a prebooking logic that will search for the
 * "prebooking:submissionTime" attribute in the origin activity of a trip. If a
 * DRT leg is found in the trip, the submission time for this request will be
 * read from the attribute. In order to make use of prebooked requests, you need
 * to define these attributes before the start of the QSim iteration.
 * 
 * To indicate the expected departure time (for which the vehicle is requested),
 * you can also define the "prebooking:plannedDepartureTime" attribute. If it is
 * not specified, the logic will try to figure out the departure time by
 * traversing the activities and legs according to the configured time
 * interpretation.
 * 
 * Note that this logic is aimed towards scenarios where each agent has one trip
 * (trip-based simulation). There is no logic that handles the special case
 * where an agent may submit a request in the morning and another one for the
 * afternoon, but the first one is rejected.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AttributePrebookingLogic extends TimedPrebookingLogic {
	static public final String SUBMISSION_TIME_ATTRIBUTE = "prebooking:submissionTime";
	static public final String PLANNED_DEPARTURE_ATTRIBUTE = "prebooking:plannedDepartureTime";

	private final String mode;

	private final Population population;
	private final TimeInterpretation timeInterpretation;
	private final QSim qsim;

	public AttributePrebookingLogic(String mode, PrebookingManager prebookingManager, Population population,
			TimeInterpretation timeInterpretation, QSim qsim) {
		super(prebookingManager);

		this.mode = mode;
		this.population = population;
		this.timeInterpretation = timeInterpretation;
		this.qsim = qsim;
	}

	@Override
	protected void scheduleRequests() {
		PopulationIterator iterator = PopulationIterator.create(population, qsim);

		while (iterator.hasNext()) {
			var item = iterator.next();

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			Double submissionTime = null;
			Double plannedDepartureTime = null;
			boolean foundLeg = false;

			for (PlanElement element : item.plan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (!TripStructureUtils.isStageActivityType(activity.getType())) {
						foundLeg = false;
						submissionTime = (Double) activity.getAttributes().getAttribute(SUBMISSION_TIME_ATTRIBUTE);
						plannedDepartureTime = (Double) activity.getAttributes()
								.getAttribute(PLANNED_DEPARTURE_ATTRIBUTE);
					}
				}

				if (element instanceof Leg) {
					Leg leg = (Leg) element;

					if (leg.getMode().equals(mode)) {
						Verify.verify(!foundLeg, "Person " + item.agent().getId().toString()
								+ " has at least two drt legs in one trip.");
						foundLeg = true;

						if (plannedDepartureTime == null) {
							plannedDepartureTime = timeTracker.getTime().seconds();
						}

						if (submissionTime != null) {
							queue.schedule(submissionTime, item.agent(), leg, plannedDepartureTime);
						}
					}
				}

				timeTracker.addElement(element);
			}
		}
	}

	static public void install(String mode, Controler controller) {
		controller.addOverridingQSimModule(new AbstractDvrpModeQSimModule(mode) {
			@Override
			protected void configureQSim() {
				addModalQSimComponentBinding().toProvider(modalProvider(getter -> {
					PrebookingManager prebookingManager = getter.getModal(PrebookingManager.class);
					Population population = getter.get(Population.class);
					TimeInterpretation timeInterpretation = TimeInterpretation.create(getConfig());
					QSim qsim = getter.get(QSim.class);

					return new AttributePrebookingLogic(mode, prebookingManager, population, timeInterpretation, qsim);
				}));
			}
		});
	}

}
