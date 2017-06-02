/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.FileUtils;

/**
 * A class to extract the currentBestDevisionVariable and currentBestObjectiveFunction.
 *
 * Created by amit on 30.05.17.
 */

public class OpdytsConvergencePlotter {

    private static final Logger LOGGER = Logger.getLogger(OpdytsConvergencePlotter.class);

    private final String rawVale = "Raw Objective Function Value";
    private final String avgValue = "Averaged Objective Function Value";

    private final List<Tuple<Double, Double>> rawValueList = new ArrayList<>();
    private final List<Tuple<Double, Double>> avgValueList = new ArrayList<>();

    public static void main(String[] args) {

        String filesDir = FileUtils.RUNS_SVN+"/opdyts/equil/car,bicycle/parametrizedRuns/avgIts_STwt/";
        Integer avgItrsCases [] = {20,40,80,100,150,200};
        Double selfTuningWeight [] = {1.,2.,4.,6.,8.};

        for(Integer avgItr : avgItrsCases) {
            for (Double selfTuningWt : selfTuningWeight) {
                String caseFileDir = filesDir+ "calibration_"+avgItr+"Its_"+selfTuningWt+"weight_0.0asc/";

                OpdytsConvergencePlotter opdytsLogReader = new OpdytsConvergencePlotter();
                opdytsLogReader.readFile(caseFileDir+"/opdyts.con");
                opdytsLogReader.plotData(caseFileDir+"/convergence_"+avgItr+"Its_"+selfTuningWt+"weight.png");
            }
        }
    }

    public void readFile(final String inputFile){
        LOGGER.info("Reading file ... "+ inputFile);
        List<String> labels = null ;
        double iterationNr = 0.;
        try(BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
            String line = reader.readLine();
            boolean isHeaderLine = true;

            while(line!= null) {
                if (isHeaderLine) {
                    String parts [] = line.split("\t");
                    labels = Arrays.asList(parts);
                    isHeaderLine = false;
                } else {
                    String parts [] = line.split("\t");

                    {
                        String avgValue = parts[labels.indexOf(this.avgValue)];
                        if (! avgValue.equals("")) avgValueList.add(new Tuple<>(iterationNr, Double.valueOf(avgValue)));
                    }
                    {
                        String rawValue = parts[labels.indexOf(this.rawVale)];
                        rawValueList.add(new Tuple<>(iterationNr,Double.valueOf(rawValue)));
                    }
                    iterationNr++;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read/written. Reason "+e);
        }
    }

    public void plotData(final String outFile){
        LOGGER.info("Plotting file "+outFile);
        XYScatterChart chart = new XYScatterChart(rawVale+" & "+avgValue,"Iteration","value of objection function");
        {
            double[] xs = new double[ avgValueList.size()];
            double[] ys = new double[ avgValueList.size()];

            for(int index = 0; index < avgValueList.size(); index++ ) {
                xs[index] = avgValueList.get(index).getFirst();
                ys[index] = avgValueList.get(index).getSecond();
            }
            chart.addSeries(avgValue,xs,ys);
        }
        {
            double[] xs = new double[ rawValueList.size()];
            double[] ys = new double[ rawValueList.size()];

            for(int index = 0; index < rawValueList.size(); index++ ) {
                xs[index] = rawValueList.get(index).getFirst();
                ys[index] = rawValueList.get(index).getSecond();
            }
            chart.addSeries(rawVale,xs,ys);
        }
        chart.saveAsPng(outFile,1200,800);
    }
}





