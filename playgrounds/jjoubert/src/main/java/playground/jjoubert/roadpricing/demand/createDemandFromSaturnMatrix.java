/* *********************************************************************** *
 * project: org.matsim.*
 * createDemandFromSaturnMatrix.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.demand;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.XY2Links;

import playground.jjoubert.Utilities.roadpricing.MyDemandMatrix;

public class createDemandFromSaturnMatrix {

	/**
	 * Class to generate plans files from the Saturn OD-matrices provided 
	 * by Sanral's modelling consultant, Alan Robinson.
	 * @param args containing the following elements, and in this order:
	 * <ol>
	 * 	<li> <b>matrixFilename</b> absolute path of the <code>csv</code>
	 * 		file containing the origin destination data, given without column
	 * 		headers, the first column indicating the location {@link Id} of the
	 * 		zone.
	 * 	<li> <b>coordinateFilename</b> absolute path of the file indicating the
	 * 		coordinates of each zone. The format of the file should be:
	 * 		<P><P><code>
	 * 		Long,Lat,Id<br>
	 * 		83245,23454,1<br>
	 * 		86574,33234,2<br>
	 * 		...
	 * 		</code></P>
	 * 	<li> <b>externalZonesFilename</b> the absolute path of the file that
	 * 		contains all zone {@link Id}s for which demand should be generated.
	 * 		These zones will be covered as both origin and destination zones.
	 * 		The file should contain a single {@link Id} per line without a
	 * 		column header in the first row.
	 * 	<li> <b>networkFilename</b> the absolute path of the {@link Network}
	 * 		<code>xml.gz</code> file that should be used to assign activities
	 * 		to when using the {@link XY2Links} class.
	 * 	<li> <b>plansFilename</b> the absolute path of the {@link Population} 
	 * 		(plans) file that results from this demand generation.
	 * 	<li> <b>populationFraction</b> a value indicating what proportion of 
	 * 		the actual OD-matrix demand must be generated. For example, <code>
	 * 		"0.1"</code> will indicate that a 10% sample must be generated, while
	 * 		a value of <code>"1.0"</code> indicates that a complete population,
	 * 		i.e. a 100% sample, must be generated.
	 * </ol> 		
	 */
	public static void main(String[] args) {
		String matrixFilename = null;
		String coordinateFilename = null;
		String externalZonesFilename = null;
		String networkFilename = null;
		String plansFilename = null;
		Double populationFraction = null;
		if(args.length != 6){
			throw new IllegalArgumentException("Wrong number of arguments");
		} else{
			matrixFilename = args[0];
			coordinateFilename = args[1];
			externalZonesFilename = args[2];
			networkFilename = args[3];
			plansFilename = args[4];	
			populationFraction = Double.parseDouble(args[5]);
		}
		
		List<Id> list = new ArrayList<Id>();
		try {
			BufferedReader br = IOUtils.getBufferedReader(externalZonesFilename);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					list.add(new IdImpl(line));
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.readLocationCoordinates(coordinateFilename);
		mdm.parseMatrix(matrixFilename, "Saturn", "Saturn model received for Sanral project");
		Scenario sc = mdm.generateDemand(list, new Random(5463), populationFraction);
		
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		nr.parse(networkFilename);
		
		XY2Links xy = new XY2Links((NetworkImpl) sc.getNetwork());
		xy.run(sc.getPopulation());
		
		PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		pw.write(plansFilename);
	}
	
}
