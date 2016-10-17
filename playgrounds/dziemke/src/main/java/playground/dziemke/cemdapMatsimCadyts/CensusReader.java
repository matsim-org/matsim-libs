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

package playground.dziemke.cemdapMatsimCadyts;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 */
public class CensusReader {
	private static final Logger LOG = Logger.getLogger(CensusReader.class);
	
	private ObjectAttributes municipalities = new ObjectAttributes();
	private List<String> municipalitiesList = new ArrayList<>();
			
	
	public CensusReader(String filePath, String delimiter) {
		readFile(filePath, delimiter);
	}
	
		
	private void readFile(final String filename, String delimiter) {
		LOG.info("Start reading " + filename);
				
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterRegex(";");
        new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
        	
            public void startRow(String[] row) {
        		
        		// Municipalities have this pattern; counties etc. are not considered
        		if (row[1].length() == 2 && row[2].length() == 1 && row[3].length() == 2 && row[5].length() == 3) {
        			String municipality = row[1] + row[2] + row[3]+ row[5];
        			municipalitiesList.add(municipality);
        			LOG.info("municipality = " + municipality);
        			
        			Integer populationMale = 0;
        			if (!row[10].equals("-")) {
        				populationMale = simplifyAndParseInteger(row[10]);
        			} else {
        				populationMale = 0;
        			}
        			LOG.info("populationMale = " + populationMale);
        			municipalities.putAttribute(municipality, "populationMale", populationMale);
        			
        			Integer populationFemale = 0;
        			if (!row[11].equals("-")) {
        				populationFemale = simplifyAndParseInteger(row[11]);
        			} else {
        				populationFemale = 0;
        			}
        			LOG.info("populationFemale = " + populationFemale);
        			municipalities.putAttribute(municipality, "populationFemale", populationFemale);

        			Integer marriedMale = 0;
        			if (!row[19].equals("-")) {
        				marriedMale = simplifyAndParseInteger(row[19]);
        			} else {
        				marriedMale = 0;
        			}
        			LOG.info("marriedMale = " + marriedMale);
        			municipalities.putAttribute(municipality, "marriedMale", marriedMale);

        			Integer marriedFemale = 0;
        			if (!row[20].equals("-")) {
        				marriedFemale = simplifyAndParseInteger(row[20]);
        			} else {
        				marriedFemale = 0;
        			}
        			LOG.info("marriedFemale = " + marriedFemale);
        			municipalities.putAttribute(municipality, "marriedFemale", marriedFemale);

        			Integer infantsMale = 0;
        			if (!row[73].equals("-") && !row[76].equals("-")) {
        				infantsMale = simplifyAndParseInteger(row[73]) + simplifyAndParseInteger(row[76]);
        			} else {
        				infantsMale = 0;
        			}
        			LOG.info("infantsMale = " + infantsMale);
        			municipalities.putAttribute(municipality, "infantsMale", infantsMale);

        			Integer infantsFemale;
        			if (!row[74].equals("-") && !row[77].equals("-")) {
        				infantsFemale = simplifyAndParseInteger(row[74]) + simplifyAndParseInteger(row[77]);
        			} else {
        				infantsFemale = 0;
        			}
        			LOG.info("infantsFemale = " + infantsFemale);
        			municipalities.putAttribute(municipality, "infantsFemale", infantsFemale);

        			Integer childrenMale;
        			if (!row[79].equals("-")) {
        				childrenMale = simplifyAndParseInteger(row[79]);
        			} else {
        				childrenMale = 0;
        			}
        			LOG.info("childrenMale = " + childrenMale);
        			municipalities.putAttribute(municipality, "childrenMale", childrenMale);

        			Integer childrenFemale;
        			if (!row[80].equals("-")) {
        				childrenFemale = simplifyAndParseInteger(row[80]);
        			} else {
        				childrenFemale = 0;
        			}
        			LOG.info("childrenFemale = " + childrenFemale);
        			municipalities.putAttribute(municipality, "childrenFemale", childrenFemale);

        			Integer adolescentsMale = 0;
        			if (!row[82].equals("-") && !row[85].equals("-")) {
        				adolescentsMale = simplifyAndParseInteger(row[82]) + simplifyAndParseInteger(row[85]);
        			} else {
        				adolescentsMale = 0;
        			}
        			LOG.info("adolescentsMale = " + adolescentsMale);
        			municipalities.putAttribute(municipality, "adolescentsMale", adolescentsMale);

        			Integer adolescentsFemale = 0;
        			if (!row[83].equals("-") && !row[86].equals("-")) {
        				adolescentsFemale = simplifyAndParseInteger(row[83]) + simplifyAndParseInteger(row[86]);
        			} else {
        				adolescentsFemale = 0;
        			}
        			LOG.info("adolescentsFemale = " + adolescentsFemale);
        			municipalities.putAttribute(municipality, "adolescentsFemale", adolescentsFemale);

        			Integer adultsMale = 0;
        			if (!row[88].equals("-") && !row[91].equals("-") && !row[94].equals("-") && !row[97].equals("-")) {
        				adultsMale = simplifyAndParseInteger(row[88]) + simplifyAndParseInteger(row[91]) + simplifyAndParseInteger(row[94]) + simplifyAndParseInteger(row[97]);
        			} else {
        				adultsMale = 0;
        			}
        			LOG.info("adultsMale = " + adultsMale);
        			municipalities.putAttribute(municipality, "adultsMale", adultsMale);

        			Integer adultsFemale = 0;
        			if (!row[89].equals("-") && !row[92].equals("-") && !row[95].equals("-") && !row[98].equals("-")) {
        				adultsFemale = simplifyAndParseInteger(row[89]) + simplifyAndParseInteger(row[92]) + simplifyAndParseInteger(row[95]) + simplifyAndParseInteger(row[98]);
        			} else {
        				adultsFemale = 0;
        			}
        			LOG.info("adultsFemale = " + adultsFemale);
        			municipalities.putAttribute(municipality, "adultsFemale", adultsFemale);

        			Integer seniorsMale = 0;
        			if (!row[100].equals("-") && !row[103].equals("-")) {
        				seniorsMale = simplifyAndParseInteger(row[100]) + simplifyAndParseInteger(row[103]);
        			} else {
        				seniorsMale = 0;
        			}
        			LOG.info("seniorsMale = " + seniorsMale);
        			municipalities.putAttribute(municipality, "seniorsMale", seniorsMale);

        			Integer seniorsFemale = 0;
        			if (!row[101].equals("-") && !row[104].equals("-")) {
        				seniorsFemale = simplifyAndParseInteger(row[101]) + simplifyAndParseInteger(row[104]);
        			} else {
        				seniorsFemale = 0;
        			}
        			LOG.info("seniorsFemale = " + seniorsFemale);
        			municipalities.putAttribute(municipality, "seniorsFemale", seniorsFemale);

        			Integer employedMale;
        			if (row.length > 123) { // Note: Length o row has to be considered here
        				employedMale = simplifyAndParseInteger(row[154]);
        			} else {
        				employedMale = 0;
        			}
        			LOG.info("employedMale = " + employedMale);
        			municipalities.putAttribute(municipality, "employedMale", employedMale);

        			Integer employedFemale = 0;
        			if (row.length > 123) { // Note: Length o row has to be considered here
        				employedFemale = simplifyAndParseInteger(row[155]);
        			} else {
        				employedFemale = 0;
        			}
        			LOG.info("employedFemale = " + employedFemale);
        			municipalities.putAttribute(municipality, "employedFemale", employedFemale);

        			Integer studying = 0;
        			if (row.length > 123) { // Note: Length o row has to be considered here
        				studying = simplifyAndParseInteger(row[194]);
        			} else {
        				studying = 0;
        			}
        			LOG.info("studying = " + studying);
        			municipalities.putAttribute(municipality, "studying", studying);
        		}
        	}
        });
	}
        
        
    private Integer simplifyAndParseInteger (String input) {
    	return Integer.parseInt(input.replace("(", "").replace(")", ""));
    }
    
    
	public ObjectAttributes getMunicipalities() {
		return this.municipalities;
	}
	
	
	public List<String> getMunicipalitiesList() {
		return this.municipalitiesList;
	}
}