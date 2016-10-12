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

package playground.agarwalamit.opdyts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import javax.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.FilteredModalShareEventHandler;

/**
 * Created by amit on 20/09/16.
 */


public class ModalStatsControlerListner implements StartupListener, IterationEndsListener, ShutdownListener {

    private final FilteredModalShareEventHandler modalShareEventHandler = new FilteredModalShareEventHandler();
    private BufferedWriter writer;
    private final Set<String> mode2consider;

    public ModalStatsControlerListner(final Set<String> mode2optimize) {
        this.mode2consider = mode2optimize;
    }

    public ModalStatsControlerListner() {
        this.mode2consider = new HashSet<>();
        this.mode2consider.add(TransportMode.car);
        this.mode2consider.add(TransportMode.pt);
    }

    @Inject
    private EventsManager events;

    @Override
    public void notifyStartup(StartupEvent event) {
        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/modalStats.txt";
        writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("iterationNr" + "\t" +
                    "modes" + "\t" +
                    "numberOfLegs" + "\t" +
                    "modes" + "\t" +
                    "ascs" + "\t" +
                    "util_trav" + "\t" +
                    "util_dist" + "\t" +
                    "money_dist_rate");
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
        events.addHandler(modalShareEventHandler);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        SortedMap<String, Integer> mode2Legs = modalShareEventHandler.getMode2numberOflegs();

        try {
            writer.write(event.getIteration() + "\t" +
                    mode2Legs.keySet().toString() + "\t" +
                    mode2Legs.values().toString() + "\t");

            // write modalParams
            Map<String, PlanCalcScoreConfigGroup.ModeParams> mode2Params = event.getServices().getConfig().planCalcScore().getModes();

            SortedMap<String, Double> ascs = new TreeMap<>();
            SortedMap<String, Double> travs = new TreeMap<>();
            SortedMap<String, Double> dists = new TreeMap<>();
            SortedMap<String, Double> moneyRates = new TreeMap<>();

            for (String mode : mode2Params.keySet()) {
                if (mode2consider.contains(mode)) {
                    ascs.put(mode, mode2Params.get(mode).getConstant());
                    travs.put(mode, mode2Params.get(mode).getMarginalUtilityOfTraveling());
                    dists.put(mode, mode2Params.get(mode).getMarginalUtilityOfDistance());
                    moneyRates.put(mode, mode2Params.get(mode).getMonetaryDistanceRate());
                }
            }

            writer.write(ascs.keySet().toString() + "\t" +
                    ascs.values().toString() + "\t" +
                    travs.values().toString() + "\t" +
                    dists.values().toString() + "\t" +
                    moneyRates.values().toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
        modalShareEventHandler.reset(event.getIteration());
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

}
