/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
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
