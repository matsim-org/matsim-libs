package org.matsim.simwrapper;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Exclude all properties except these from the map.
 */
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties("engine")
public abstract class ComponentMixin {


}
