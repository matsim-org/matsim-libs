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

import java.io.*;
import java.text.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.*;

import playground.michalm.util.matrices.*;

import com.google.common.base.*;


public class TaxiStatusDataAnalyser
{
    public static Matrices calculateAveragesByHour(Matrices statusMatrices, int days)
    {
        //convert 5-minute-vehicles ==> 1-hour-vehicles 5 / 60
        //and then average over days 
        final double normalizeFactor = 5. / 60 / days;

        return calculateAverages(statusMatrices, normalizeFactor, new Function<String, String>() {
            public String apply(String from)
            {
                try {
                    return StringUtils.leftPad(STATUS_DATE_FORMAT.parse(from).getHours() + "", 2);
                }
                catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    public static Matrices calculateAverages(Matrices statusMatrices, double factor,
            Function<? super String, String> keyAggregator)
    {
        Matrices avgMatrices = MatrixUtils.aggregateMatrices(statusMatrices, keyAggregator);
        MatrixUtils.scaleMatrices(avgMatrices, factor);
        return avgMatrices;
    }


    @SuppressWarnings("deprecation")
    public static void dumpTaxisInSystem(Matrices statusMatrices, String start, String end,
            String averagesFile, String taxisOverTimeFile)
    {
        Map<String, Double> taxisInSystem = new TreeMap<String, Double>();
        Map<String, Double> averageTaxisPerHour = new TreeMap<String, Double>();

        SimpleDateFormat hrs = new SimpleDateFormat("yyyyMMddHH");

        try {
            Date currentTime = STATUS_DATE_FORMAT.parse(start);
            Date endTime = STATUS_DATE_FORMAT.parse(end);

            double hourTaxis = 0.;
            double filesPerhr = 12;

            while (!currentTime.equals(endTime)) {
                Matrix matrix = statusMatrices.getMatrix(STATUS_DATE_FORMAT.format(currentTime));
                if (matrix == null) {
                    System.err.println("id: " + STATUS_DATE_FORMAT.format(currentTime)
                            + " not found");
                    currentTime = getNextTime(currentTime);
                    filesPerhr--;
                    continue;
                }

                Iterable<Entry> entryIter = MatrixUtils.createEntryIterable(matrix);
                double totalTaxis = 0.;
                for (Entry e : entryIter) {
                    totalTaxis += e.getValue();
                    hourTaxis += e.getValue();
                }

                taxisInSystem.put(STATUS_DATE_FORMAT.format(currentTime), totalTaxis);

                if (currentTime.getMinutes() == 55) {
                    double average = hourTaxis / filesPerhr;
                    String t = hrs.format(currentTime);
                    averageTaxisPerHour.put(t, average);
                    hourTaxis = 0.;
                    filesPerhr = 12.;
                }
                currentTime = getNextTime(currentTime);

            }

            dumpMapToFile(averageTaxisPerHour, averagesFile);
            dumpMapToFile(taxisInSystem, taxisOverTimeFile);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }


    private static void dumpMapToFile(Map<String, Double> mapToWrite, String fileName)
    {
        Writer writer = IOUtils.getBufferedWriter(fileName);
        try {
            for (Map.Entry<String, Double> e : mapToWrite.entrySet()) {
                writer.write(e.getKey() + "\t" + e.getValue() + "\n");
            }
            writer.flush();
            writer.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    private static Date getNextTime(Date currentTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTime);
        cal.add(Calendar.MINUTE, 5);
        return cal.getTime();
    }


    private static void writeMatrices(Matrices matrices, String xmlFile, String txtFile)
    {
        new MatricesWriter(matrices).write(xmlFile);
        new MatricesTxtWriter(matrices).write(txtFile);
    }


    private static final SimpleDateFormat STATUS_DATE_FORMAT = new SimpleDateFormat(
            "yyyyMMddHHmmss");


    public static void main(String[] args)
    {
//        String dir = "d:/eclipse-vsp/sustainability-w-michal-and-dlr/data/OD/2014/status/";
        String dir = "c:/local_jb/data/taxi_berlin/2014/status/";
        String statusMatricesFile = dir + "statusMatrix.xml.gz";

        String averagesFile = dir + "averages.csv";
        String taxisOverTimeFile = dir + "taxisovertime.csv";

        String hourlyStatusMatricesXmlFile = dir + "statusMatrixHourly.xml";
        String hourlyStatusMatricesTxtFile = dir + "statusMatrixHourly.txt";

        String avgStatusMatricesXmlFile = dir + "statusMatrixAvg.xml";
        String avgStatusMatricesTxtFile = dir + "statusMatrixAvg.txt";

        String zonalStatuses = dir + "statusByZone.txt";

//
        Matrices statusMatrices = MatrixUtils.readMatrices(statusMatricesFile);
//
//                dumpTaxisInSystem(statusMatrices, "20130415000000", "20130421235500", averagesFile,
//                        taxisOverTimeFile);
//
        writeStatusByZone(statusMatrices, zonalStatuses);
//        Matrices hourlyMatrices = calculateAveragesByHour(statusMatrices, 7);
//        writeMatrices(hourlyMatrices, hourlyStatusMatricesXmlFile, hourlyStatusMatricesTxtFile);
//
//        writeMatrices(calculateAverages(hourlyMatrices, 1./24, Functions.constant("avg")),
//                avgStatusMatricesXmlFile, avgStatusMatricesTxtFile);
    }


    private static void writeStatusByZone(Matrices statusMatrices, String zonalStatuses)
    {   
            Map<String,IdleStatusEntry> statuses = new TreeMap<String, IdleStatusEntry>();
            for (Matrix m: statusMatrices.getMatrices().values()){
                for (ArrayList<Entry> l : m.getFromLocations().values()){
                    
                    for (Entry e : l ){
                        if (!statuses.containsKey(e.getFromLocation())){
                            statuses.put(e.getFromLocation(), new IdleStatusEntry(e.getFromLocation()));
                        }
                        IdleStatusEntry ent = statuses.get(e.getFromLocation()); 
                        if (e.getToLocation().equals("65")) ent.inc65(e.getValue());
                        if (e.getToLocation().equals("70")) ent.inc70(e.getValue());
                        if (e.getToLocation().equals("80")) ent.inc80(e.getValue());
                        if (e.getToLocation().equals("83")) ent.inc83(e.getValue());
                    }
                }
            }
        BufferedWriter bw = IOUtils.getBufferedWriter(zonalStatuses);
        try {
            bw.append("zone\t65\t70\t80\t83\tsum");
            bw.newLine();
            for (IdleStatusEntry ise : statuses.values()) {
                bw.append(ise.toString());
                bw.newLine();
            }
            bw.flush();
            bw.close();
            
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
            
    }
}
    class IdleStatusEntry{
        private String zone;
        private double status65 = 0;
        private double status70 = 0;
        private double status80 = 0;
        private double status83 = 0;
        
        public IdleStatusEntry(String zone){
            this.zone = zone;
        }
        
        public void inc65(double value){
            status65+=value;
        }
        public void inc70(double value){
            status70+=value;
        }
        public void inc80(double value){
            status80+=value;
        }
        public void inc83(double value){
            status83+=value;
        }
      
        public String toString(){
            return zone+"\t"+Math.round(status65)+"\t"+Math.round(status70)+"\t"+Math.round(status80)+"\t"+Math.round(status83)+"\t"+Math.round(status70+status80+status65+status83);
        }
    } 
