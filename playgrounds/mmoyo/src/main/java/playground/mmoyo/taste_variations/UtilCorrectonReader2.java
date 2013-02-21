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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.mmoyo.analysis.counts.reader.TabularCountReader;

/**
 * Reads an tabular text with cadyts utility corrections per agent
 */
public class UtilCorrectonReader2 implements TabularFileHandler {
		private static final Logger log = Logger.getLogger(TabularCountReader.class);
		private static final String[] HEADER = {"ID", "CORR1", "CORR2", "CORR3", "CORR4", "CORR5", "CORR6", "CORR7", "CORR8", "CORR9", "CORR10"};
		private final TabularFileParserConfig tabFileParserConfig;
		private int rowNum=0;
		final static String TB = "\t";
		private Map <Id, double[]> correcMap = new TreeMap <Id, double[]>();  
		
		public UtilCorrectonReader2(){
			this.tabFileParserConfig = new TabularFileParserConfig();
			this.tabFileParserConfig.setDelimiterTags(new String[] {TB});
		}
		
		public void readFile(final String tabCountFile) throws IOException {
			this.tabFileParserConfig.setFileName(tabCountFile);
			new TabularFileParser().parse(this.tabFileParserConfig, this);
		}
		
		@Override
		public void startRow(String[] row) {
			if (rowNum>0) {
				Id agentId = new IdImpl(row[0]);
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
				uiCorrectionsFile= "../../input/choiceM44/10plans/10corrections.txt"; 	
			}

			//read utilities corrections
			UtilCorrectonReader2 reader= new UtilCorrectonReader2();
			try {
				reader.readFile(uiCorrectionsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}