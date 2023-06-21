package org.matsim.simwrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Mixing for Plotly components.
 */
@JsonNaming(PropertyNamingStrategies.LowerCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties({
	"engine", "zerolinewidth", "gridwidth", "showline", "showgrid", "constrain", "linecolor", "gridcolor",
	"fixedrange", "autorange", "rangemode", "visible", "zerolinecolor", "linewidth", "scaleratio"})
public abstract class ComponentMixin {
}
