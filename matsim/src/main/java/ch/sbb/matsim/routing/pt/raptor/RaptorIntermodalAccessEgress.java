/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.List;

/**
 * @author pmanser / SBB
 */
public interface RaptorIntermodalAccessEgress {

    RIntermodalAccessEgress calcIntermodalAccessEgress(List<? extends PlanElement> legs, RaptorParameters params, Person person);

    class RIntermodalAccessEgress {

        public final List<? extends PlanElement> routeParts;
        public final double disutility;
        public final double travelTime;

        public RIntermodalAccessEgress(List<? extends PlanElement> planElements, double disutility, double travelTime) {
            this.routeParts = planElements;
            this.disutility = disutility;
            this.travelTime = travelTime;
        }
    }
}
