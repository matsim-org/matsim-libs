package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;

public class ExperiencedGenericRouteBuilderProvider implements ExperiencedRouteBuilderProvider {
	@Override
	public ExperiencedRouteBuilder get() {
		return new ExperiencedGenericRouteBuilder();
	}

	@Override
	public ExperiencedRouteBuilder get(Message fromMessage) {
		if (fromMessage instanceof ExperiencedGenericRouteBuilder egrb) {
			return egrb;
		}

		throw new IllegalArgumentException("Cannot create route builder from message of type " + fromMessage.getClass());
	}
}
