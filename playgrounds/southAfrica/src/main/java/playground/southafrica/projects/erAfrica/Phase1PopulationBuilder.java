/* *********************************************************************** *
 * project: org.matsim.*
 * BuildCapeTownPopulation.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.southafrica.projects.erAfrica;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.IncomeImpl;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.population.capeTownTravelSurvey.SurveyParser;
import playground.southafrica.population.census2011.attributeConverters.CoordConverter;
import playground.southafrica.population.census2011.capeTown.SurveyPlanPicker;
import playground.southafrica.population.census2011.capeTown.facilities.AmenityParser;
import playground.southafrica.population.census2011.capeTown.facilities.InformalHousingParser;
import playground.southafrica.population.census2011.capeTown.facilities.LanduseParser;
import playground.southafrica.population.freight.ChainChopper;
import playground.southafrica.population.freight.FreightPopulationSampler;
import playground.southafrica.population.utilities.PopulationUtils;
import playground.southafrica.population.utilities.SAIncomeConverter;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * This class tries to, in a repeatable and reproducible manner, generate an
 * entire synthetic population for the City of Cape Town. The idea is that this
 * is a single-run process with no manual intervention. The exception would be
 * for the R analysis of activity durations.<br><br>
 * 
 * TODO The post-R portion is covered in ...
 * 
 * @author jwjoubert
 */
public class Phase1PopulationBuilder {
	final private static Logger LOG = Logger.getLogger(Phase1PopulationBuilder.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Phase1PopulationBuilder.class.toString(), args);

		/* Parse the arguments */
		String censusPopulationFolder = args[0]; /* This should be in WGS84_SA_Albers. */
		String sample = args[1];
		String outputFolder = args[2];
		String surveyFolder = args[3];
		String surveyShapefileWgs = args[4];
		String surveyShapefileZoneIdField = args[5];
		String surveyShapefileLo19 = args[6];
		String osmFile = args[7];
		String formalShapefile = args[8];
		String informalShapefile = args[9];
		String commercial = args[10];
		String commercialAttributes = args[11];
		
		/* Clean up and process the arguments. */
		censusPopulationFolder += censusPopulationFolder.endsWith("/") ? "" : "/";
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		surveyFolder += surveyFolder.endsWith("/") ? "" : "/";

		/* Set up the folder structure. */
		outputFolder += "Population_" + sample + "/" + getDateString();
		setup(getSourceFolder(censusPopulationFolder, sample), outputFolder);

		/* Start the magic... */
		step1SurveyParser(outputFolder, surveyFolder, surveyShapefileWgs, surveyShapefileZoneIdField);
		step2AssignTravelDemand(outputFolder, surveyShapefileLo19);
		step3ParseAmenities(outputFolder, osmFile, formalShapefile, informalShapefile);
		step4RelocateActivities(outputFolder);
		step5SampleCommercial(outputFolder, sample, commercial, commercialAttributes);
		step6ExtractActivityDurations(outputFolder);

		/* Activity duration folder. */
		new File(outputFolder + "durations/").mkdirs();
		
		/* Cleanup. */
		FileUtils.delete(new File(outputFolder + "tmp/"));
		FileUtils.delete(new File(outputFolder + "survey/"));
		
		Header.printFooter();
	}

	/**
	 * Executing the class {@link SurveyParser}.
	 * 
	 * @param root
	 * @param surveyFolder
	 * @param shapefile
	 * @param shapefileIdField
	 */
	private static void step1SurveyParser(String root, String surveyFolder,
			String shapefile, String shapefileIdField){
		LOG.info("Parsing the travel survey...");
		String[] args = {shapefile, shapefileIdField,
				surveyFolder + "persons.csv",
				surveyFolder + "households.csv",
				surveyFolder + "householdsDerived.csv",
				surveyFolder + "diary.csv",
				root + "survey/",
				root + "survey/diaryOnly/"};
		new File(root + "survey/").mkdirs();
		new File(root + "survey/diaryOnly/").mkdirs();
		SurveyParser.runSurveyParser(args);
		
		LOG.info("Done parsing the travel survey.");
	}

	/**
	 * Executing {@link SurveyPlanPicker} for Cape Town. The original survey
	 * was generated in the {@link TransformationFactory#WGS84} coordinate 
	 * reference system. For Cape Town the accepted standard is 
	 * {@link TransformationFactory#HARTEBEESTHOEK94_LO19}. From this step 
	 * forward we need a projected coordinate reference system.
	 * @param root
	 * @param shapefile
	 */
	private static void step2AssignTravelDemand(String root, String shapefile){
		LOG.info("Assigning travel demand, i.e. plans...");
		String[] args = {
				root + "survey/diaryOnly/",
				root + "tmp/",
				shapefile,
				TransformationFactory.WGS84_SA_Albers,
				TransformationFactory.HARTEBEESTHOEK94_LO19};
		SurveyPlanPicker.runSurveyPlanPicker(args);
		LOG.info("Done assigning plans.");
	}

	
	/**
	 * Parsing amenities from three sources: 1) OpenStreetMap; 2) City of Cape
	 * Town's formal land use shapefile; and 3) City of Cape Town's informal
	 * settlement shapefile.
	 *  
	 * @param root
	 * @param osmFile
	 * @param landuseFile
	 * @param informalFile
	 */
	private static void step3ParseAmenities(
			String root, String osmFile, String landuseFile, String informalFile){
		/* Parse the OSM facilities. */
		LOG.info("Parsing the OpenStreetMap amenities...");
		String[] osmArgs = {osmFile, root + "tmp/osmFacilities.xml.gz"};
		AmenityParser.runAmenityParser(osmArgs);
		
		/* Parse the land use facilities. */
		LOG.info("Parsing the formal housing shapefile...");
		String[] formalArgs = {landuseFile, root + "tmp/formalFacilities.xml.gz"};
		LanduseParser.runLanduseParser(formalArgs);

		/* Parse the land use facilities. */
		LOG.info("Parsing the informal housing shapefile...");
		String[] informalArgs = {informalFile, root + "tmp/informalFacilities.xml.gz"};
		InformalHousingParser.runInformalHousingParser(informalArgs);
		LOG.info("Done parsing amenities.");
	}

	
	private static void step4RelocateActivities(String root){
		LOG.info("Relocate person activities...");
		
		/* Build the scenario with the correct elements. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(root + "tmp/persons_withPlans.xml.gz");
		
		ObjectAttributesXmlReader pa = new ObjectAttributesXmlReader(sc.getPopulation().getPersonAttributes());
		pa.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
		pa.readFile(root + "tmp/personAttributes.xml.gz");
		
		new HouseholdsReaderV10(sc.getHouseholds()).readFile(root + "tmp/households.xml.gz");
		ObjectAttributesXmlReader ha = new ObjectAttributesXmlReader(sc.getHouseholds().getHouseholdAttributes());
		ha.putAttributeConverter(IncomeImpl.class, new SAIncomeConverter());
		ha.putAttributeConverter(Coord.class, new CoordConverter());
		ha.readFile(root + "tmp/householdAttributes_withPlanHome.xml.gz");
		
		ERAfricaActivityLocationAdjuster ala = new ERAfricaActivityLocationAdjuster(sc);
		ala.buildQuadTreeFromFacilities(
				root + "tmp/osmFacilities.xml.gz", 
				root + "tmp/formalFacilities.xml.gz",
				root + "tmp/informalFacilities.xml.gz");
		ala.processHouseholds();
		
		ala.writeTuples(root);
		ala.writeTries(root);
		ala.writeUpdatedScenario(sc, root);
		
		LOG.info("Done relocating activities.");
	}
	
	/**
	 * 
	 * TODO Need to incorporate BUILDING an actual commercial vehicle (sub)population
	 * in this step as well. Currently we just use a/the Cape Town one from
	 * earlier runs. The input population is assumed to be a 100% sample, and
	 * have already been "chopped" into 24-hour segments. See {@link ChainChopper}
	 * for details.
	 * 
	 * @param root
	 * @param commercial
	 */
	private static void step5SampleCommercial(String root, String sample, 
			String commercial, String commercialAttributes){
		LOG.info("Sampling the commercial vehicle subpopulation...");
		
		double fraction = 0.0;
		switch (sample) {
		case "001":
			fraction = 0.01;
			break;
		case "010":
			fraction = 0.10;
			break;
		case "100":
			fraction = 1.0;
			break;
		default:
			throw new RuntimeException("Don't know what sample size " + sample + " is.");
		}
		String[] freightArgs = {
				commercial,
				commercialAttributes,
				String.valueOf(fraction),
				getDateString().substring(0, getDateString().length()-1),
				root + "tmp/commercial.xml.gz",
				root + "commercialAttributes.xml.gz"
		};
		FreightPopulationSampler.run(freightArgs);
		LOG.info("Done sampling the commercial vehicle subpopulation.");
		
		/* The original commercial vehicle population was done in WGS84_SA_Albers
		 * coordinate reference system. So I need to manually now convert it to
		 * Hartebeesthoek, Lo19 NE (even though the national freight would be
		 * better using Lo29. */
		LOG.info("Converting freight coordinates...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84_SA_Albers, 
				TransformationFactory.HARTEBEESTHOEK94_LO19);
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(root + "tmp/commercial.xml.gz");
		for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
			Plan plan = sc.getPopulation().getPersons().get(id).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity activity = (Activity)pe;
					activity.setCoord(ct.transform(activity.getCoord()));
				}
			}
		}
		/*FIXME This only writes a V5 population until Via can visualise later versions. */
		new PopulationWriter(sc.getPopulation()).writeV5(root + "commercial.xml.gz");
	}
	
	private static void step6ExtractActivityDurations(String root){
		LOG.info("Extracting activity durations for persons...");
		PopulationUtils.extractActivityDurations(root + "persons.xml.gz", root + "personsActivityDurations.csv.gz");
		LOG.info("Done extracting activity durations for persons.");

		LOG.info("Extracting activity durations for freight...");
		PopulationUtils.extractActivityDurations(root + "commercial.xml.gz", root + "commercialActivityDurations.csv.gz");
		LOG.info("Done extracting activity durations for freight.");
	}
	
	
	/**
	 * Ensure there is a tmp folder for the intermediary files, and it only 
	 * contains the original, unedited population.  
	 */
	private static void setup(String censusFolder, String outputFolder){
		LOG.info("Setting up folder structure...");
		/* Empty and create a temporary folder. */
		File outFolder = new File(outputFolder);
		File tmpFolder = new File(outputFolder + "tmp/");
		if(outFolder.exists()){
			LOG.warn("The output folder will be deleted!");
			LOG.warn("Deleting " + outFolder.getAbsolutePath());
			FileUtils.delete(outFolder);
		}
		tmpFolder.mkdirs();

		/* Copy the (raw) population from then Treasury 2014 data. */
		try {
			FileUtils.copyFile(
					new File(censusFolder + "population.xml.gz"), 
					new File(outputFolder + "tmp/persons.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "populationAttributes.xml.gz"), 
					new File(outputFolder + "tmp/personAttributes.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "populationAttributes.xml.gz"), 
					new File(outputFolder + "personAttributes.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "households.xml.gz"), 
					new File(outputFolder + "tmp/households.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "households.xml.gz"), 
					new File(outputFolder + "households.xml.gz"));
			FileUtils.copyFile(
					new File(censusFolder + "householdAttributes_withPlanHome.xml.gz"), 
					new File(outputFolder + "tmp/householdAttributes.xml.gz"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot copy base treasury files.");
		}
	}

	
	/**
	 * Based on the location of the Treasury root folder, and the required 
	 * sample size, this class just find the correct population folder within
	 * the known tree structure.
	 * 
	 * @param censusFolder
	 * @param sample
	 * @return
	 */
	public static String getSourceFolder(String censusFolder, String sample){
		String folder = null;
		switch (sample) {
		case "001":
			folder = censusFolder + "sample/001/CapeTown/";
			break;
		case "010":
			folder = censusFolder + "sample/010/CapeTown/";
			break;
		case "100":
			folder = censusFolder + "full/CapeTown/";
			break;
		default:
			LOG.error("Don't know how to interpret sample " + sample);
			break;
		}
		return folder;
	}
	
	/**
	 * Returns a string, followed by a trailing slash, of the current date in 
	 * the format YYYYMMDD.
	 *  
	 * @return
	 */
	public static String getDateString(){
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		String s = String.format("%d%02d%02d/", 
				cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH)+1,
				cal.get(Calendar.DAY_OF_MONTH)); 
		return s;
	}
	
}
