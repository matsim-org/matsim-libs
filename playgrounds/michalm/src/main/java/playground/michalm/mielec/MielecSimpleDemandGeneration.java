/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.mielec;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.contrib.zone.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.demand.*;
import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;
import playground.michalm.util.array2d.*;
import playground.michalm.util.matrices.MatrixUtils;


public class MielecSimpleDemandGeneration
{
    public static void main(String[] args)
    {
        String dir = "D:\\michalm\\2013_07\\mielec-2-peaks-new\\";
        String networkFile = dir + "network.xml";
        String zonesXmlFile = dir + "zones.xml";
        String zonesShpFile = dir + "GIS\\zones.SHP";
        String odMatrixFile = dir + "odMatrix.dat";
        String plansFile = dir + "plans.xml";

        String taxiFile = dir + "taxiCustomers_03_pc.txt";

        // double hours = 2;
        // double flowCoeff = 1;
        // double taxiProbability = 0;

        double duration = 3600;
        double[] flowCoeffs = { 0.2, 0.4, 0.6, 0.8, 0.6, 0.4, 0.2 };
        double taxiProbability = 0.03;

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        Map<Id<Zone>, Zone> zones = Zones.readZones(zonesXmlFile, zonesShpFile);

        ActivityCreator ac = new DefaultActivityCreator(scenario);
        PersonCreatorWithRandomTaxiMode pc = new PersonCreatorWithRandomTaxiMode(scenario,
                taxiProbability);
        ODDemandGenerator dg = new ODDemandGenerator(scenario, zones, true, ac, pc);

        double[][] matrix = Array2DReader.getDoubleArray(odMatrixFile, zones.size());
        Matrix afternoonODMatrix = MatrixUtils.createSparseMatrix("afternoon", zones.keySet(),
                matrix);
        double[][] transposedMatrix = Array2DUtils.transponse(matrix);
        Matrix morningODMatrix = MatrixUtils.createSparseMatrix("morning", zones.keySet(),
                transposedMatrix);

        double startTime = 6 * 3600;
        dg.generateMultiplePeriods(morningODMatrix, "dummy", "dummy", TransportMode.car, startTime,
                duration, flowCoeffs);

        startTime += 3600 * flowCoeffs.length;
        dg.generateMultiplePeriods(afternoonODMatrix, "dummy", "dummy", TransportMode.car,
                startTime, duration, flowCoeffs);

        dg.write(plansFile);
        pc.writeTaxiCustomers(taxiFile);
    }
}
