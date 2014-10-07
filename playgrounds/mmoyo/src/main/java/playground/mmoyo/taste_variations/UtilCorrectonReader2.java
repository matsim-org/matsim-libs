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

package playground.mmoyo.taste_variations;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * Reads an tabular text with Cadyts utility corrections per agent. Look at the header structure before using it!
 */
public class UtilCorrectonReader2 implements TabularFileHandler {
		private static final Logger log = Logger.getLogger(UtilCorrectonReader2.class);
		private String[] HEADER;//  This is an example of header:{"ID", "SELIIDX", "CORR1", "CORR2", "CORR3", "CORR4", "CORR5"};
		private final TabularFileParserConfig tabFileParserConfig;
		private int rowNum=0;
		final static String TB = "\t";
		private Map <Id, double[]> correcMap = new TreeMap <Id, double[]>();  
		
		public UtilCorrectonReader2(int numCorr){
			this.tabFileParserConfig = new TabularFileParserConfig();
			this.tabFileParserConfig.setDelimiterTags(new String[] {TB});
			
			//create flexible header
			final String strCORR= "CORR";
			int numCols= 2+ numCorr;
			HEADER = new String[numCols];
			HEADER[0]= "ID";
			HEADER[1]= "SELIIDX";
			for (int i=2; i<numCols; i++){
				HEADER[i]= strCORR + (i-1);
			}
			
		}
		
		public void readFile(final String tabCountFile) throws IOException {
			this.tabFileParserConfig.setFileName(tabCountFile);
			new TabularFileParser().parse(this.tabFileParserConfig, this);
		}
		
		@Override
		public void startRow(String[] row) {
			if (rowNum>0) {
				Id<Person> agentId = Id.create(row[0], Person.class);
				int corrNum = row.length-2;                             //only corrections
				double correcArray[] = new double[corrNum]; 
				for (int i=0; i<corrNum ; i++){                          // the row contains at the beginning:  [id, selindx,  .... ]  they are not put into the correcArray
					correcArray[i]= Double.parseDouble(row[i+2]);    
				}
				correcMap.put(agentId, correcArray);
			}else{
				boolean equalsHeader = true;
				int i = 0;
				for (String s : row) {
					if (!s.equalsIgnoreCase(HEADER[i])){
						equalsHeader = false;
						break;
					}
					i++;
				}
				if (!equalsHeader) {
					log.warn("the structure does not match. The header should be:  ");
					for (String g : HEADER) {
						System.out.print(g + " ");
					}
					System.out.println();
				}
			}
			rowNum++;
		}
		
		protected Map <Id, double[]> getCorrecMap(){
			return correcMap;
		}
		
		public static void main(String[] args) {
			String uiCorrectionsFile;
			if(args.length>0){
				uiCorrectionsFile= args[0];
			}else{
				uiCorrectionsFile= "../../"; 	
			}

			//read utilities corrections
			UtilCorrectonReader2 reader= new UtilCorrectonReader2(5);
			try {
				reader.readFile(uiCorrectionsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			for(Map.Entry <Id,double[]> entry: reader.getCorrecMap().entrySet() ){
				Id key = entry.getKey();
				double[] value = entry.getValue();
				System.out.print("\n" + key);
				for (int i =0;i<value.length; i++){
					System.out.print("   " + value[i]);
				}
			}
		
		}
		
	}