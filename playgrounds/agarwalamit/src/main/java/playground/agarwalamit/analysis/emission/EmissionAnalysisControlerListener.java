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

package playground.agarwalamit.analysis.emission;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 01/12/2016.
 */

public class EmissionAnalysisControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

    public EmissionAnalysisControlerListener(final EmissionCostHandler emissionCostHandler, final EmissionModule emissionModule) {
        this.emissionCostHandler = emissionCostHandler;
        this.emissionModule = emissionModule;
    }

    private final EmissionCostHandler emissionCostHandler;
    private final EmissionModule emissionModule;
    private BufferedWriter writer ;

    @Override
    public void notifyStartup(StartupEvent event) {
        this.writer = IOUtils.getBufferedWriter(event.getServices().getConfig().controler().getOutputDirectory()+"/totalEmissionsCosts.txt");
        try {
            this.writer.write("ItNr\t");
            if(this.emissionCostHandler.isFiltering()) {
                for (MunichPersonFilter.MunichUserGroup munichUserGroup : MunichPersonFilter.MunichUserGroup.values()) {
                    this.writer.write(munichUserGroup.toString()+"\t");
                }
            }
            this.writer.write("total\n");
            this.writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }

        event.getServices().getEvents().addHandler(this.emissionModule.getWarmEmissionHandler());
        event.getServices().getEvents().addHandler(this.emissionModule.getColdEmissionHandler());

        emissionModule.getEmissionEventsManager().addHandler(this.emissionCostHandler);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        this.emissionCostHandler.reset(event.getIteration());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        Map<String, Double> userGrp2cost = this.emissionCostHandler.getUserGroup2TotalEmissionCosts();
        try {
            this.writer.write(event.getIteration()+"\t");
            if(this.emissionCostHandler.isFiltering()) {
                for (MunichPersonFilter.MunichUserGroup munichUserGroup : MunichPersonFilter.MunichUserGroup.values()) {
                    this.writer.write(userGrp2cost.get(munichUserGroup.toString()) + "\t");
                }
            }
            this.writer.write(MapUtils.doubleValueSum(userGrp2cost)+"\n");
            this.writer.flush();
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
