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

public class DecisionVariableAndBestSolutionPlotter {

    private static final Logger LOGGER = Logger.getLogger(DecisionVariableAndBestSolutionPlotter.class);

    public DecisionVariableAndBestSolutionPlotter(final String modeToGetASC) {
        //TODO for patna, probably explode it to list and then use as many lists to plot ASC for each mode
        this.modeToGetASC = ";"+modeToGetASC+":";
    }

    private final String bestOverallDecisionVariable = "Best Overall Decision Variable";
    private final String bestOverallSolution = "Best Overall Solution";
    private final String modeToGetASC ;

    private final List<Tuple<Double, Double>> currentBestSolutions = new ArrayList<>();
    private final List<Tuple<Double, Double>> currentBestDecisionVariables = new ArrayList<>();

    public static void main(String[] args) {

        String filesDir = FileUtils.RUNS_SVN+"/opdyts/equil/car,bicycle/parametrizedRuns/avgIts_STwt/";
        Integer avgItrsCases [] = {20,40,80,100,150,200};
        Double selfTuningWeight [] = {1.,2.,4.,6.,8.};

        for(Integer avgItr : avgItrsCases) {
            for (Double selfTuningWt : selfTuningWeight) {
                String caseFileDir = filesDir+ "calibration_"+avgItr+"Its_"+selfTuningWt+"weight_0.0asc/";

                DecisionVariableAndBestSolutionPlotter opdytsLogReader = new DecisionVariableAndBestSolutionPlotter("bicycle");
                opdytsLogReader.readFile(caseFileDir+"/opdyts.log");
                opdytsLogReader.plotData(caseFileDir+"/decisionVariableVsASC_"+avgItr+"Its_"+selfTuningWt+"weight.png");
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

                    { // best solution
                        String bestSoluString = parts[labels.indexOf(bestOverallSolution)];
                        if (! bestSoluString.equals("")) currentBestSolutions.add(new Tuple<>(iterationNr, Double.valueOf(bestSoluString)));
                    }
                    {
                        int indexDecisionVariable = labels.indexOf(bestOverallDecisionVariable);
                        String decisionVariable = parts[indexDecisionVariable];
                        String bicycleDecisionVariable = decisionVariable.substring(decisionVariable.lastIndexOf(modeToGetASC)+modeToGetASC.length()+1);
                        String bicycleASC = bicycleDecisionVariable.substring(0,bicycleDecisionVariable.indexOf("+"));
                        double currentBestDecisionVarible = Double.valueOf(bicycleASC);
                        currentBestDecisionVariables.add(new Tuple<>(iterationNr,currentBestDecisionVarible));
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
        XYScatterChart chart = new XYScatterChart( bestOverallSolution+ " & "+bestOverallDecisionVariable,"Iteration","value of objection function / asc ");
        {
            double[] xs = new double[ currentBestDecisionVariables.size()];
            double[] ys = new double[ currentBestDecisionVariables.size()];

            for(int index =0; index < currentBestDecisionVariables.size(); index++ ) {
                xs[index] = currentBestDecisionVariables.get(index).getFirst();
                ys[index] = currentBestDecisionVariables.get(index).getSecond();
            }
            chart.addSeries(bestOverallDecisionVariable,xs,ys);
        }
        {
            double[] xs = new double[ currentBestSolutions.size()];
            double[] ys = new double[ currentBestSolutions.size()];

            for(int index =0; index < currentBestSolutions.size(); index++ ) {
                xs[index] = currentBestSolutions.get(index).getFirst();
                ys[index] = currentBestSolutions.get(index).getSecond();
            }
            chart.addSeries(bestOverallSolution, xs, ys);
        }
        chart.saveAsPng(outFile,1200,800);
    }
}





