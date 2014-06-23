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

package playground.jbischoff.taxi.berlin.supply;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandGenerator;
import playground.michalm.zone.util.MatrixUtils;

public class TaxiStatusDataAnalyser
{

        private Scenario scenario;
        private Matrices matrices;
        private Matrices relativeMatrices;
        private static final String NETWORKFILE = "C:/local_jb/data/scenarios/2014_05_basic_scenario_v3/berlin_brb.xml";
        private static final String STATUSMATRIX = "C:/local_jb/data/OD/status/statusMatrix.xml.gz";
        private static final String AVERAGES = "C:/local_jb/data/OD/status/averages.csv";
        private static final String TAXISOVERTIME = "C:/local_jb/data/OD/status/taxisovertime.csv";
        private static final String AVERAGESMATRIX = "C:/local_jb/data/OD/status/statusMatrixAv.xml";
        
        Map<String,Double> taxisInSystem;
        Map<String,Double> averageTaxisPerHour;
        /**
     * @param args
         * @throws ParseException 
     */
    public static void main(String[] args) 
    {
        TaxiStatusDataAnalyser ast = new TaxiStatusDataAnalyser();
//        ast.dumpTaxisInSystem("20140407000000", "20140413235500");
        ast.calculateAverages();
        
    }
    public TaxiStatusDataAnalyser()
    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
        this.matrices = new Matrices();
        new MatsimMatricesReader(matrices, (ScenarioImpl)scenario).readFile(STATUSMATRIX);
        this.taxisInSystem = new TreeMap<String, Double>();
        this.averageTaxisPerHour= new TreeMap<String, Double>();
        this.relativeMatrices = new Matrices();
       
    }
    
    private void calculateAverages(){
     
        
        Matrices avAllTime = new Matrices();
        Matrix content = avAllTime.createMatrix("ave", "all time status values");
        Set<Id> allStatuses = new HashSet<Id>();
        Set<Id> allLOR = new HashSet<Id>();
        for (Matrix matrix : this.matrices.getMatrices().values()){
            allStatuses.addAll(matrix.getToLocations().keySet());
            allLOR.addAll(matrix.getFromLocations().keySet());
        }
        System.out.println(allStatuses.size());
        
        System.out.println(allLOR.size());
        for (Id statusId : allStatuses){
            for (Id lorId : allLOR){
                double totalStatusLor = 0.;
                double validEntries = 0.;
                for (Matrix matrix : this.matrices.getMatrices().values()){
                    Entry e =  matrix.getEntry(lorId, statusId);
                    if (e == null) continue;
                    double v =e.getValue();
                    if (v != 0){
                        validEntries++;
                        totalStatusLor += v;
                    }
              
                }
                double average = totalStatusLor/validEntries;
                if (Double.isNaN(average)) average = 0.;
                content.createEntry(lorId, statusId, average);
            }
        }
        new MatricesWriter(avAllTime).write(AVERAGESMATRIX);

    }
    
    @SuppressWarnings("deprecation")
  private void dumpTaxisInSystem(String start, String end){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat hrs = new SimpleDateFormat("yyyyMMddHH");

        try {
            Date currentTime = sdf.parse(start);
            Date endTime = sdf.parse(end);
            double hourTaxis = 0.;
            double filesPerhr = 12;
            while (!(currentTime.toString().equals(endTime.toString())))   
            {
            Matrix matrix = this.matrices.getMatrix(sdf.format(currentTime));
            if (matrix == null) {
                System.err.println("id: "+sdf.format(currentTime) + " not found");
                currentTime = getNextTime(currentTime);
                filesPerhr --;
                continue;
            };
            Iterable<Entry> entryIter = MatrixUtils.createEntryIterable(matrix);
            double totalTaxis = 0.;
            for (Entry e : entryIter) {
                
                totalTaxis += e.getValue();
                hourTaxis += e.getValue(); 
            }
            this.taxisInSystem.put(sdf.format(currentTime),totalTaxis);
            if (currentTime.getMinutes() == 55){
                double average = hourTaxis / filesPerhr;
                String t = hrs.format(currentTime);
                this.averageTaxisPerHour.put(t, average);
                hourTaxis = 0.;
                filesPerhr = 12.;
            }
            currentTime = getNextTime(currentTime);
            
            }
            
        this.dumpMapToFile(this.averageTaxisPerHour, AVERAGES);
        this.dumpMapToFile(taxisInSystem, TAXISOVERTIME);
        }
        catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        

    
    }
    private void dumpMapToFile(Map<String,Double> mapToWrite, String fileName){
        Writer writer = IOUtils.getBufferedWriter(fileName);
        try {
        for (java.util.Map.Entry<String,Double> e : mapToWrite.entrySet()) {
            writer.write(e.getKey() + "\t"+e.getValue()+"\n");
            
        }
            writer.flush();
            writer.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    private Date getNextTime(Date currentTime){
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTime);
        cal.add(Calendar.MINUTE, 5);
        return cal.getTime();
    }
    
    
}
