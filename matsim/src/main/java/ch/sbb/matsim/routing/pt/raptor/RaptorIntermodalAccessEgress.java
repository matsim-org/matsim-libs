/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder.Direction;

import java.util.List;

/**
 * @author pmanser / SBB
 */
public interface RaptorIntermodalAccessEgress {

    RIntermodalAccessEgress calcIntermodalAccessEgress(List<? extends PlanElement> legs, RaptorParameters params, Person person, Direction direction);

    class RIntermodalAccessEgress {

        public final List<? extends PlanElement> routeParts;
        public final double disutility;
        public final double travelTime;
        public final Direction direction;

        public RIntermodalAccessEgress(List<? extends PlanElement> planElements, double disutility, double travelTime, Direction direction) {
            this.routeParts = planElements;
            this.disutility = disutility;
            this.travelTime = travelTime;
            this.direction = direction;
        }
    }
}
