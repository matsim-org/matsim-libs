/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;

/**
 * @author mrieser / SBB
 */
public interface RaptorParametersForPerson {
    RaptorParameters getRaptorParameters(Person person);
}
