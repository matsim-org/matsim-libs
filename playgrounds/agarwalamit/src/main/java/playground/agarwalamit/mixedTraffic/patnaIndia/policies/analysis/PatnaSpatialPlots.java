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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 06/01/2017.
 */

public class PatnaSpatialPlots {

    private static final String dir = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/";
    private static final String bauDir = dir+"/bau/";
    private static final String policyName = "BT-b";
    private static final String policyDir = dir+"/"+policyName+"/";
    private static final String shapeFile = FileUtils.SHARED_SVN+"/projects/patnaIndia/inputs/raw/others/shapeFiles/patnaUrbanArea.shp";

    private static final double countScaleFactor = 1.0 / PatnaUtils.SAMPLE_SIZE;

    private static final double gridSize = 100;
    private static final double smoothingRadius = 100;

    private static final boolean isWritingGGPLOTData = true;
    private static final int noOfBins = 1;

    private static final double xMin=304200.00;
    private static final double xMax=326700.00;
    private static final double yMin=2828000.00;
    private static final double yMax=2838500.00;

    private static final boolean isComparing = false;

    private static final CoordinateReferenceSystem targetCRS = MGC.getCRS(PatnaUtils.EPSG);

    public static void main(String[] args) {
        new PatnaSpatialPlots().writeEmissionsToCell();
    }

    private void writeEmissionsToCell(){

        Map<Id<Link>,SortedMap<String,Double>> linkEmissionsBau = new HashMap<>();
        Map<Id<Link>,SortedMap<String,Double>> linkEmissionsPolicy = new HashMap<>();

        SpatialDataInputs inputs;
        if (isComparing)inputs = new SpatialDataInputs(SpatialDataInputs.LinkWeightMethod.line, bauDir, policyDir);
        else inputs = new SpatialDataInputs(SpatialDataInputs.LinkWeightMethod.line, bauDir);

        inputs.setBoundingBox(xMin, xMax, yMin, yMax);
        inputs.setTargetCRS(targetCRS);
        inputs.setGridInfo(GeneralGrid.GridType.HEX,gridSize);
        inputs.setSmoothingRadius(smoothingRadius);
        inputs.setShapeFile(shapeFile);

        SpatialInterpolation plot = new SpatialInterpolation(inputs, dir+"/analysis/spatialPlots/");
//        SpatialInterpolation plot = new SpatialInterpolation(inputs, dir+"/analysis/spatialPlots/", true);

        double simeEndTime = LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig);
        {
            EmissionLinkAnalyzer emissionLinkAnalyzerBAU = new EmissionLinkAnalyzer(simeEndTime, inputs.initialCaseEmissionEventsFile, noOfBins);
            emissionLinkAnalyzerBAU.preProcessData();
            emissionLinkAnalyzerBAU.postProcessData();
            linkEmissionsBau = emissionLinkAnalyzerBAU.getLink2TotalEmissions().entrySet().iterator().next().getValue();
        }

        if(inputs.isComparing) {
            EmissionLinkAnalyzer emissionLinkAnalyzerPolicy = new EmissionLinkAnalyzer(simeEndTime, inputs.compareToCaseEmissionEventsFile, noOfBins);
            emissionLinkAnalyzerPolicy.preProcessData();
            emissionLinkAnalyzerPolicy.postProcessData();
            linkEmissionsPolicy = emissionLinkAnalyzerPolicy.getLink2TotalEmissions().entrySet().iterator().next().getValue();
        }

        Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(policyDir+"/output_network.xml.gz");

        EmissionTimebinDataWriter writer = new EmissionTimebinDataWriter();
        if(isComparing) writer.openWriter(dir+"/analysis/spatialPlots/"+"viaData_NO2_"+ GeneralGrid.GridType.HEX+"_"+gridSize+"_"+smoothingRadius+"_line_"+policyName+"_diff.txt");
        else writer.openWriter(dir+"/analysis/spatialPlots/"+"viaData_NO2_"+ GeneralGrid.GridType.HEX+"_"+gridSize+"_"+smoothingRadius+"_line_"+"_bau.txt");

        for(Link l : sc.getNetwork().getLinks().values()){
            Id<Link> id = l.getId();

            if(plot.isInResearchArea(l)){
                double emiss = 0;
                if(inputs.isComparing){
                    double linkEmissionBau =0;
                    double linkEmissionPolicy =0;

                    if(linkEmissionsBau.containsKey(id) && linkEmissionsPolicy.containsKey(id)) {
                        linkEmissionBau = countScaleFactor * linkEmissionsBau.get(id).get(WarmPollutant.NO2.toString());
                        linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(id).get(WarmPollutant.NO2.toString());
                    } else if(linkEmissionsBau.containsKey(id)){
                        linkEmissionBau = countScaleFactor * linkEmissionsBau.get(id).get(WarmPollutant.NO2.toString());
                    } else if(linkEmissionsPolicy.containsKey(id)){
                        linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(id).get(WarmPollutant.NO2.toString());
                    }
                    emiss = linkEmissionPolicy - linkEmissionBau;

                } else {
                    if(linkEmissionsBau.containsKey(id)) emiss = countScaleFactor * linkEmissionsBau.get(id).get(WarmPollutant.NO2.toString());
                    else emiss =0;
                }

                plot.processLink(l,  emiss);
            }
        }
        writer.writeData(simeEndTime, plot.getCellWeights());
        plot.writeRData("NO2_", isWritingGGPLOTData);
        plot.reset();
        writer.closeWriter();
    }

    private class EmissionTimebinDataWriter{
        BufferedWriter writer;
        public void openWriter (final String outputFile){
            writer = IOUtils.getBufferedWriter(outputFile);
            try {
                writer.write("timebin\t centroidX \t centroidY \t weight \n");
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason "+e);
            }
        }

        public void writeData(final double timebin, final Map<Point,Double> cellWeights){
            try {
                for(Point p : cellWeights.keySet()){
                    writer.write(timebin+"\t"+p.getCentroid().getX()+"\t"+p.getCentroid().getY()+"\t"+cellWeights.get(p)+"\n");
                }
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason "+e);
            }
        }

        public void closeWriter (){
            try {
                writer.close();
            } catch (Exception e) {
                throw new RuntimeException("Data is not written to file. Reason "+e);
            }
        }
    }
}
