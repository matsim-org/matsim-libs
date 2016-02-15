/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.sim;

import org.matsim.core.config.ConfigGroup;
import playground.johannes.synpop.sim.Hamiltonian;

/**
 * @author johannes
 */
public class AnnealingHamiltonianConfigurator {

    public static AnnealingHamiltonian configure(Hamiltonian delegate, ConfigGroup configGroup) {
        AnnealingHamiltonian hamiltonian = new AnnealingHamiltonian(
                delegate,
                Double.parseDouble(configGroup.getValue("theta_min")),
                Double.parseDouble(configGroup.getValue("theta_max")));

        String value = configGroup.getValue("theta_factor");
        if(value != null) hamiltonian.setThetaFactor(Double.parseDouble(value));

        value = configGroup.getValue("delta_interval");
        if(value != null) hamiltonian.setDeltaInterval((long)Double.parseDouble(value));

        value = configGroup.getValue("delta_threshold");
        if(value != null) hamiltonian.setDeltaThreshold(Double.parseDouble(value));

        value = configGroup.getValue("startIteration");
        if(value != null) hamiltonian.setStartIteration((long)Double.parseDouble(value));

        return hamiltonian;
    }
}
