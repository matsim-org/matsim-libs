/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

/**
 * A default implementation of {@link RaptorParametersForPerson} returning the
 * same parameters for every person in the same subpopulation.
 *
 * @author mrieser / SBB
 */
public class DefaultRaptorParametersForPerson implements RaptorParametersForPerson {

    private final Config config;
    private final Map<String, RaptorParameters> parameters = new HashMap<>();

    @Inject
    public DefaultRaptorParametersForPerson(Config config) {
        this.config = config;
    }

    @Override
    public RaptorParameters getRaptorParameters(Person person) {
        final String subpopulation = PopulationUtils.getSubpopulation(person);
        return this.parameters.computeIfAbsent(
            subpopulation,
            (sp) -> RaptorUtils.createParameters(config, sp));
    }
}
