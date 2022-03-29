/* *********************************************************************** *
 * project: org.matsim.*
 * PKMbyModeCalculator.java
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

package org.matsim.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.utils.charts.StackedBarChart;

/**
 * analyses passenger hours traveled based on experienced plans.
 * @author vsp-gleich
 */
public class PHbyModeCalculator {

    private final Map<Integer,Map<String,TravelTimeAndWaitTime>> phtPerIteration = new TreeMap<>();
    private final boolean writePng;
    private final OutputDirectoryHierarchy controlerIO;
    private final static char DEL = '\t';
    private final static String FILENAME = "ph_modestats";

    private static final String TRAVEL_TIME_SUFFIX = "_travel";
    private static final String WAIT_TIME_SUFFIX = "_wait";
    private static final String STAGE_ACTIVITY = "stageActivity";

    @Inject
    PHbyModeCalculator(ControlerConfigGroup controlerConfigGroup, OutputDirectoryHierarchy controlerIO) {
        writePng = controlerConfigGroup.isCreateGraphs();
        this.controlerIO = controlerIO;
    }

    void addIteration(int iteration, IdMap<Person, Plan> map) {
        Map<String,TravelTimeAndWaitTime> phtbyMode = map.values()
                .parallelStream()
                .flatMap(plan -> plan.getPlanElements().stream())
                .map(pe->{
                	if (pe instanceof Leg) {
                        Leg leg = (Leg) pe;
                        double travelTime = 0.0;
                        double waitTime = 0.0;
                        if (leg.getRoute()!=null) {
							travelTime = leg.getRoute().getTravelTime().seconds();
                            double enterVehicleTime = Double.NaN;
                            Object attr = leg.getAttributes().getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME);
                            if (attr != null) {
                            	enterVehicleTime = (Double) attr;
                            }
							waitTime = enterVehicleTime - leg.getDepartureTime().seconds();
                            if (!Double.isFinite(waitTime)) {waitTime = 0.0;}
                            if (waitTime >= 0.0) {
                            	travelTime -= waitTime;
                            } else {
								throw new RuntimeException("negative wait time" + enterVehicleTime + " " + leg.getDepartureTime()
										.seconds());
                            }
                        }

                        if (Double.isNaN(travelTime)) {travelTime = 0.0; }

                        return new AbstractMap.SimpleEntry<>(leg.getMode(),new TravelTimeAndWaitTime(travelTime, waitTime));
                        
                	} else if (pe instanceof Activity) {
                		Activity act = (Activity) pe;
                		if (StageActivityTypeIdentifier.isStageActivity(act.getType())) {
                            double duration = act.getEndTime().orElse(0) - act.getStartTime().orElse(0);
                            return new AbstractMap.SimpleEntry<>(STAGE_ACTIVITY,new TravelTimeAndWaitTime(0.0, duration));
                		}
                	}
                	return new AbstractMap.SimpleEntry<>(STAGE_ACTIVITY,new TravelTimeAndWaitTime(0.0, 0.0));
                })
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue(),(a,b)->TravelTimeAndWaitTime.sum(a, b)));
        phtPerIteration.put(iteration,phtbyMode);
    }


    void writeOutput() {
        writePHTText();

    }

    private void writePHTText() {
        TreeSet<String> allModes = new TreeSet<>();
        allModes.addAll(this.phtPerIteration.values()
                .stream()
                .flatMap(i->i.keySet().stream())
                .collect(Collectors.toSet()));

        try (CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getOutputFilename( FILENAME+ ".txt"))), CSVFormat.DEFAULT.withDelimiter(DEL))) {
            csvPrinter.print("Iteration");
            for (String mode: allModes) {
                csvPrinter.print(mode + TRAVEL_TIME_SUFFIX);
                csvPrinter.print(mode + WAIT_TIME_SUFFIX);
            }
            csvPrinter.println();
            
            for (Map.Entry<Integer,Map<String,TravelTimeAndWaitTime>> e : phtPerIteration.entrySet()){
                csvPrinter.print(e.getKey());
                for (String mode : allModes){
					TravelTimeAndWaitTime travelTimeAndWaitTime = e.getValue().getOrDefault(mode, new TravelTimeAndWaitTime(0.0, 0.0));
					csvPrinter.print((int) Math.round(travelTimeAndWaitTime.travelTime / 3600.0));
					csvPrinter.print((int) Math.round(travelTimeAndWaitTime.waitTime / 3600.0));
				}
                csvPrinter.println();
            }


        } catch (IOException e) {
			Logger.getLogger(getClass()).error("Could not write PH Modestats.");
		}
        if (writePng){
            String[] categories = new String[phtPerIteration.size()];
            int i = 0;
            for (Integer it : phtPerIteration.keySet()){
                categories[i++] = it.toString();
            }

            StackedBarChart chart = new StackedBarChart("Passenger hours traveled per Mode","Iteration","person hours",categories);
            //rotate x-axis by 90degrees
            chart.getChart().getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

            for (String mode : allModes){
                double[] valueTravelTime =  phtPerIteration.values().stream()
                        .mapToDouble(k->k.getOrDefault(mode,new TravelTimeAndWaitTime(0.0, 0.0)).travelTime/1000.0)
                        .toArray();
                chart.addSeries(mode + TRAVEL_TIME_SUFFIX, valueTravelTime);
                double[] valueWaitTime =  phtPerIteration.values().stream()
                        .mapToDouble(k->k.getOrDefault(mode,new TravelTimeAndWaitTime(0.0, 0.0)).waitTime/1000.0)
                        .toArray();
                chart.addSeries(mode + WAIT_TIME_SUFFIX, valueWaitTime);
            }
            chart.addMatsimLogo();
            chart.saveAsPng(controlerIO.getOutputFilename(FILENAME+ ".png"), 1024, 768);

        }

    }
    
    private static class TravelTimeAndWaitTime {
    	private double travelTime;
    	private double waitTime;
    	
    	private TravelTimeAndWaitTime(double travelTime, double waitTime) {
    		this.travelTime = travelTime;
    		this.waitTime = waitTime;
    	}
    	
    	private static TravelTimeAndWaitTime sum(TravelTimeAndWaitTime object1, TravelTimeAndWaitTime object2) {
    		return new TravelTimeAndWaitTime(object1.travelTime + object2.travelTime, object1.waitTime + object2.waitTime);
    	}
    }
}

