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
package playground.fhuelsmann.emission;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import playground.fhuelsmann.emission.objects.HbefaObject;


public class HbefaTable {

	/**
	 *  2-dim array , contains the values of HBEFA i.e Speed and Factor. 
	 *  for example 
	 *  [1][0]1;
	 *  [1][1]1;56.46376038;0.367850393
	 *  [1][2]1;48.9208374;0.349735767
	 *  [1][3]1;12.75674725;0.710567832
	 **/

	private final HbefaObject [] [] hbefaTable = new HbefaObject [59][4];

	public HbefaObject[][] getHbefaTableWithSpeedAndEmissionFactor() {
		return hbefaTable;}

	public void makeHbefaTable(String filename){
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line

			int place = 0;

			br.readLine();

			while ((strLine = br.readLine()) != null)   {

				//for all lines (whole text) we split the line to an array 


				String[] array = strLine.split(";");
				HbefaObject obj = new HbefaObject(
						Integer.parseInt(array[1]) //Road_Category
						,array[2], //IDTS
						Double.parseDouble(array[4]), //S (speed)
						Double.parseDouble(array[5]), //RPA
						Double.parseDouble(array[6]), //%stop
						Double.parseDouble(array[7]), //mKr
						Double.parseDouble(array[8]), //EF_Nox
						Double.parseDouble(array[9]), //EF_CO2(rep.)
						Double.parseDouble(array[10]),//EF_CO2(total)
						Double.parseDouble(array[11]), //NO2
						Double.parseDouble(array[12])); // PM




				int row = Integer.parseInt(array[1]);

				this.hbefaTable [row] [place] = 
					obj;

				place++;
				if (place==4) place =0;

			}

			//Close the input stream
			in.close();
		} catch (Exception e) {
			// Pass this exception upwards, because there is no way we can continue if we couldn't read this file!
			throw new RuntimeException(e);
		}
	}

}
