package org.matsim.contrib.drt.extension.preemptive_rejection;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

public class PreemptiveRejectionOptimizer implements DrtOptimizer {
    static public final String CAUSE = "preemptive rejection";
    static public final String BOOKING_CLASS = "drt:bookingClass";

    private final String mode;
    private final DrtOptimizer delegate;

    private final Population population;
    private final EventsManager eventsManager;

    private final Random random;

    private final RejectionEntryContainer container;

    private double now;

    public PreemptiveRejectionOptimizer(String mode, DrtOptimizer delegate, EventsManager eventsManager,
            Population population, RejectionEntryContainer container) {
        this.mode = mode;
        this.delegate = delegate;
        this.eventsManager = eventsManager;
        this.population = population;
        this.random = MatsimRandom.getLocalInstance();
        this.container = container;
    }

    @Override
    public void requestSubmitted(Request rawRequest) {
        DrtRequest request = (DrtRequest) rawRequest;

        String bookingClass = getBookingClass(request);
        double rejectionRate = getPreemptiveRejectionRate(now, bookingClass);

        double u = random.nextDouble();
        boolean reject = u < rejectionRate;

        if (reject) {
            eventsManager.processEvent(new PassengerRequestRejectedEvent(now, mode, request.getId(),
                    ((DrtRequest) request).getPassengerIds(), CAUSE));
        } else {
            delegate.requestSubmitted(request);
        }
    }

    private double getPreemptiveRejectionRate(double time, String bookingClass) {
        RejectionEntry entry = null;

        for (RejectionEntry candidate : container.rejections) {
            if (time >= candidate.startTime && time < candidate.endTime) {
                if (candidate.bookingClass.equals(bookingClass)) {
                    Preconditions.checkState(entry == null,
                            "Duplicate entry for time " + time + " and booking class " + bookingClass);
                    entry = candidate;
                }
            }
        }

        return entry == null ? 0.0 : entry.rejectionRate;
    }

    @Override
    public void nextTask(DvrpVehicle vehicle) {
        delegate.nextTask(vehicle);
    }

    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
        now = e.getSimulationTime();
        delegate.notifyMobsimBeforeSimStep(e);
    }

    private String getBookingClass(DrtRequest request) {
        return getBookingClass(population, request.getPassengerIds());
    }

    static public String getBookingClass(Population population, Collection<Id<Person>> personIds) {
        String bookingClass = null;

        for (Id<Person> personId : personIds) {
            Person person = population.getPersons().get(personId);

            String personAttribute = getBookingClass(person);
            Preconditions.checkNotNull(personAttribute, "Person " + personId + " does not have a booking class");
            Preconditions.checkState(bookingClass == null || bookingClass.equals(personAttribute),
                    "Inconsistent booking classes for {"
                            + personIds.stream().map(Id::toString).collect(Collectors.joining(",")) + "}");

            bookingClass = personAttribute;
        }

        return bookingClass;
    }

    static public String getBookingClass(Person person) {
        return (String) person.getAttributes().getAttribute(BOOKING_CLASS);
    }

    static public void setBookingClass(Person person, String bookingClass) {
        person.getAttributes().putAttribute(BOOKING_CLASS, bookingClass);
    }

    static public class RejectionEntry {
        @JsonProperty("booking_class")
        public String bookingClass;

        @JsonProperty("start_time")
        public double startTime = Double.NEGATIVE_INFINITY;

        @JsonProperty("end_time")
        public double endTime = Double.POSITIVE_INFINITY;

        @JsonProperty("rejection_rate")
        public double rejectionRate = 0.0;
    }

    static public class RejectionEntryContainer {
        public List<RejectionEntry> rejections = new LinkedList<>();

        static public RejectionEntryContainer read(URL source) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(source, RejectionEntryContainer.class);
        }
    }
}
