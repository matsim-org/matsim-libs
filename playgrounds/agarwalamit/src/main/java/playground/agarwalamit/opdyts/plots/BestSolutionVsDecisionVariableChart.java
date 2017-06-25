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

package playground.agarwalamit.opdyts.plots;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
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

public class BestSolutionVsDecisionVariableChart {

    private static final Logger LOGGER = Logger.getLogger(BestSolutionVsDecisionVariableChart.class);

    public BestSolutionVsDecisionVariableChart(final Collection<String> modesToGetASC) {
        this.modesToGetASC = modesToGetASC;

        for(String mode : this.modesToGetASC){
            currentBestDecisionVariables.put(mode, new ArrayList<>());
        }
    }

    private final String bestOverallDecisionVariable = "Best Overall Decision Variable";
    private final String bestOverallSolution = "Best Overall Solution";
    private final Collection<String> modesToGetASC;

    private final List<Tuple<Double, Double>> currentBestSolutions = new ArrayList<>();
    private final Map<String, List<Tuple<Double, Double>>> currentBestDecisionVariables = new HashMap<>();


    //BEGIN_EXAMPLE
    public static void main(String[] args) {

        String filesDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_allModes/calibration_variationSize0.1_AvgIts20/";

        BestSolutionVsDecisionVariableChart opdytsLogReader = new BestSolutionVsDecisionVariableChart(Arrays.asList("car","bike","motorbike","pt","walk"));
        opdytsLogReader.readFile(filesDir+"/opdyts.log");
        opdytsLogReader.plotData(filesDir+"/decisionVariableVsASC.png");
    }
    //END_EXAMPLE

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
                        for(String mode : this.modesToGetASC) {
                            String modeDecisionVariable = decisionVariable.substring(decisionVariable.lastIndexOf(
                                    convertToModeLookUp(mode))+ convertToModeLookUp(mode).length()+1);
                            String modeASC = modeDecisionVariable.substring(0,modeDecisionVariable.indexOf("+"));
                            double currentBestDecisionVarible = Double.valueOf(modeASC);
                            List<Tuple<Double, Double>> storedData = this.currentBestDecisionVariables.get(mode);
                            storedData.add(new Tuple<>(iterationNr,currentBestDecisionVarible));
                        }
                    }
                    iterationNr++;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not read/written. Reason "+e);
        }
    }

    private String convertToModeLookUp(final String mode ){
        return ";"+mode+":";
    }

    /*
     * It will create multiple series corresponding to each mode in one plot.
     */
    public void plotData(final String outFile){
        LOGGER.info("Plotting file "+outFile);
        XYScatterChart chart = new XYScatterChart( bestOverallSolution + " & "+bestOverallDecisionVariable,"Iteration","value of objection function / asc ");
        for (String mode : this.modesToGetASC) {
            double[] xs = new double[ currentBestDecisionVariables.get(mode).size()];
            double[] ys = new double[ currentBestDecisionVariables.get(mode).size()];

            for(int index =0; index < currentBestDecisionVariables.get(mode).size(); index++ ) {
                xs[index] = currentBestDecisionVariables.get(mode).get(index).getFirst();
                ys[index] = currentBestDecisionVariables.get(mode).get(index).getSecond();
            }
            chart.addSeries(bestOverallDecisionVariable+"_"+mode, xs, ys);
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





