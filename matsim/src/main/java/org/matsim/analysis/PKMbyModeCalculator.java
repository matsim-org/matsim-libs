package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jfree.chart.util.ArrayUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.StackedBarChart;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class PKMbyModeCalculator {

    private final Map<Integer,Map<String,Double>> pmtPerIteration = new TreeMap<>();
    private final boolean writePng;
    private final OutputDirectoryHierarchy controlerIO;
    private final static char DEL = '\t';
    private final DecimalFormat df = new DecimalFormat();
    final String filename = "pkm_ModeStats";


    @Inject
    PKMbyModeCalculator(ControlerConfigGroup controlerConfigGroup, OutputDirectoryHierarchy controlerIO) {
    writePng = controlerConfigGroup.isCreateGraphs();
    this.controlerIO = controlerIO;
    }

    public void addIteration(int iteration, Map<Id<Person>, Plan> map) {
        Map<String,Double> pmtbyMode = map.values()
                .parallelStream()
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(Leg.class::isInstance)
                .map(l->{
                    Leg leg = (Leg) l;
                    double dist = leg.getRoute()!=null?leg.getRoute().getDistance():0;
                    if (Double.isNaN(dist)) {dist = 0.0; }
                    return new AbstractMap.SimpleEntry<>(leg.getMode(),dist);
                })
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue(),(a,b)->a+b));
        pmtPerIteration.put(iteration,pmtbyMode);
    }



    public void writeOutput() {
         writeVKTText();
        
    }

    private void writeVKTText() {
        TreeSet<String> allModes = new TreeSet<>();
        allModes.addAll(this.pmtPerIteration.values()
                                .stream()
                                .flatMap(i->i.keySet().stream())
                                .collect(Collectors.toSet()));

        try(CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getOutputFilename(filename + ".txt"))), CSVFormat.DEFAULT.withDelimiter(DEL)))

        {
            csvPrinter.print("Iteration");
            csvPrinter.printRecord(allModes);

            for (Map.Entry<Integer,Map<String,Double>> e : pmtPerIteration.entrySet()){
                csvPrinter.print(e.getKey());
            for (String mode : allModes){
                csvPrinter.print(df.format(e.getValue().getOrDefault(mode,0.0)/1000.0));
            }
                csvPrinter.println();
        }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (writePng){
            String[] categories = new String[pmtPerIteration.size()];
            int i = 0;
            for (Integer it : pmtPerIteration.keySet()){
                categories[i++] = it.toString();
                }

            StackedBarChart chart = new StackedBarChart("Passenger kilometers traveled per Mode","Iteration","pkm",categories);
            for (String mode : allModes){
                double[] value =  pmtPerIteration.values().stream()
                        .mapToDouble(k->k.getOrDefault(mode,0.0)/1000.0)
                        .toArray();
                chart.addSeries(mode, value);
            }
            chart.addMatsimLogo();
            chart.saveAsPng(controlerIO.getOutputFilename(filename+".png"),1024,768);

        }

    }
}

