/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.OptionalTime;

import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder.Direction;

/**
 * A default implementation of {@link RaptorIntermodalAccessEgress} returning a new RIntermodalAccessEgress,
 * which contains a list of legs (same as in the input), the associated travel time as well as the disutility.
 *
 * @author pmanser / SBB
 */
public class DefaultRaptorIntermodalAccessEgress implements RaptorIntermodalAccessEgress {

    @Override
    public RIntermodalAccessEgress calcIntermodalAccessEgress(final List<? extends PlanElement> legs, RaptorParameters params, Person person, Direction direction) {
        double disutility = 0.0;
        double tTime = 0.0;
        for (PlanElement pe : legs) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();
                OptionalTime travelTime = ((Leg) pe).getTravelTime();
                if (travelTime.isDefined()) {
                    tTime += travelTime.seconds();
                    disutility += travelTime.seconds() * -params.getMarginalUtilityOfTravelTime_utl_s(mode);
                }
            }
            else if (pe instanceof Activity) {
            	if (((Activity) pe).getMaximumDuration().isDefined()) {
            		tTime += ((Activity) pe).getMaximumDuration().seconds();
            	}
            }
        }
        return new RIntermodalAccessEgress(legs, disutility, tTime, direction);
    }
}
