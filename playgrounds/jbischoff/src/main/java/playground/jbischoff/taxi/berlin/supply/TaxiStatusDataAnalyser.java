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
        new MatricesTxtWriter(matrices.getMatrices()).write(txtFile);
    }


    private static final SimpleDateFormat STATUS_DATE_FORMAT = new SimpleDateFormat(
            "yyyyMMddHHmmss");


    public static void main(String[] args) throws ParseException, IOException
    {
//        String dir = "c:/local_jb/data/taxi_berlin/2014/status/";
    	
//        String dir = "/Users/jb/sustainability-w-michal-and-dlr/data/taxi_berlin/2014/status/";
        //String dir = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/taxi_berlin/2014/status/";
        String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/taxi_berlin/2013/status/";
        String statusMatricesFile = dir + "statusMatrix.xml.gz";

        String averagesFile = dir + "averages.csv";
        String taxisOverTimeFile = dir + "taxisovertime.csv";

        String hourlyStatusMatricesXmlFile = dir + "statusMatrixHourly.xml";
        String hourlyStatusMatricesTxtFile = dir + "statusMatrixHourly.txt";

        String avgStatusMatricesXmlFile = dir + "statusMatrixAvg.xml";
        String avgStatusMatricesTxtFile = dir + "statusMatrixAvg.txt";

        String zonalStatuses = dir + "statusByZone_tuesday.txt";
        
        String idleVehiclesPerZoneAndHour = dir + "idleVehiclesPerZoneAndHour.txt";

//
        Matrices statusMatrices = MatrixUtils.readMatrices(statusMatricesFile);

//      dumpTaxisInSystem(statusMatrices, "20130415000000", "20130421235500", averagesFile, taxisOverTimeFile);

        
        //2013
        String start =  "20130415000000";
        String end =    "20130422000000";
        
        //2014
//        String start = 	"20140407000000";
//        String end = 	"20140414000000";
        
        writeStatusByZone(statusMatrices, zonalStatuses, start, end);
        Matrices hourlyMatrices = calculateAveragesByHour(statusMatrices, 7);
        writeMatrices(hourlyMatrices, hourlyStatusMatricesXmlFile, hourlyStatusMatricesTxtFile);

        writeMatrices(calculateAverages(hourlyMatrices, 1./24, Functions.constant("avg")),
                avgStatusMatricesXmlFile, avgStatusMatricesTxtFile);
        writeIdleVehiclesByZoneAndStatus(statusMatrices, idleVehiclesPerZoneAndHour, start, end);
    }

    private static void writeIdleVehiclesByZoneAndStatus(Matrices statusMatrices, String idleVehicles, String start, String end) throws ParseException, IOException
    {

        Set<String> allZones = new HashSet<String>();
        for (Matrix matrix : statusMatrices.getMatrices().values()){
        	allZones.addAll(matrix.getFromLocations().keySet());
        }
        
        BufferedWriter writer = IOUtils.getBufferedWriter(idleVehicles);
        Date currentTime = STATUS_DATE_FORMAT.parse(start);
        Date endTime = STATUS_DATE_FORMAT.parse(end);
       
        //header
        writer.append("zone");
        while (!currentTime.equals(endTime)) {
        	
        	if (currentTime.getMinutes() == 00){
            	writer.append("\t"+STATUS_DATE_FORMAT.format(currentTime));
            	
            	}
        	currentTime = getNextTime(currentTime);

        }
        
        for (String zone : allZones){
            currentTime = STATUS_DATE_FORMAT.parse(start);
            endTime = STATUS_DATE_FORMAT.parse(end);
            double hourlyVehicles = 0;
            writer.newLine();
            writer.append(zone);
            while (!currentTime.equals(endTime)) {
            	
            	Matrix  m = statusMatrices.getMatrix(STATUS_DATE_FORMAT.format(currentTime));   
            	if (m!=null){
            	if (m.getFromLocations().containsKey(zone))
            	{
            	for (Entry entry : m.getFromLocEntries(zone)){
            		switch (entry.getToLocation()){
            		case "65": 
            		case "70" : 
            		case "80" : 
            		case "83" : 
            		case "85":
            		{
            			hourlyVehicles+=entry.getValue();
            			break;
            		} 
            		default:
            			break;
            		
            	}
            	}
            	}
            	}
            	if (zone.equals("1011401")){
            		System.out.println(STATUS_DATE_FORMAT.format(currentTime)+"\t"+hourlyVehicles);
            	}
            	if (currentTime.getMinutes() == 55){
            	writer.append("\t"+hourlyVehicles/12.0);
            	hourlyVehicles = 0;	

            	}
            
            	currentTime = getNextTime(currentTime);
            }
        
        }
        writer.flush();
        writer.close();
        }
   
    
    private static void writeStatusByZone(Matrices statusMatrices, String zonalStatuses, String start, String end) throws ParseException
    {   
        Date currentTime = STATUS_DATE_FORMAT.parse(start);
        Date endTime = STATUS_DATE_FORMAT.parse(end);
        Map<String,IdleStatusEntry> statuses = new TreeMap<String, IdleStatusEntry>();
            
        while (!currentTime.equals(endTime)) {
        	Matrix  m = statusMatrices.getMatrix(STATUS_DATE_FORMAT.format(currentTime));   
        	System.out.println(STATUS_DATE_FORMAT.format(currentTime));
            if (m == null)
            {
            	System.err.println("time comes without status"+STATUS_DATE_FORMAT.format(currentTime));
                currentTime = getNextTime(currentTime);

            	continue;
            }
        	for (ArrayList<Entry> l : m.getFromLocations().values()){
                    
                    for (Entry e : l ){
                        if (!statuses.containsKey(e.getFromLocation())){
                            statuses.put(e.getFromLocation(), new IdleStatusEntry(e.getFromLocation()));
                        }
                        IdleStatusEntry ent = statuses.get(e.getFromLocation()); 
                        if (e.getToLocation().equals("65")) ent.inc65(e.getValue());
                        if (e.getToLocation().equals("66")) ent.inc66(e.getValue());
                        if (e.getToLocation().equals("70")) ent.inc70(e.getValue());
                        if (e.getToLocation().equals("75")) ent.inc75(e.getValue());
                        if (e.getToLocation().equals("79")) ent.inc79(e.getValue());
                        if (e.getToLocation().equals("80")) ent.inc80(e.getValue());
                        if (e.getToLocation().equals("83")) ent.inc83(e.getValue());
                        if (e.getToLocation().equals("85")) ent.inc85(e.getValue());
                        if (e.getToLocation().equals("87")) ent.inc87(e.getValue());
                        if (e.getToLocation().equals("90")) ent.inc90(e.getValue());
                    }
                }
                currentTime = getNextTime(currentTime);
            }
        BufferedWriter bw = IOUtils.getBufferedWriter(zonalStatuses);
        try {
            bw.append("zone\ts65\ts66\ts70\ts75\ts79\tss80\ts83\ts85\ts87\ts90\tsum (wait)");
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
        private double status66 = 0;
        
        private double status70 = 0;
        private double status75 = 0;
        private double status79 = 0;
        private double status80 = 0;
        private double status83 = 0;
        private double status85 = 0;
        private double status87 = 0;
        private double status90 = 0;
        
        public IdleStatusEntry(String zone){
            this.zone = zone;
        }
        
        public void inc65(double value){
            status65+=value;
        }
        public void inc66(double value){
            status66+=value;
        }
        public void inc70(double value){
            status70+=value;
        }
        public void inc75(double value){
        	status75+=value;
        }
        public void inc79(double value){
        	status79+=value;
        }
        public void inc80(double value){
            status80+=value;
        }
        public void inc83(double value){
            status83+=value;
        }
        public void inc85(double value){
        	status85+=value;
        }
        public void inc87(double value){
        	status87+=value;
        }
        public void inc90(double value){
        	status90+=value;
        }
        
      
        public String toString(){
            return zone+"\t"+Math.round(status65)+"\t"+Math.round(status66)+"\t"+Math.round(status70)+"\t"+Math.round(status75)+"\t"+Math.round(status79)+"\t"+Math.round(status80)+"\t"+Math.round(status83)+"\t"+Math.round(status85)+"\t"+Math.round(status87)+"\t"+Math.round(status90)+"\t"+Math.round(status70+status80+status65+status83);
        }
    } 
