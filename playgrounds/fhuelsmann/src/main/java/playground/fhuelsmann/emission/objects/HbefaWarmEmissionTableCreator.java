/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission.objects;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class HbefaWarmEmissionTableCreator {

	/**
	 *  2-dim array , contains the values of HBEFA i.e Speed and Factor. 
	 *  for example 
	 *  [1][0]1;
	 *  [1][1]1;56.46376038;0.367850393
	 *  [1][2]1;48.9208374;0.349735767
	 *  [1][3]1;12.75674725;0.710567832
	 **/
	private final HbefaWarmEmissionFactors [] [] hbefaWarmTable = new HbefaWarmEmissionFactors [59][4];

	public HbefaWarmEmissionFactors[][] getHbefaWarmTable() {
		return hbefaWarmTable;}

	public void makeHbefaWarmTable(String filename){
		try{
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int place = 0;
			// TODO: test for header line! See e.g. ReadFromUrbansimParcelModel.java by kai
			// Read and forget header line
			br.readLine();
			// Read file line by line
			while ((strLine = br.readLine()) != null)   {

				String[] array = strLine.split(";");
				HbefaWarmEmissionFactors row = new HbefaWarmEmissionFactors(
						Integer.parseInt(array[1])      // Road_Category
						,array[2], 					    //IDTS
						Double.parseDouble(array[4]),   //S (speed)
						Double.parseDouble(array[5]),   //RPA
						Double.parseDouble(array[6]),   //%stop
						Double.parseDouble(array[7]),   //mKr
						Double.parseDouble(array[8]),   //EF_Nox
						Double.parseDouble(array[9]),   //EF_CO2(rep.)
						Double.parseDouble(array[10]),  //EF_CO2(total)
						Double.parseDouble(array[11]),  //NO2
						Double.parseDouble(array[12])); // PM

				int rowNumber = Integer.parseInt(array[1]);

				this.hbefaWarmTable [rowNumber] [place] = row;

				place++;
				if (place==4) place =0;
			}
			in.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
