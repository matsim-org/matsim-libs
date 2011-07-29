/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
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
package playground.fhuelsmann.emission.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class HbefaWarmEmissionTableCreatorDetailed {

	Map<String, HbefaWarmEmissionFactorsDetailed> hbefaWarmEmissionFactorsDetailed = new HashMap<String, HbefaWarmEmissionFactorsDetailed>() ;

	public void makeHbefaWarmTableDetailed(String filename){

		File file = new File(filename);
		BufferedReader reader = null;	

		try{
			reader = new BufferedReader(new FileReader(file));
			String text = null;
			reader.readLine();
			while ((text = reader.readLine()) != null) {

				// split is implemented in this class, see explanation.  
				String[] row = split(text,";");
				String key="";
				String[] value = new String[2];

				//create the key, the key is an array , hotKey	
				for(int i=0;i<13;i++)
					if (!(i==8 || i==9))
						key +=row[i]+";";

				//create the value, the value is an array , hotValue	
				value[0] = row[15];
				value[1] = row[18];

				hbefaWarmEmissionFactorsDetailed.put(key, new HbefaWarmEmissionFactorsDetailed(value));
			}
		}
		catch(Exception e ){
			throw new RuntimeException(e);
		}
	}

	public Map<String, HbefaWarmEmissionFactorsDetailed> getHbefaHot() {
		return hbefaWarmEmissionFactorsDetailed;
	}

	public void setHbefaHot(Map<String, HbefaWarmEmissionFactorsDetailed> hbefaHot) {
		hbefaWarmEmissionFactorsDetailed = hbefaHot;
	}

	private String[] split(String hbefa,String symbol) {
		String[] result = new String[27];
		int index=0;
		String part="";

		for(int i=0;i<hbefa.length();i++){
			if (hbefa.substring(i, i+1).equals(symbol)){
				result[index++]= ""+ part;
				part="";
			}
			else {
				part+= hbefa.substring(i,i+1);
			}
		}
		result[index++]=part;
		return result;
	}		
}