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

import playground.mmoyo.analysis.counts.reader.TabularCountReader;

/**
 * Reads an tabular text with cadyts utility corrections per agent and their calculated parameter weights
 */
public class UtilCorrectonReader implements TabularFileHandler {
		private static final Logger log = Logger.getLogger(TabularCountReader.class);
		private static final String[] HEADER = {"ID", "SEL_INX", "CORR1", "CORR2", "CORR3", "CORR4", "wWALK", "wTIME", "wDIST", "wCHNG"};
		private final TabularFileParserConfig tabFileParserConfig;
		private int rowNum=0;
		final static String TB = "\t";
		private Map <Id, double[]> correcMap = new TreeMap <Id, double[]>();
		private Map <Id, IndividualPreferences> svdMap = new TreeMap <Id, IndividualPreferences>();
		
		public UtilCorrectonReader(){
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
				Id<Person> agentId = Id.create(row[0], Person.class);
				
				double correcArray[] = new double[5]; //it is the selected plan and 4 corrections
				correcArray[0]= Double.parseDouble(row[1]);
				correcArray[1] = Double.parseDouble(row[2]);
				correcArray[2] = Double.parseDouble(row[3]);
				correcArray[3] = Double.parseDouble(row[4]);
				correcArray[4] = Double.parseDouble(row[5]);
				correcMap.put(agentId, correcArray);

				IndividualPreferences svdValues = new IndividualPreferences(agentId, Double.parseDouble(row[6]), Double.parseDouble(row[7]), Double.parseDouble(row[8]),Double.parseDouble(row[9])); 
				svdMap.put(agentId, svdValues);				
				
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
		
		public Map <Id, double[]> getCorrecMap (){
			return correcMap;
		}
		
		public Map <Id, IndividualPreferences> getsvdMap (){
			return svdMap;
		}
		
		public static void main(String[] args) {
			String uiCorrectionsFile;
			if(args.length>0){
				uiCorrectionsFile= args[0];
			}else{
				uiCorrectionsFile= "../../input/choiceM44/7.cadytsUtlCorr_SVD.xls"; 	////"../../input/newDemand/ptLinecountsScenario/utlCorrections24hrs.txt";
			}

			//read utilities corrections
			UtilCorrectonReader reader= new UtilCorrectonReader();
			try {
				reader.readFile(uiCorrectionsFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			System.out.println(reader.correcMap.toString());
			System.out.println(reader.svdMap.toString());
		}
		
	}

