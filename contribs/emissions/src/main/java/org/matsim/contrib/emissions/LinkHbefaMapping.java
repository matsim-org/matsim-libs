package org.matsim.contrib.emissions;

import org.matsim.api.core.v01.network.Link;

class LinkHbefaMapping extends HbefaRoadTypeMapping {
    @Override
    public String determineHebfaType(Link link) {
        return EmissionUtils.getHbefaRoadType(link);
    }
}
