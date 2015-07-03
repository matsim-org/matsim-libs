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

package playground.jbischoff.taxi.berlin.data;

import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.util.matrices.MatrixUtils;

public class ZoneExtractor
{

    Matrices matrices = MatrixUtils.readMatrices("C:/local_jb/data/taxi_berlin/2013/OD/demandMatrices.xml");
    Matrices outmat = new Matrices();
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    /**
     * @param input matrix file
     * exports values from or to one zone at different times
     * @throws ParseException 
     */
    public static void main(String[] args) throws ParseException
    {
        ZoneExtractor ze = new ZoneExtractor();
        Date start = sdf.parse("20130415000000");
        Date end = sdf.parse("20130421230000");
        ze.extract(start,end);
        ze.writeMatrices("C:/local_jb/data/taxi_berlin/2013/OD/");
    }
    private void writeMatrices(String dirname)
    {
        for (Matrix m : outmat.getMatrices().values()){
            exportMatrix(m, dirname+m.getId()+".txt");
        }
    }
    private void extract(Date start, Date end)
    {
        Matrix fromTXL = outmat.createMatrix("fromtxl", "from tegel");
        Matrix toTXL = outmat.createMatrix("totxl", "to tegel");
        Matrix fromSXF = outmat.createMatrix("fromsxf", "from schoenefeld");
        Matrix toSXF = outmat.createMatrix("tosxf", "to schoenefeld");
        
        Date currentHour = start;
        do {
            
            Matrix currentMatrix = matrices.getMatrix(sdf.format(currentHour));
            try {
            for (Entry e : currentMatrix.getFromLocEntries(BerlinZoneUtils.TXL_LOR_ID.toString())){
                fromTXL.createEntry(e.getToLocation(), sdf.format(currentHour), e.getValue());
            }
            } catch (NullPointerException e) {}
            try {
            for (Entry e : currentMatrix.getToLocEntries(BerlinZoneUtils.TXL_LOR_ID.toString())){
                toTXL.createEntry(e.getFromLocation(), sdf.format(currentHour), e.getValue());
            }
            } catch (NullPointerException e) {}
            try {
            
            for (Entry e : currentMatrix.getFromLocEntries(BerlinZoneUtils.SXF_LOR_ID.toString())){
                fromSXF.createEntry(e.getToLocation(), sdf.format(currentHour), e.getValue());
            }
            } catch (NullPointerException e) {}
            try {
        
            for (Entry e : currentMatrix.getToLocEntries(BerlinZoneUtils.SXF_LOR_ID.toString())){
                toSXF.createEntry(e.getFromLocation(), sdf.format(currentHour), e.getValue());
            } 
            } catch (NullPointerException e) {}
            
            
            currentHour = getNextTime(currentHour);
        }
        while (currentHour.before(end));
        
        
    }
    
    private Date getNextTime(Date currentTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTime);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        return cal.getTime();
    }
    
    private void exportMatrix(Matrix matrix, String fileName){
        Writer writer = IOUtils.getBufferedWriter(fileName);
        try {
            writer.append("loc");
            for (String time : matrix.getToLocations().keySet()){
                writer.append("\t"+time.toString());
            }
            writer.append("\n");
        
        for (String locId : matrix.getFromLocations().keySet()){
            Map<String,Double> currentLoc= new TreeMap<String,Double>();
            
            for (String time : matrix.getToLocations().keySet()){
                double value = 0;
                Entry e = matrix.getEntry(locId, time);
                if (e != null) value = e.getValue();
                currentLoc.put(time, value);
            }
                writer.append(locId.toString());
                for (Double d : currentLoc.values()){
                    writer.append("\t"+d);
                }
                writer.append("\n");
            
        }
        writer.flush();
        writer.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}
