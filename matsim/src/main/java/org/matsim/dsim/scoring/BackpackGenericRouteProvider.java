package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;

class BackpackGenericRouteProvider implements BackpackRouteProvider {
	@Override
	public BackpackRoute get() {
		return new BackpackGenericRoute();
	}

	@Override
	public BackpackRoute get(Message fromMessage) {
		if (fromMessage instanceof BackpackGenericRoute egrb) {
			return egrb;
		}

		throw new IllegalArgumentException("Cannot create route builder from message of type " + fromMessage.getClass());
	}
}
