package org.matsim.contrib.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will calculate the potential refund a PT user will get because of the upper bound of the daily fare (
 * e.g. ticket subscription). If the upper bound is reached, refund will be issued at the end of the
 * iteration (i.e. after Mobsim). In that case, we assume that PT user is a frequent traveller and will use subscription
 * instead of single tickets.
 * @author Chengqi Lu (luchengqi7)
 */
public class PtFareUpperBoundHandler implements PersonMoneyEventHandler, AfterMobsimListener {
    public static final String PT_REFUND = "pt fare refund";

    private double mobsimEndTime = Double.NaN;
    private final double upperBoundFactor;

    @Inject
    private EventsManager events;
    @Inject
    private QSimConfigGroup qSimConfigGroup;

    private final Map<Id<Person>, List<Double>> ptFareRecords = new HashMap<>();

    public PtFareUpperBoundHandler(double upperBoundFactor) {
        this.upperBoundFactor = upperBoundFactor;
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        if (event.getPurpose().equals(PtFareConfigGroup.PT_FARE)) {
            Id<Person> personId = event.getPersonId();
            ptFareRecords.computeIfAbsent(personId, l -> new ArrayList<>()).add(event.getAmount() * -1);
        }
    }

    @Override
    public void reset(int iteration) {
        ptFareRecords.clear();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        for (Id<Person> personId : ptFareRecords.keySet()) {
            double refund = calculateRefund(ptFareRecords.get(personId));
            if (refund > 0) {
                // Issue refund to person
                events.processEvent(
                        new PersonMoneyEvent(getOrCalcCompensationTime(), personId, refund,
                                PT_REFUND, TransportMode.pt, "Refund for person " + personId.toString()));
            }
        }
    }

    private double calculateRefund(List<Double> fares) {
        double sum = 0;
        double maxFare = 0;
        for (double fare : fares) {
            sum += fare;
            if (fare > maxFare) {
                maxFare = fare;
            }
        }
        double upperBound = maxFare * upperBoundFactor;
        if (sum > upperBound) {
            return sum - upperBound;
        }
        return 0;
    }

    private double getOrCalcCompensationTime() {
        if (Double.isNaN(this.mobsimEndTime)) {
            this.mobsimEndTime = (Double.isFinite(qSimConfigGroup.getEndTime().seconds()) && qSimConfigGroup.getEndTime().seconds() > 0)
                    ? qSimConfigGroup.getEndTime().seconds()
                    : Double.MAX_VALUE;
        }
        return this.mobsimEndTime;
    }
}
