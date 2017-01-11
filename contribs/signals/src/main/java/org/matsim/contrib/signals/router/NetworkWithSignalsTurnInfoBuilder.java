/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.signals.router;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.*;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkExpandNode.TurnInfo;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;

import com.google.inject.Inject;


/**
 * Do bind(NetworkTurnInfoBuilder.class).to(NetworkWithSignalsTurnInfoBuilder.class)
 * if the extended NetworkWithSignalsTurnInfoBuilder (with
 * SignalsData information) should be used for building turning restriction.
 * 
 * Otherwise the default NetworkTurnInfoBuilder is used for link to link routing.
 *
 * @author nagel, michalm
 */
public class NetworkWithSignalsTurnInfoBuilder
    extends NetworkTurnInfoBuilder
{
    @Inject
    public NetworkWithSignalsTurnInfoBuilder(Scenario scenario)
    {
        super(scenario);
    }


    @Override
    public Map<Id<Link>, List<TurnInfo>> createAllowedTurnInfos()
    {
        Map<Id<Link>, List<TurnInfo>> allowedInLinkTurnInfoMap = super.createAllowedTurnInfos();

        final SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(
                scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME,
                SignalSystemsConfigGroup.class);

        if (signalsConfig.isUseSignalSystems()) {
            SignalSystemsData ssd = ((SignalsData)scenario
                    .getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData();
            Map<Id<Link>, List<TurnInfo>> signalsTurnInfoMap = SignalsTurnInfoBuilder
                    .createSignalsTurnInfos(ssd);
            mergeTurnInfoMaps(allowedInLinkTurnInfoMap, signalsTurnInfoMap);
        }
        return allowedInLinkTurnInfoMap;
    }
}
