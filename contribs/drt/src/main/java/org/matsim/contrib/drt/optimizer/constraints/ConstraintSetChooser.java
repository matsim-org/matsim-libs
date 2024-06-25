package org.matsim.contrib.drt.optimizer.constraints;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface ConstraintSetChooser {

    Optional<DrtOptimizationConstraintsSet> chooseConstraintSet(double departureTime, Link accessActLink,
                                                                Link egressActLink, Person person, Attributes tripAttributes);
}
