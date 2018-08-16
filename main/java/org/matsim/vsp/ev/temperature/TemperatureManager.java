/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.vsp.ev.temperature;/*
 * created by jbischoff, 15.08.2018
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;
import java.net.URL;
import java.util.*;

public class TemperatureManager implements MobsimBeforeSimStepListener, MobsimInitializedListener {

    private Queue<TemperatureChange> temperatureChanges = new LinkedList<>();
    private TemperatureChange nextTemperatureChange;
    private List<TemperatureChange> temperatureChangeList = new ArrayList<>();

    @Inject
    private EventsManager events;

    @Inject
    public TemperatureManager(Config config) {
        TemperatureChangeConfigGroup temperatureChangeConfigGroup = (TemperatureChangeConfigGroup) config.getModules().get(TemperatureChangeConfigGroup.GROUP_NAME);
        readTemperatureFile(temperatureChangeConfigGroup.getTemperatureFileURL(config.getContext()), temperatureChangeConfigGroup.getDelimiter());
    }

    private void readTemperatureFile(URL temperatureFileURL, String delimiter) {
        TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
        tabularFileParserConfig.setFileName(temperatureFileURL.getFile());
        tabularFileParserConfig.setDelimiterTags(new String[]{delimiter});
        new TabularFileParser().parse(tabularFileParserConfig, new TabularFileHandler() {
            @Override
            public void startRow(String[] row) {
                double time = Time.parseTime(row[0]);
                Id<Link> linkId = Id.createLinkId(row[1]);
                double temperature = Double.parseDouble(row[2]);
                TemperatureChange temperatureChange = new TemperatureChange(time, temperature, linkId);
                temperatureChangeList.add(temperatureChange);
            }
        });
        Collections.sort(temperatureChangeList);
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        if (nextTemperatureChange != null) {
            if (e.getSimulationTime() == nextTemperatureChange.time) {
                events.processEvent(new TemperatureChangeEvent(e.getSimulationTime(), nextTemperatureChange.linkId, nextTemperatureChange.temperature));
                nextTemperatureChange = temperatureChanges.poll();
                notifyMobsimBeforeSimStep(e);
            }
        }
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        for (TemperatureChange t : temperatureChangeList) {
            temperatureChanges.add(t);
        }
        nextTemperatureChange = temperatureChanges.poll();
    }

    class TemperatureChange implements Comparable<TemperatureChange> {
        Double time;
        double temperature;
        Id<Link> linkId;

        public TemperatureChange(double time, double temperature, Id<Link> linkId) {
            this.time = time;
            this.temperature = temperature;
            this.linkId = linkId;
        }

        @Override
        public int compareTo(TemperatureChange o) {
            return time.compareTo(o.time);
        }
    }
}
