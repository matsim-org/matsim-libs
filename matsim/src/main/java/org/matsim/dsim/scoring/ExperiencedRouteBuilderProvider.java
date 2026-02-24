package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;

public interface ExperiencedRouteBuilderProvider {

	ExperiencedRouteBuilder get();

	ExperiencedRouteBuilder get(Message fromMessage);
}
