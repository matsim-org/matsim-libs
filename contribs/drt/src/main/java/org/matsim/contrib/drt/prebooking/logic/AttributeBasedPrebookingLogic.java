package org.matsim.contrib.drt.prebooking.logic;

import java.util.Optional;

import org.matsim.api.core.v01.population.Activity;
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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Preconditions;

/**
 * This class represents a prebooking logic that will search for the
 * "prebooking:submissionTime:[mode]" attribute in the origin activity of a
 * trip. If a DRT leg is found in the trip, the submission time for this request
 * will be read from the attribute. In order to make use of prebooked requests,
 * you need to define these attributes before the start of the QSim iteration.
 * 
 * To indicate the expected departure time (for which the vehicle is requested),
 * you can also define the "prebooking:plannedDepartureTime:[mode]" attribute.
 * If it is not specified, the logic will try to figure out the departure time
 * by traversing the activities and legs according to the configured time
 * interpretation.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class AttributeBasedPrebookingLogic implements PrebookingLogic, MobsimInitializedListener {
	static private final String SUBMISSION_TIME_ATTRIBUTE_PREFIX = "prebooking:submissionTime:";
	static private final String PLANNED_DEPARTURE_ATTRIBUTE_PREFIX = "prebooking:plannedDepartureTime:";

	static public String getSubmissionTimeAttribute(String mode) {
		return SUBMISSION_TIME_ATTRIBUTE_PREFIX + mode;
	}

	static public String getPlannedDepartureTimeAttribute(String mode) {
		return PLANNED_DEPARTURE_ATTRIBUTE_PREFIX + mode;
	}

	static public Optional<Double> getSubmissionTime(String mode, Trip trip) {
		return Optional.ofNullable((Double) trip.getTripAttributes().getAttribute(getSubmissionTimeAttribute(mode)));
	}

	static public Optional<Double> getPlannedDepartureTime(String mode, Trip trip) {
		return Optional
				.ofNullable((Double) trip.getTripAttributes().getAttribute(getPlannedDepartureTimeAttribute(mode)));
	}

	static public void setSubmissionTime(String mode, Trip trip, double submissionTime) {
		trip.getTripAttributes().putAttribute(getSubmissionTimeAttribute(mode), submissionTime);
	}

	static public void setPlannedDepartureTime(String mode, Trip trip, double plannedDepartureTime) {
		trip.getTripAttributes().putAttribute(getPlannedDepartureTimeAttribute(mode), plannedDepartureTime);
	}

	static public void setSubmissionTime(String mode, Activity originActivity, double submissionTime) {
		originActivity.getAttributes().putAttribute(getSubmissionTimeAttribute(mode), submissionTime);
	}

	static public void setPlannedDepartureTime(String mode, Activity originActivity, double plannedDepartureTime) {
		originActivity.getAttributes().putAttribute(getPlannedDepartureTimeAttribute(mode), plannedDepartureTime);
	}

	private final PrebookingQueue prebookingQueue;
	private final PopulationIteratorFactory populationIteratorFactory;
	private final TimeInterpretation timeInterpretation;

	private final String mode;

	private AttributeBasedPrebookingLogic(String mode, PrebookingQueue prebookingQueue,
			PopulationIteratorFactory populationIteratorFactory, TimeInterpretation timeInterpretation) {
		this.prebookingQueue = prebookingQueue;
		this.populationIteratorFactory = populationIteratorFactory;
		this.mode = mode;
		this.timeInterpretation = timeInterpretation;

	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {
		PopulationIterator populationIterator = populationIteratorFactory.create();

		while (populationIterator.hasNext()) {
			var personItem = populationIterator.next();

			TimeTracker timeTracker = new TimeTracker(timeInterpretation);

			for (Trip trip : TripStructureUtils.getTrips(personItem.plan())) {
				timeTracker.addActivity(trip.getOriginActivity());
				boolean foundLeg = false;

				for (PlanElement element : trip.getTripElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (mode.equals(leg.getMode())) {
							Preconditions.checkState(!foundLeg,
									"Attribute-based prebooking logic only works with one DRT leg per trip");
							foundLeg = true;

							Optional<Double> submissionTime = getSubmissionTime(mode, trip);
							Optional<Double> plannedDepartureTime = getPlannedDepartureTime(mode, trip);

							if (submissionTime.isPresent()) {
								if (plannedDepartureTime.isPresent()) {
									Preconditions.checkState(plannedDepartureTime.get() > submissionTime.get(),
											"Planned departure time must be after submission time");
								}

								prebookingQueue.schedule(submissionTime.get(), personItem.agent(), leg,
										plannedDepartureTime.orElse(timeTracker.getTime().seconds()));
							}
						}
					}

					timeTracker.addElement(element);
				}

			}
		}

		prebookingQueue.performInitialSubmissions();
	}

	static public AbstractDvrpModeQSimModule createModule(DrtConfigGroup drtConfig) {
		return new AbstractDvrpModeQSimModule(drtConfig.getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(AttributeBasedPrebookingLogic.class).toProvider(modalProvider(getter -> {
					Preconditions.checkState(drtConfig.getPrebookingParams().isPresent());

					return new AttributeBasedPrebookingLogic(drtConfig.getMode(),
							getter.getModal(PrebookingQueue.class), getter.getModal(PopulationIteratorFactory.class),
							getter.get(TimeInterpretation.class));
				}));
				addModalQSimComponentBinding().to(modalKey(AttributeBasedPrebookingLogic.class));
			}
		};
	}

	static public void install(Controler controller, DrtConfigGroup drtConfig) {
		controller.addOverridingQSimModule(createModule(drtConfig));
	}
}
