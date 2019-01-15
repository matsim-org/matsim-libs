package org.matsim.contrib.emissions;

import com.google.inject.ProvidedBy;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.utils.EmissionUtils;

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
