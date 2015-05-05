/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
/**
 * 
 */
package playground.southafrica.freight.digicore.tmp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * @author jwjoubert
 *
 */
public class ExtractVehiclesForSollyFrans {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ExtractVehiclesForSollyFrans.class.toString(), args);
		
		String path = "/home/jwjoubert/Documents/data/Digicore/ByMonth/201403/SollyFrans/xml/";
		String output = "/home/jwjoubert/Documents/data/Digicore/ByMonth/201403/SollyFrans/chains.csv";
		List<File> list = FileUtils.sampleFiles(new File(path), 100, FileUtils.getFileFilter(".xml.gz"));
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("Vehicle,Chain,Activity,X,Y,Long,Lat,Start,End");
			bw.newLine();
			
			for(File f : list){
				DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
				dvr.parse(f.getAbsolutePath());
				DigicoreVehicle v = dvr.getVehicle();
				bw.write(v.getId().toString());
				bw.write(",");
				
				int chain = 1;
				for(DigicoreChain c : v.getChains()){
					int act = 1;
					for(DigicoreActivity a : c.getAllActivities()){
						Coord coord = a.getCoord();
						Coord coordWgs84 = ct.transform(coord);
						
						String s = String.format("%s,%d,%d,%.0f,%.0f,%.6f,%.6f,%s,%s\n",
								v.getId().toString(),
								chain,
								act,
								coord.getX(),
								coord.getY(),
								coordWgs84.getX(),
								coordWgs84.getY(),
								getNiceDate(a.getStartTimeGregorianCalendar()),
								getNiceDate(a.getEndTimeGregorianCalendar()));
						bw.write(s);
						act++;
					}
					chain++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		Header.printFooter();
	}
	
	private static String getNiceDate(GregorianCalendar cal){
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		
		return String.format("%d/%02d/%02d %02d:%02d", year, month, day, hour, min);
	}

}
