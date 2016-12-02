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

package playground.agarwalamit.analysis.emission.experienced;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;

/**
 * Created by amit on 01/12/2016.
 */

public class ExperiencedEmissionAnalysisControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

    public ExperiencedEmissionAnalysisControlerListener(final EmissionResponsibilityCostModule ecm, final EmissionModule emissionModule, final PersonFilter pf) {
        this.causedEmissionCostHandler = new ExperiencedEmissionCostHandler(ecm, pf);
        this.emissionModule = emissionModule;
    }

    public ExperiencedEmissionAnalysisControlerListener(final EmissionResponsibilityCostModule ecm, final EmissionModule emissionModule) {
        this(ecm, emissionModule, null);
    }

    private final ExperiencedEmissionCostHandler causedEmissionCostHandler;
    private final EmissionModule emissionModule;
    private BufferedWriter writer ;

    @Inject private Scenario scenario;

    @Override
    public void notifyStartup(StartupEvent event) {
        this.writer = writer = IOUtils.getBufferedWriter(event.getServices().getConfig().controler().getOutputDirectory()+"/userGroup2EmissionsCosts.txt");
        try {
            this.writer.write("ItNr\t");
            for (MunichPersonFilter.MunichUserGroup munichUserGroup : MunichPersonFilter.MunichUserGroup.values()) {
                this.writer.write(munichUserGroup.toString()+"\t");
            }
            this.writer.write("total\n");
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }

        event.getServices().getEvents().addHandler(this.emissionModule.getWarmEmissionHandler());
        event.getServices().getEvents().addHandler(this.emissionModule.getColdEmissionHandler());

        emissionModule.getEmissionEventsManager().addHandler(this.causedEmissionCostHandler);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        Map<String, Double> userGrp2cost = this.causedEmissionCostHandler.getUserGroup2TotalEmissionCosts();
        try {
            this.writer.write(event.getIteration()+"\t");
            for (MunichPersonFilter.MunichUserGroup munichUserGroup : MunichPersonFilter.MunichUserGroup.values()) {
                this.writer.write(userGrp2cost.get(munichUserGroup.toString())+"\t");
            }
            this.writer.write(MapUtils.doubleValueSum(userGrp2cost)+"\n");
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
