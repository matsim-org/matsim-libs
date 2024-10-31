package org.matsim.api;

import org.matsim.dsim.simulation.SimPerson;

@FunctionalInterface
public interface NextStateHandler {

    void accept(SimPerson person, double now);
}
