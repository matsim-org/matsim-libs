package playground.sebhoerl.avtaxi.scoring;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.scoring.SumScoringFunction;
import org.opengis.filter.capability.Operator;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.config.AVPriceStructureConfig;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.routing.AVRouteFactory;
import playground.sebhoerl.avtaxi.schedule.AVTransitEvent;

import java.util.*;

public class AVScoringFunction implements SumScoringFunction.ArbitraryEventScoring {
    final static Logger log = Logger.getLogger(AVScoringFunction.class);

    final private AVConfig config;
    final private double marginalUtilityOfWaiting;
    final private double marginalUtilityOfTraveling;
    final private double marginalUtilityOfMoney;

    final private Set<Id<AVOperator>> subscriptions = new HashSet<>();

    private AVScoringTrip scoringTrip = null;
    private double score = 0.0;

    final private Person person;

    public AVScoringFunction(AVConfig config, Person person, double marginalUtilityOfMoney, double marginalUtilityOfTraveling) {
        this.marginalUtilityOfWaiting = config.getMarginalUtilityOfWaitingTime() / 3600.0;
        this.marginalUtilityOfTraveling = marginalUtilityOfTraveling;
        this.marginalUtilityOfMoney = marginalUtilityOfMoney;
        this.config = config;
        this.person = person;
    }
    
    @Override
    public void handleEvent(Event event) {
        if (event instanceof PersonDepartureEvent) {
            if (((PersonDepartureEvent) event).getLegMode().equals(AVModule.AV_MODE)) {
                if (scoringTrip != null) {
                    throw new IllegalStateException();
                }

                scoringTrip = new AVScoringTrip();
                scoringTrip.processDeparture((PersonDepartureEvent) event);
            }
        } else if (event instanceof PersonEntersVehicleEvent) {
            if (scoringTrip != null) {
                scoringTrip.processEnterVehicle((PersonEntersVehicleEvent) event);
            }
        } else if (event instanceof AVTransitEvent) {
            if (scoringTrip != null) {
                scoringTrip.processTransit((AVTransitEvent) event);
            }
        }

        if (scoringTrip != null && scoringTrip.isFinished()) {
            handleScoringTrip(scoringTrip);
            scoringTrip = null;
        }
    }

    private AVPriceStructureConfig getPriceStructure(Id<AVOperator> id) {
        for (AVOperatorConfig oc : config.getOperatorConfigs()) {
            if (oc.getId().equals(id)) {
                return oc.getPriceStructureConfig();
            }
        }

        return null;
    }

    private void handleScoringTrip(AVScoringTrip trip) {
        score += computeWaitingTimeScoring(trip);
        score += computePricingScoring(trip);
    }

    private double computeWaitingTimeScoring(AVScoringTrip trip) {
        // Compensate for the travel disutility
        return (marginalUtilityOfWaiting - marginalUtilityOfTraveling) * trip.getWaitingTime();
    }

    static int noPricingWarningCount = 100;

    private double computePricingScoring(AVScoringTrip trip) {
        AVPriceStructureConfig priceStructure = getPriceStructure(trip.getOperatorId());

        double costs = 0.0;

        if (priceStructure != null) {
            double billableDistance = Math.max(1, Math.ceil(
                    trip.getDistance() / priceStructure.getSpatialBillingInterval()))
                    * priceStructure.getSpatialBillingInterval();

            double billableTravelTime = Math.max(1, Math.ceil(
                    trip.getInVehicleTravelTime() / priceStructure.getTemporalBillingInterval()))
                    * priceStructure.getTemporalBillingInterval();

            costs += (billableDistance / 1000.0) * priceStructure.getPricePerKm();
            costs += (billableTravelTime / 60.0) * priceStructure.getPricePerMin();
            costs += priceStructure.getPricePerTrip();

            if (priceStructure.getDailySubscriptionFee() > 0.0) {
                if (!subscriptions.contains(trip.getOperatorId())) {
                    costs += priceStructure.getDailySubscriptionFee();
                    subscriptions.add(trip.getOperatorId());
                }
            }
        } else if (noPricingWarningCount > 0) {
            log.warn("No pricing strategy defined for operator " + trip.getOperatorId().toString());
            noPricingWarningCount--;
        }

        return -costs * marginalUtilityOfMoney;
    }
    
    @Override
    public void finish() {}

    @Override
    public double getScore() {
        return score;
    }
}
