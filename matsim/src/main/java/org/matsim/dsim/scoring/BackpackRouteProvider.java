package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Message;

public interface BackpackRouteProvider {

	BackpackRoute get();

	BackpackRoute get(Message fromMessage);
}
