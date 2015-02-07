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

package playground.acmarmol.matsim2030.microcensus1994;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.matsim.vehicles.Vehicles;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.EtappenLoader;
import playground.acmarmol.matsim2030.microcensus2010.Etappe;
import playground.acmarmol.matsim2030.microcensus2010.MZ2010EtappenParser;
import playground.acmarmol.matsim2030.microcensus2010.MZPopulationUtils;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.EtappeConverter;



/**
* 
* MATSim-DB: creates population, vehicles and households and xml files from MicroCensus 1994 database
* 
*
* @author acmarmol
* 
*/

public class MZ1994ToXmlFiles {

	private final static Logger log = Logger.getLogger(MZ1994ToXmlFiles.class);
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) throws Exception {
		
		System.out.println("MATSim-DB: creates population, vehicles and households and xml files from MicroCensus 1994 database \n");
		
		
		// from local directory
		//String inputBase = "P:/Daten/Mikrozensen Verkehr Schweiz/2010/3_DB_SPSS/dat files/";
		String inputBase = "P:/Daten/Mikrozensen Verkehr Schweiz/MZ'94/SPSS/dat_files/";
				args = new String[] {
				inputBase+"haushalte.dat",
				inputBase+"haushaltspersonen.dat",
				inputBase+"fahrzeuge.dat",
				inputBase+"zielpersonen.dat",
				inputBase+"wege.dat",
				inputBase+"ausgaenge.dat",
				inputBase+"etappen.dat",
				"C:/local/marmolea/output/MicroCensus1994/"
				//"C:/local/marmolea/output/MicroCensus2010/"
		};
		
		if (args.length != 8) {
			log.error("createMZ2Plans haushalteFile haushaltspersonenFile fahrzeugeFile zielpersonenFile wegeFile ausgaengeFile etappenFile outputBase");
			System.exit(-1);
		}

		Gbl.startMeasurement();
		
		// store input parameters
		String haushalteFile = args[0];
		String haushaltspersonenFile = args[1];
		String fahrzeugeFile = args[2];
		String zielpersonenFile = args[3];
		String wegeFile = args[4];
		String ausgaengeFile = args[5];
		String etappenFile = args[6];
		String outputBase = args[7];

		// print input parameters
		log.info("haushalteFile: "+haushalteFile);
		log.info("haushaltspersonenFile: "+haushaltspersonenFile);
		log.info("fahrzeugeFile: "+fahrzeugeFile);
		log.info("zielpersonenFile: "+zielpersonenFile);
		log.info("wegeFile: "+wegeFile);
		log.info("ausgaengeFile: "+ausgaengeFile);
		log.info("etappenFile: "+etappenFile);
		log.info("outputBase: "+outputBase);
		System.out.println("\n");
		
		
		// Things to create: households, persons and vehicles, incl. additional object attributes
		//scenario
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseHouseholds(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		//population
		Population population = scenario.getPopulation();
		ObjectAttributes populationAttributes = new ObjectAttributes();
		ObjectAttributes householdpersonsAttributes = new ObjectAttributes();
		
		//households
		Households households = scenario.getHouseholds();
		ObjectAttributes householdAttributes = new ObjectAttributes();
		//vehicles
		Vehicles vehicles = scenario.getVehicles();
		ObjectAttributes vehiclesAttributes = new ObjectAttributes();
		ObjectAttributes wegeAttributes = new ObjectAttributes(); 
		//wegeAttributes is just used while handling border crossing trips, to identify the border crossing.
				
		Gbl.printElapsedTime();
		
//////////////////////////////////////////////////////////////////////
//////////////////////PARSING/////////////////////////////////////////
//////////////////////////////////////////////////////////////////////	

		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing haushalteFile...");
		new MZ1994HouseholdParser(households,householdAttributes).parse(haushalteFile);
		log.info("done. (parsing haushalteFile)");
				
		Gbl.printElapsedTime();


		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.00.xml.gz");
		ObjectAttributesXmlWriter households_axmlw = new ObjectAttributesXmlWriter(householdAttributes);
		households_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		households_axmlw.writeFile(outputBase+"/householdAttributes.00.xml.gz");
		log.info("done. (writing)");
				
		Gbl.printElapsedTime();
		
/////////////////////////////////////////////////////////////////////////
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("parsing fahrzeugeFile...");
//		// next fill up the vehicles. For that you need to doublecheck consistency with the households (as given in the MZ database structure)
//		// and probably add additional data to the households too
//		//new MZVehicleParser(vehicles,vehiclesAttributes,households,householdAttributes).parse(fahrzeugeFile);
//		log.info("done. (parsing fahrzeugeFile)");
//				
//		Gbl.printElapsedTime();
//
//		log.info("writing intermediate files...");
//		//new HouseholdsWriterV10(households).writeFile(outputBase+"/households.02.xml.gz");
//		//households_axmlw.writeFile(outputBase+"/householdAttributes.02.xml.gz");
//		//new VehicleWriterV1(vehicles).writeFile(outputBase+"vehicles.02.xml.gz");
//		//new ObjectAttributesXmlWriter(vehiclesAttributes).writeFile(outputBase+"/vehiclesAttributes.02.xml.gz");
//		log.info("done. (writing)");
//		
//		Gbl.printElapsedTime();
//		
//////////////////////////////////////////////////////////////////////	
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing zielpersonenFile...");
		new MZ1994ZielPersonParser(population,populationAttributes,households,householdAttributes).parse(zielpersonenFile);
		log.info("done. (parsing zielpersonenFile)");
				
		Gbl.printElapsedTime();

		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.03.xml.gz");
		households_axmlw.writeFile(outputBase+"/householdAttributes.03.xml.gz");
		new PopulationWriter(population, null).write(outputBase+"population.03.xml");
		ObjectAttributesXmlWriter population_axmlw = new ObjectAttributesXmlWriter(populationAttributes);
		population_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		population_axmlw.writeFile(outputBase+"/populationAttributes.03.xml.gz");
		log.info("done. (writing)");
		
		int original_pop_size = population.getPersons().size();
		
		Gbl.printElapsedTime();
		
////////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing haushaltspersonenFile...");
		new MZ1994HouseholdPersonParser(households,population, populationAttributes, householdpersonsAttributes).parse(haushaltspersonenFile);
		log.info("done. (parsing haushaltspersonenFile)");
		
		Gbl.printElapsedTime();
		
		log.info("writing intermediate files...");
		//population_axmlw.writeFile(outputBase+"/populationAttributes.04.xml");
		//new HouseholdsWriterV10(households).writeFile(outputBase+"/households.01.xml.gz");
		//households_axmlw.writeFile(outputBase+"/householdAttributes.01.xml.gz");
		new ObjectAttributesXmlWriter(householdpersonsAttributes).writeFile(outputBase+"/householdpersonsAttributes.01.xml");
		log.info("done. (writing)");
		
		Gbl.printElapsedTime();
		
	
////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("parsing wegeFile...");
		ArrayList<Set<Id>> pids = new MZ1994WegeParser(population, wegeAttributes).parse(wegeFile);
		log.info("done. (parsing wegeFile)");
		
		Gbl.printElapsedTime();
		
		log.info("writing intermediate files...");
		new HouseholdsWriterV10(households).writeFile(outputBase+"/households.04.xml");
		households_axmlw.writeFile(outputBase+"/householdAttributes.04.xml");
		new PopulationWriter(population, null).write(outputBase+"population.04.xml");
		population_axmlw.writeFile(outputBase+"/populationAttributes.04.xml");
		ObjectAttributesXmlWriter wege_axmlw = new ObjectAttributesXmlWriter(wegeAttributes);
		wege_axmlw.putAttributeConverter(CoordImpl.class, new CoordConverter());
		wege_axmlw.putAttributeConverter(Etappe.class, new EtappeConverter());
		wege_axmlw.writeFile(outputBase+"/wegeAttributes.00.xml");

		log.info("done. (writing)");
		
		Gbl.printElapsedTime();
//////////////////////////////////////////////////////////////////////
		//no mode specification in wege file! need to get this data from etappen level
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("parsing etappenFile...");		
//		TreeMap<String, ArrayList<playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.ocupancyRate.Etappe>> etappes = EtappenLoader.loadData(2000);
//		MZPopulationUtils.setMZ2000LegModesWithEtappenInfo(population, etappes);
//		new PopulationWriter(population, null).write(outputBase+"population.05.xml");
//		
//		
//		MZPopulationUtils.setHomeLocationsMZ2000FromHouseholdAdress(population, populationAttributes, householdAttributes, wegeAttributes);
//		new PopulationWriter(population, null).write(outputBase+"population.06.xml");
//		new MZ2000EtappenParser(wegeAttributes).parse(etappenFile);
//		wege_axmlw.writeFile(outputBase+"/wegeAttributes.01.xml.gz");
//		log.info("done. (parsing wegeFile)");
//		
//		Gbl.printElapsedTime();
//////////////////////////////////////////////////////////////////////
//		
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("setting work locations...");
//		MZPopulationUtils.setWorkLocations(population, populationAttributes);
//		System.out.println("      done.");
//		System.out.println("      Writing population with work coords set xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.05.xml.gz");
//		System.out.println("  done.");
//
//////////////////////////////////////////////////////////////////////
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("setting home locations...");
//		MZPopulationUtils.setHomeLocations(population, householdAttributes, populationAttributes);
//		System.out.println("      done.");
//		System.out.println("      Writing population with home coords set xml file \n");
//		new PopulationWriter(population, null).write(outputBase+"population.06.xml.gz");
//		System.out.println("  done.");
//
		
//////////////////////////////////////////////////////////////////////		
//////////////////////FILTERING/////////////////////////////////////////
//////////////////////////////////////////////////////////////////////
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("removing persons with coord inconsistencies...");
//		Set<Id> coord_err_pids = pids.get(0);
//		if(coord_err_pids.size()>0){
//			MZPopulationUtils.removePlans(population, coord_err_pids);
//			System.out.println("      done.");
//			System.out.println("      Total persons removed: " +  coord_err_pids.size());
//			System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//			System.out.println("      Writing population without coord. inconsistencies xml file \n");	
//			//new PopulationWriter(population, null).write(outputBase+"population.07.xml.gz");
//			System.out.println("  done.");
//			
//			}else{System.out.println("      NO PEOPLE WITH COORD INCONSISTENCIES \n");} 
//		
//////////////////////////////////////////////////////////////////////////
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("removing persons with time inconsistencies...");
//		Set<Id> time_err_pids = pids.get(1);
//		if(time_err_pids.size()>0){
//		MZPopulationUtils.removePlans(population, time_err_pids);
//		System.out.println("      done.");
//		System.out.println("      Total persons removed: " + time_err_pids.size());
//		System.out.println("      Remaining population size: " + population.getPersons().size()+" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//		System.out.println("      Writing population without time  inconsistencies xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.08.xml.gz");
//		System.out.println("  done.");
//		
//		}else{System.out.println("      NO PEOPLE WITH TIME INCONSISTENCIES \n");}
//		
//		
//////////////////////////////////////////////////////////////////////
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("removing persons with all plan outside switzerland...");
//		Set<Id> out_pids = MZPopulationUtils.identifyPlansOutOfSwitzerland(population, wegeAttributes, "");
//		if(out_pids.size()>0){
//		MZPopulationUtils.removePlans(population, out_pids);
//		System.out.println("      done.");
//		System.out.println("      Total persons removed: " + out_pids.size());
//		System.out.println("      Remaining population size: " + population.getPersons().size()+" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//		System.out.println("      Writing population without time  inconsistencies xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.09.xml");
//		System.out.println("  done.");
//		
//		}else{System.out.println("      NO PEOPLE WITH PLANS COMPLETELY OUT OF SWITZERLAND \n");}
////////////////////////////////////////////////////////////////////////
////		
////		
////		//NOTE: after handling border crossing tips, references to weges and etappen are lost
////		//because some legs are removed from agents plans!!!
////		//-> wegeAttributes no longer useful!		
////		
////		
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("handling border-crossing trips...");
//		ArrayList<Set<Id>> border_crossing_plane_ids = MZPopulationUtils.identifyCrossBorderWeges(population, wegeAttributes, "Schweiz");
//		Set<Id> border_crossing_wids = border_crossing_plane_ids.get(1);
//		if(border_crossing_wids.size()>0){
//		MZPopulationUtils.HandleBorderCrossingTrips(population, wegeAttributes, border_crossing_wids, "Schweiz");
//		System.out.println("      done.");
//		System.out.println("      Total trips handled: " + border_crossing_wids.size());
//		System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//		System.out.println("      Writing population without undefined coords xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.10.xml");
//		System.out.println("  done.");
//	
//		}else{System.out.println("      NO BORDER CROSSING TRIPS \n");}
//		
//
//////////////////////////////////////////////////////////////////////////
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("removing persons with undefined coords...");
//		Set<Id> undef_neg_pids = MZPopulationUtils.identifyPlansWithUndefinedNegCoords(population);
//		if(undef_neg_pids.size()>0){
//		MZPopulationUtils.removePlans(population, undef_neg_pids);
//		System.out.println("      done.");
//		System.out.println("      Total persons removed: " + undef_neg_pids.size());
//		System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//		System.out.println("      Writing population without undefined coords xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.11.xml");
//		System.out.println("NUMBER OF UNDEFINED  COORDS "+undef_neg_pids.size());
//		System.out.println("  done.");
//	
//		}else{System.out.println("      NO PEOPLE WITH UNDEFINED COORDS \n");}
//
//////////////////////////////////////////////////////////////////////////
//
//		System.out.println("-----------------------------------------------------------------------------------------------------------");
//		log.info("removing persons with negative coords...");
//		Set<Id> neg_coord_pids = MZPopulationUtils.identifyPlansWithNegCoords(population);
//		if(neg_coord_pids.size()>0){
//		MZPopulationUtils.removePlans(population, neg_coord_pids);
//		System.out.println("      done.");
//		System.out.println("      Total persons removed: " + neg_coord_pids.size());
//		System.out.println("      Remaining population size: " + population.getPersons().size() +" (" + (double)population.getPersons().size()/(double)original_pop_size*100 + "%)");
//		System.out.println("      Writing population without negative coords xml file \n");	
//		new PopulationWriter(population, null).write(outputBase+"population.12.xml");
//		System.out.println("NUMBER OF PEOPLE WITH NEGATIVE COORDS "+neg_coord_pids.size());
//		System.out.println("  done.");
//		
//		}else{System.out.println("      NO PEOPLE WITH NEGATIVE COORDS \n");}
//

//////////////////////////////////////////////////////////////////////
		System.out.println("-----------------------------------------------------------------------------------------------------------");
		log.info("Finished filtering population. Las population size = "+ population.getPersons().size());

		Gbl.printElapsedTime();
		
//		MZPopulationUtils.classifyActivityChains(population);
		

	}//end main		


}

	
	
	
	
	
	
	

		

		
	

	
		

		

	
	

