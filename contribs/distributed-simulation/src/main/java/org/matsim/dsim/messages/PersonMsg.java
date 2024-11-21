package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent source will create needed messages.
 */
@Data
@Builder(setterPrefix = "set", toBuilder = true)
@Deprecated
public class PersonMsg {

    private final int currentPlanElement;
    private final int currentRouteElement;
    private final Id<Person> id;

    @Builder.Default
    private final List<PlanElement> plan = new ArrayList<>();

}
