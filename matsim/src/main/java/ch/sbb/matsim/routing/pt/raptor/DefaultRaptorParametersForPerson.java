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
import org.matsim.core.config.Config;

import jakarta.inject.Inject;

/**
 * A default implementation of {@link RaptorParametersForPerson} returning the
 * same parameters for every person.
 *
 * @author mrieser / SBB
 */
public class DefaultRaptorParametersForPerson implements RaptorParametersForPerson {

    private final RaptorParameters defaultParameters;

    @Inject
    public DefaultRaptorParametersForPerson(Config config) {
        this.defaultParameters = RaptorUtils.createParameters(config);
    }

    @Override
    public RaptorParameters getRaptorParameters(Person person) {
        return this.defaultParameters;
    }
}
