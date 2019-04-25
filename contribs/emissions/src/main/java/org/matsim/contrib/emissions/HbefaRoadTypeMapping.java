package org.matsim.contrib.emissions;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by molloyj on 01.12.2017.
 */
abstract class HbefaRoadTypeMapping {

    public void addHbefaMappings(Network network) {
        for (Link link : network.getLinks().values()) {
            String hbefaString = determineHebfaType(link);
            if (hbefaString != null) {
                EmissionUtils.setHbefaRoadType(link, hbefaString);
            }
        }
    }

    protected abstract String determineHebfaType(Link link);

}
