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

package playground.southafrica.projects.complexNetworks.tmp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class ExtractAllStudyAreaActivities {

	public static void main(String[] args) {
		Header.printHeader(ExtractAllStudyAreaActivities.class.toString(), args);
		
		String inputFolder = args[0];
		String outputFile = args[1];
		Double HEX_WIDTH = Double.parseDouble(args[2]); 
		
		QuadTree<Coord> qt = buildQuadTree();
		
		/* Get all the files */
		List<File> listOfFiles = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		
		Counter counter = new Counter("   vehicles # ");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("Long,Lat");
			bw.newLine();
			
			for(File file : listOfFiles){
				/* Parse the vehicle from file. */
				DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
				dvr.parse(file.getAbsolutePath());
				DigicoreVehicle vehicle = dvr.getVehicle();
				
				/* Check how far EACH activity is. If it is within the threshold,
				 * then write it to file. */
				for(DigicoreChain chain : vehicle.getChains()){
					for(DigicoreActivity act : chain.getAllActivities()){
						Coord actCoord = act.getCoord();
						Coord closest = qt.get(actCoord.getX(), actCoord.getY());
						double dist = CoordUtils.calcDistance(actCoord, closest);
						if(dist <= HEX_WIDTH){
							bw.write(String.format("%.2f, %.2f\n", actCoord.getX(), actCoord.getY()));
						}
					}
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		counter.printCounter();
		
		Header.printFooter();
	}


	/**
	 * Builds a (artificial) QuadTree for the 10 study areas in Nelson Mandela
	 * Bay Metropole.
	 * 
	 * @return
	 */
	private static QuadTree<Coord> buildQuadTree(){
		QuadTree<Coord> qt = new QuadTree<Coord>(130000.0, -3707000.0, 152000.0, -3684000.0);

		qt.put(130048.2549,-3685018.8482, new CoordImpl(130048.2549,-3685018.8482));
		qt.put(148048.2549,-3702339.3562, new CoordImpl(148048.2549,-3702339.3562));
		qt.put(148798.2549,-3704504.4197, new CoordImpl(148798.2549,-3704504.4197));
		qt.put(149548.2549,-3706669.4833, new CoordImpl(149548.2549,-3706669.4833));
		qt.put(151048.2549,-3706669.4833, new CoordImpl(151048.2549,-3706669.4833));
		qt.put(148048.2549,-3701473.3308, new CoordImpl(148048.2549,-3701473.3308));
		qt.put(146548.2549,-3697143.2038, new CoordImpl(146548.2549,-3697143.2038));
		qt.put(146548.2549,-3704937.4325, new CoordImpl(146548.2549,-3704937.4325));
		qt.put(148048.2549,-3705803.4579, new CoordImpl(148048.2549,-3705803.4579));
		qt.put(130048.2549,-3684152.8228, new CoordImpl(130048.2549,-3684152.8228));

		return qt;
	}

}
