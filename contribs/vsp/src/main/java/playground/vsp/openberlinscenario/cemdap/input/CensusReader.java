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

package playground.vsp.openberlinscenario.cemdap.input;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dziemke
 */
public class CensusReader {
	private static final Logger LOG = Logger.getLogger(CensusReader.class);
	
	private ObjectAttributes municipalities = new ObjectAttributes();
	private List<String> municipalitiesList = new ArrayList<>();
	
	public static void main(String[] args) {
		String censusFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/zensus_2011/bevoelkerung/csv_Bevoelkerung/Zensus11_Datensatz_Bevoelkerung.csv";
		String delimiter = ";";
		
		CensusReader censusReader = new CensusReader(censusFile, delimiter);
		LOG.info("Simple test: 434 = " + censusReader.getMunicipalities().getAttribute("16077039", CensusAttributes.marriedMale.toString()));
	}
			
	
	public CensusReader(String filePath, String delimiter) {
		readFile(filePath, delimiter);
	}
	
		
	private void readFile(final String filename, String delimiter) {
		LOG.info("Start reading " + filename);
				
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterRegex(delimiter);
        new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {

            public void startRow(String[] row) {

        		// Municipalities have this pattern; counties etc. are not considered
        		if (row[1].length() == 2 && row[2].length() == 1 && row[3].length() == 2 && row[5].length() == 3) {
        			String municipality = row[1] + row[2] + row[3]+ row[5];
        			municipalitiesList.add(municipality);
        			LOG.info("municipality = " + municipality);

        			Integer population;
        			if (!row[9].equals("-")) {
        				population = simplifyAndParseInteger(row[9]);
        			} else {
        				population = 0;
        			}
        			LOG.info("population = " + population);
        			municipalities.putAttribute(municipality, CensusAttributes.population.toString(), population);

        			Integer populationMale;
        			if (!row[10].equals("-")) {
        				populationMale = simplifyAndParseInteger(row[10]);
        			} else {
        				populationMale = 0;
        			}
        			LOG.info("populationMale = " + populationMale);
        			municipalities.putAttribute(municipality, CensusAttributes.populationMale.toString(), populationMale);

        			Integer populationFemale;
        			if (!row[11].equals("-")) {
        				populationFemale = simplifyAndParseInteger(row[11]);
        			} else {
        				populationFemale = 0;
        			}
        			LOG.info("populationFemale = " + populationFemale);
        			municipalities.putAttribute(municipality, CensusAttributes.populationfemale.toString(), populationFemale);

        			Integer marriedMale;
        			if (!row[19].equals("-")) {
        				marriedMale = simplifyAndParseInteger(row[19]);
        			} else {
        				marriedMale = 0;
        			}
        			LOG.info("marriedMale = " + marriedMale);
        			municipalities.putAttribute(municipality, CensusAttributes.marriedMale.toString(), marriedMale);

        			Integer marriedFemale;
        			if (!row[20].equals("-")) {
        				marriedFemale = simplifyAndParseInteger(row[20]);
        			} else {
        				marriedFemale = 0;
        			}
        			LOG.info("marriedFemale = " + marriedFemale);
        			municipalities.putAttribute(municipality, CensusAttributes.marriedFemale.toString(), marriedFemale);

        			// ##############
					// 0-2

        			Integer pop0_2;
        			if (!row[72].equals("-")) {
        				pop0_2 = simplifyAndParseInteger(row[72]);
        			} else {
        				pop0_2 = 0;
        			}
        			LOG.info("pop0_2 = " + pop0_2);
        			municipalities.putAttribute(municipality, CensusAttributes.pop0_2.toString(), pop0_2);

        			Integer pop0_2Male;
        			if (!row[73].equals("-")) {
        				pop0_2Male = simplifyAndParseInteger(row[73]);
        			} else {
        				pop0_2Male = 0;
        			}
        			LOG.info("pop0_2Male = " + pop0_2Male);
        			municipalities.putAttribute(municipality, CensusAttributes.pop0_2Male.toString(), pop0_2Male);

        			Integer pop0_2Female;
        			if (!row[74].equals("-")) {
        				pop0_2Female = simplifyAndParseInteger(row[74]);
        			} else {
        				pop0_2Female = 0;
        			}
        			LOG.info("pop0_2Female = " + pop0_2Female);
        			municipalities.putAttribute(municipality, CensusAttributes.pop0_2Female.toString(), pop0_2Female);

					// ###############
					// 3-5

					Integer pop3_5;
					if (!row[75].equals("-")) {
						pop3_5 = simplifyAndParseInteger(row[75]);
					} else {
						pop3_5 = 0;
					}
					LOG.info("pop3_5 = " + pop3_5);
					municipalities.putAttribute(municipality, CensusAttributes.pop3_5.toString(), pop3_5);

					Integer pop3_5Male;
					if (!row[76].equals("-")) {
						pop3_5Male = simplifyAndParseInteger(row[76]);
					} else {
						pop3_5Male = 0;
					}
					LOG.info("pop3_5Male = " + pop3_5Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop3_5Male.toString(), pop3_5Male);

					Integer pop3_5Female;
					if (!row[77].equals("-")) {
						pop3_5Female = simplifyAndParseInteger(row[77]);
					} else {
						pop3_5Female = 0;
					}
					LOG.info("pop3_5Female = " + pop3_5Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop3_5Female.toString(), pop3_5Female);

					// ###############
					// 6-14

					Integer pop6_14;
					if (!row[78].equals("-")) {
						pop6_14 = simplifyAndParseInteger(row[78]);
					} else {
						pop6_14 = 0;
					}
					LOG.info("pop6_14 = " + pop6_14);
					municipalities.putAttribute(municipality, CensusAttributes.pop6_14.toString(), pop6_14);

					Integer pop6_14Male;
					if (!row[79].equals("-")) {
						pop6_14Male = simplifyAndParseInteger(row[79]);
					} else {
						pop6_14Male = 0;
					}
					LOG.info("pop6_14Male = " + pop6_14Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop6_14Male.toString(), pop6_14Male);

					Integer pop6_14Female;
					if (!row[80].equals("-")) {
						pop6_14Female = simplifyAndParseInteger(row[80]);
					} else {
						pop6_14Female = 0;
					}
					LOG.info("pop6_14Female = " + pop6_14Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop6_14Female.toString(), pop6_14Female);

					// ###############
					// 15-17

					Integer pop15_17;
					if (!row[81].equals("-")) {
						pop15_17 = simplifyAndParseInteger(row[81]);
					} else {
						pop15_17 = 0;
					}
					LOG.info("pop15_17 = " + pop15_17);
					municipalities.putAttribute(municipality, CensusAttributes.pop15_17.toString(), pop15_17);

					Integer pop15_17Male;
					if (!row[82].equals("-")) {
						pop15_17Male = simplifyAndParseInteger(row[82]);
					} else {
						pop15_17Male = 0;
					}
					LOG.info("pop15_17Male = " + pop15_17Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop15_17Male.toString(), pop15_17Male);

					Integer pop15_17Female;
					if (!row[83].equals("-")) {
						pop15_17Female = simplifyAndParseInteger(row[83]);
					} else {
						pop15_17Female = 0;
					}
					LOG.info("pop15_17Female = " + pop15_17Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop15_17Female.toString(), pop15_17Female);

					// ###############
					// 18-24

					Integer pop18_24;
					if (!row[84].equals("-")) {
						pop18_24 = simplifyAndParseInteger(row[84]);
					} else {
						pop18_24 = 0;
					}
					LOG.info("pop18_24 = " + pop18_24);
					municipalities.putAttribute(municipality, CensusAttributes.pop18_24.toString(), pop18_24);

					Integer pop18_24Male;
					if (!row[85].equals("-")) {
						pop18_24Male = simplifyAndParseInteger(row[85]);
					} else {
						pop18_24Male = 0;
					}
					LOG.info("pop18_24Male = " + pop18_24Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop18_24Male.toString(), pop18_24Male);

					Integer pop18_24Female;
					if (!row[86].equals("-")) {
						pop18_24Female = simplifyAndParseInteger(row[86]);
					} else {
						pop18_24Female = 0;
					}
					LOG.info("pop18_24Female = " + pop18_24Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop18_24Female.toString(), pop18_24Female);

					// ###############
					// 25-29

					Integer pop25_29;
					if (!row[87].equals("-")) {
						pop25_29 = simplifyAndParseInteger(row[87]);
					} else {
						pop25_29 = 0;
					}
					LOG.info("pop25_29 = " + pop25_29);
					municipalities.putAttribute(municipality, CensusAttributes.pop25_29.toString(), pop25_29);

					Integer pop25_29Male;
					if (!row[88].equals("-")) {
						pop25_29Male = simplifyAndParseInteger(row[88]);
					} else {
						pop25_29Male = 0;
					}
					LOG.info("pop25_29Male = " + pop25_29Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop25_29Male.toString(), pop25_29Male);

					Integer pop25_29Female;
					if (!row[89].equals("-")) {
						pop25_29Female = simplifyAndParseInteger(row[89]);
					} else {
						pop25_29Female = 0;
					}
					LOG.info("pop25_29Female = " + pop25_29Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop25_29Female.toString(), pop25_29Female);

					// ###############
					// 30-39

					Integer pop30_39;
					if (!row[90].equals("-")) {
						pop30_39 = simplifyAndParseInteger(row[90]);
					} else {
						pop30_39 = 0;
					}
					LOG.info("pop30_39 = " + pop30_39);
					municipalities.putAttribute(municipality, CensusAttributes.pop30_39.toString(), pop30_39);

					Integer pop30_39Male;
					if (!row[91].equals("-")) {
						pop30_39Male = simplifyAndParseInteger(row[91]);
					} else {
						pop30_39Male = 0;
					}
					LOG.info("pop30_39Male = " + pop30_39Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop30_39Male.toString(), pop30_39Male);

					Integer pop30_39Female;
					if (!row[92].equals("-")) {
						pop30_39Female = simplifyAndParseInteger(row[92]);
					} else {
						pop30_39Female = 0;
					}
					LOG.info("pop30_39Female = " + pop30_39Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop30_39Female.toString(), pop30_39Female);

					// ###############
					// 40-49

					Integer pop40_49;
					if (!row[93].equals("-")) {
						pop40_49 = simplifyAndParseInteger(row[93]);
					} else {
						pop40_49 = 0;
					}
					LOG.info("pop40_49 = " + pop40_49);
					municipalities.putAttribute(municipality, CensusAttributes.pop40_49.toString(), pop40_49);

					Integer pop40_49Male;
					if (!row[94].equals("-")) {
						pop40_49Male = simplifyAndParseInteger(row[94]);
					} else {
						pop40_49Male = 0;
					}
					LOG.info("pop40_49Male = " + pop40_49Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop40_49Male.toString(), pop40_49Male);

					Integer pop40_49Female;
					if (!row[95].equals("-")) {
						pop40_49Female = simplifyAndParseInteger(row[95]);
					} else {
						pop40_49Female = 0;
					}
					LOG.info("pop40_49Female = " + pop40_49Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop40_49Female.toString(), pop40_49Female);

					// ###############
					// 50-64

					Integer pop50_64;
					if (!row[96].equals("-")) {
						pop50_64 = simplifyAndParseInteger(row[96]);
					} else {
						pop50_64 = 0;
					}
					LOG.info("pop50_64 = " + pop50_64);
					municipalities.putAttribute(municipality, CensusAttributes.pop50_64.toString(), pop50_64);

					Integer pop50_64Male;
					if (!row[97].equals("-")) {
						pop50_64Male = simplifyAndParseInteger(row[97]);
					} else {
						pop50_64Male = 0;
					}
					LOG.info("pop50_64Male = " + pop50_64Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop50_64Male.toString(), pop50_64Male);

					Integer pop50_64Female;
					if (!row[98].equals("-")) {
						pop50_64Female = simplifyAndParseInteger(row[98]);
					} else {
						pop50_64Female = 0;
					}
					LOG.info("pop50_64Female = " + pop50_64Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop50_64Female.toString(), pop50_64Female);

					// ###############
					// 65-74

					Integer pop65_74;
					if (!row[99].equals("-")) {
						pop65_74 = simplifyAndParseInteger(row[99]);
					} else {
						pop65_74 = 0;
					}
					LOG.info("pop65_74 = " + pop65_74);
					municipalities.putAttribute(municipality, CensusAttributes.pop65_74.toString(), pop65_74);

					Integer pop65_74Male;
					if (!row[100].equals("-")) {
						pop65_74Male = simplifyAndParseInteger(row[100]);
					} else {
						pop65_74Male = 0;
					}
					LOG.info("pop65_74Male = " + pop65_74Male);
					municipalities.putAttribute(municipality, CensusAttributes.pop65_74Male.toString(), pop65_74Male);

					Integer pop65_74Female;
					if (!row[101].equals("-")) {
						pop65_74Female = simplifyAndParseInteger(row[101]);
					} else {
						pop65_74Female = 0;
					}
					LOG.info("pop65_74Female = " + pop65_74Female);
					municipalities.putAttribute(municipality, CensusAttributes.pop65_74Female.toString(), pop65_74Female);

					// ###############
					// 75Plus

					Integer pop75Plus;
					if (!row[102].equals("-")) {
						pop75Plus = simplifyAndParseInteger(row[102]); // This is column "DEM_4_34"
					} else {
						pop75Plus = 0;
					}
					LOG.info("pop75Plus = " + pop75Plus);
					municipalities.putAttribute(municipality, CensusAttributes.pop75Plus.toString(), pop75Plus);

					Integer pop75PlusMale;
					if (!row[103].equals("-")) {
						pop75PlusMale = simplifyAndParseInteger(row[103]); // This is column "DEM_4_35"
					} else {
						pop75PlusMale = 0;
					}
					LOG.info("pop75PlusMale = " + pop75PlusMale);
					municipalities.putAttribute(municipality, CensusAttributes.pop75PlusMale.toString(), pop75PlusMale);

					Integer pop75PlusFemale;
					if (!row[104].equals("-")) {
						pop75PlusFemale = simplifyAndParseInteger(row[104]); // This is column "DEM_4_36"
					} else {
						pop75PlusFemale = 0;
					}
					LOG.info("pop75PlusFemale = " + pop75PlusFemale);
					municipalities.putAttribute(municipality, CensusAttributes.pop75PlusFemale.toString(), pop75PlusFemale);

					// ###############
					// Employees

        			Integer employedMale;
        			if (row.length > 123) { // Note: Length of row has to be considered here
        				employedMale = simplifyAndParseInteger(row[154]); // This is column "ERW_1.8"
        			} else {
        				employedMale = 0;
        			}
        			LOG.info("employedMale = " + employedMale);
        			municipalities.putAttribute(municipality, CensusAttributes.employedMale.toString(), employedMale);

        			Integer employedFemale;
        			if (row.length > 123) { // Note: Length of row has to be considered here
        				employedFemale = simplifyAndParseInteger(row[155]); // This is column "ERW_1.9"
        			} else {
        				employedFemale = 0;
        			}
        			LOG.info("employedFemale = " + employedFemale);
        			municipalities.putAttribute(municipality, CensusAttributes.employedFemale.toString(), employedFemale);
        			
        			// ###############
					// Students

        			Integer studying;
        			if (row.length > 123) { // Note: Length of row has to be considered here
        				studying = simplifyAndParseInteger(row[194]);
        			} else {
        				studying = 0;
        			}
        			LOG.info("studying = " + studying);
        			municipalities.putAttribute(municipality, CensusAttributes.studying.toString(), studying);

        			LOG.info("----------");
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
}