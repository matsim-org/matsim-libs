package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.network.Link;

public interface AdditionalBicycleLinkScore{
	double computeLinkBasedScore( Link link );
}
