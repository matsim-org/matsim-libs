package playground.acmarmol.matsim2030.actChainsForecast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;


import playground.acmarmol.Avignon.MainAvignon;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;

public class TimeSeriesInfoExtractor {
	
	
	private Population population;
	private ObjectAttributes populationAttributes;
	private ObjectAttributes householdAttributes;
	private int year;
	private int[] cohorts;
	private String[] cohorts_strings;
	final  Logger log = Logger.getLogger(TimeSeriesInfoExtractor.class);
	private BufferedWriter out;
	
	public TimeSeriesInfoExtractor(String populationInputFile, String populationAttributesInputFile, String householdAttributesInputFile, int year, int[] cohorts, String[] cohorts_strings){
		this.year = year;
		this.cohorts = cohorts;
		this.cohorts_strings = cohorts_strings;
		
		Config config = ConfigUtils.createConfig();
		config.setParam("plans", "inputPlansFile", populationInputFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.population = scenario.getPopulation();
		this.populationAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(populationAttributes);
		reader.putAttributeConverter(CoordImpl.class, new CoordConverter());
		reader.parse(populationAttributesInputFile);
		this.householdAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader readerHH = new ObjectAttributesXmlReader(householdAttributes);
		readerHH.putAttributeConverter(CoordImpl.class, new CoordConverter());
		readerHH.parse(householdAttributesInputFile);
	}
	
	
	public static void main(String[] args) throws IOException {
		
				
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		final int[] COHORTS = {1980,1970,1960,1950,1940,1930,1910,0};
		final String[] COHORTS_STRINGS =  {"   >1980","1970-1979","1960-1969","1950-1959","1940-1949","1930-1939","1910-1929","   <1910"};
		
		String populationInputFile;
		String populationAttributesInputFile;
		String householdAttributesInputFile;
		populationInputFile = inputBase + "population.12.MZ2005.xml";	
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2005.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2005.xml";
		TimeSeriesInfoExtractor extractorMZ2005 = new TimeSeriesInfoExtractor(populationInputFile,populationAttributesInputFile, householdAttributesInputFile, 2005, COHORTS, COHORTS_STRINGS);	
		populationInputFile = inputBase + "population.12.MZ2010.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2010.xml";
		TimeSeriesInfoExtractor extractorMZ2010 = new TimeSeriesInfoExtractor(populationInputFile,populationAttributesInputFile,householdAttributesInputFile, 2010, COHORTS, COHORTS_STRINGS);	
		
		
		
		extractorMZ2005.extractAndPrint(outputBase + "TimeSeriesInfoMZ2005.txt");
		extractorMZ2010.extractAndPrint(outputBase + "TimeSeriesInfoMZ2010.txt");
		
		

	}
	
	
	public void extractAndPrint(String outputFile) throws IOException{
		
		
		out = IOUtils.getBufferedWriter(outputFile);
		
		printTitle();
		filterPopulationOver18();
		ArrayList<Id>[] ids_cohort = getIdsByCohort();
		printTotalPersonsPerCohort(ids_cohort);
		printMeanAgePerCohort(ids_cohort);
		printDriverLicenseByCohort(ids_cohort);
		printCarAvailabilityByCohort(ids_cohort);
		printAbonnementOwnership(ids_cohort);
		printHalbTaxOwnershipByCohort(ids_cohort);
		printGAOwnershipByCohort(ids_cohort);
		printRegionalTicketOwnershipByCohort(ids_cohort);
		printAverageTripsByCohort(ids_cohort);
		printDailyDistanceByCohort(ids_cohort);
		printDailyTripsDurationByCohort(ids_cohort);
		printGroups();
		printAverageTripsByGroup(ids_cohort);
		printAverageDistanceByGroup(ids_cohort);
		printAverageDurationByGroup(ids_cohort);
		
		out.close();
	
	}

	private void printMeanAgePerCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE AGE PER COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal \tAv. Age " ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			ArrayList<Id> ids = ids_cohort[i];
			
			double sum_weight = 0;
			double total= 0;
			
			for(Id id: ids){
				
				Person person = population.getPersons().get(id);
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1; //Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				total = total + ((PersonImpl)person).getAge()*pw*hhw;
				sum_weight +=  pw*hhw;
		
				
			}
			out.write(cohorts_strings[i] + "\t" + total/sum_weight);
			out.newLine();
		}
		
		
	}


	private void printTitle() throws IOException {
		out.write("\t\t\t  Moblity tool ownership and use, MZ" +year);
		out.newLine();
		out.newLine();
		
	}


	private void printGroups() throws IOException {
		out.newLine();
		out.newLine();
		out.write("GROUP1: with at least one abonnement and car availability "); 
		out.newLine();
		out.write("GROUP2: without any abonnement and car availability "); 
		out.newLine();
		out.write("GROUP3: without at least one abonnement but no car availability "); 
		out.newLine();
		out.write("GROUP4: without abonnement nor car availability "); 
		out.newLine();
		out.newLine();

	}


	private void printAverageDurationByGroup(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE  DAILY DURATION BY COHORT AND GROUP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Group:   \t G1 \t\t G2 \t\t G3 \t\t G4" ); 
		out.newLine();
		out.write("Cohort   \t M \t F \t M \t F \t M \t F \t M \t F" ); 
		out.newLine();
		
		
		
		
		for(int i=0;i<cohorts_strings.length;i++){
						
						
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			 			 
			for(ArrayList<Id> gender: genders){ 
				
				ArrayList<Id>[] groups = this.getGroupIds(gender);
				
				for(ArrayList<Id> group :groups){
					
					double sum_weight = 0;
					double total = 0;
					for(Id id:group){
						
						double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
						total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DURATION))*pw*hhw;
						sum_weight +=  pw*hhw;
											
					}
								
					out.write( "\t" + total/sum_weight + "\t\t\t\t\t\t\t");
			   }	
			}
			
			out.newLine();
		}
				
	}


	private void printAverageDistanceByGroup(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE DAILY DISTANCE BY COHORT AND GROUP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Group:   \t G1 \t\t G2 \t\t G3 \t\t G4" ); 
		out.newLine();
		out.write("Cohort   \t M \t F \t M \t F \t M \t F \t M \t F" ); 
		out.newLine();
		
		
		
		
		for(int i=0;i<cohorts_strings.length;i++){
						
						
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			 			 
			for(ArrayList<Id> gender: genders){ 
				
				ArrayList<Id>[] groups = this.getGroupIds(gender);
				
				for(ArrayList<Id> group :groups){
					
					double sum_weight = 0;
					double total = 0;
					for(Id id:group){
						
						double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
						total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE))*pw*hhw;
						sum_weight +=  pw*hhw;
						
						
					}
								
					out.write( "\t" + total/sum_weight + "\t\t\t\t\t\t\t");
			   }	
			}
			
			out.newLine();
		}
		
		
	}


	private void printAverageTripsByGroup(ArrayList<Id>[] ids_cohort) throws IOException {
		
			
		
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE TRIPS BY COHORT AND GROUP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Group:   \t G1 \t\t G2 \t\t G3 \t\t G4" ); 
		out.newLine();
		out.write("Cohort   \t M \t F \t M \t F \t M \t F \t M \t F" ); 
		out.newLine();
		
		
		
		
		for(int i=0;i<cohorts_strings.length;i++){
						
						
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			 			 
			for(ArrayList<Id> gender: genders){ 
				
				ArrayList<Id>[] groups = this.getGroupIds(gender);
				
				for(ArrayList<Id> group :groups){
					
					double sum_weight = 0;
					double total = 0;
					for(Id id:group){
						
						double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
						total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
						sum_weight +=  pw*hhw;

						
					}
								
					out.write( "\t" + total/sum_weight + "\t\t\t\t\t\t\t");
			   }	
			}
			
			out.newLine();
		}
		
		
	}


	private void printDailyTripsDurationByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE DAILY TRAVEL DURATION BY COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \t M \t F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double sum_weight = 0;
			double total = 0;
			for(Id id:gender){
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				total += Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DURATION))*pw*hhw;
				sum_weight +=  pw*hhw;
				
				
			}
						
			out.write( "\t" + total/sum_weight + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
	}


	private void printDailyDistanceByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE DAILY DISTANCES BY COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \t M \t F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double sum_weight = 0;
			double total = 0;
			for(Id id:gender){
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE))*pw*hhw;
				sum_weight +=  pw*hhw;
				
			
			}
						
			out.write( "\t" + total/sum_weight + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
	}


	private void printAverageTripsByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE TRIPS PER DAY"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \t M \t F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double sum_weight = 0;
			double total = 0;
			for(Id id:gender){
			
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
						
			out.write( "\t" + total/sum_weight + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
		
	}


	private void printRegionalTicketOwnershipByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("REGIONAL SEASON TICKET OWNERSHIP BY COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal M \tShare M \tTotal F  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0; counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES) ){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100 + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
	}


	private void printGAOwnershipByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("GENERAL ABONNEMENT OWNERSHIP BY COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal M \tShare M \tTotal F  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0; counter = 0;
			double sum_weight=0;
			
			
			
			for(Id id:gender){
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES)
						 |((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES) ){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100 + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
	
		
	}


	private void printHalbTaxOwnershipByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("HALBTAX OWNERSHIP BY COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal M \tShare M \tTotal F  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
				
				
			
			double counter = 0; counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
			
	
			}
						
			out.write( "\t" + counter/sum_weight*100 + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
		
	}
	

	private void printAbonnementOwnership(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AT LEAST ONE ABONNEMENT OWNERSHIP BY COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal M \tShare M \tTotal F  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0; counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)
						|((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES)	
						|((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES)
						|((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES)
						|((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
			
	
			}

					
			out.write( "\t" + counter/sum_weight*100 + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
		
	}
	private void printCarAvailabilityByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("CAR AVAILABILITY PER COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal M \tShare M \tTotal F  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "availability: car")).equals(MZConstants.ALWAYS)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
			
	
			}
				
						
			out.write("\t" + counter/sum_weight*100 + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
	}


	private void printDriverLicenseByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tShare M  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100 + "\t\t\t\t\t\t\t");
			
			}
			
			out.newLine();
		}
		
		
	}


	private void printTotalPersonsPerCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		
		out.write("------------------------------"); 
		out.newLine();
		out.write("TOTAL PEOPLE PER COHORT"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \tTotal \tShare " ); 
		out.newLine();
		
		
		for(int i=0;i<cohorts_strings.length;i++){
			out.write(cohorts_strings[i] + "\t" + ids_cohort[i].size() + "\t" + (float)(((float)ids_cohort[i].size()*100)/population.getPersons().size())+ "%");
			out.newLine();
		}
		
		
	}


	public void filterPopulationOver18() throws IOException{
		
		out.write("Total population size: \t\t" + this.population.getPersons().size());
		out.newLine();
		
		ArrayList<Id>[] genders = this.getGenderIdsForCohort(new ArrayList(this.population.getPersons().keySet()));
		out.write("Total male: \t" + genders[0].size() + "\t" + (float)genders[0].size()*100/population.getPersons().size() );
		out.newLine();
		out.write("Total female: \t" + genders[1].size() + "\t" + (float)genders[1].size()*100/population.getPersons().size() );
		out.newLine();
		out.newLine();
		
	
		Set<Id> ids_to_remove = new HashSet<Id>();
		
		for(Person person: this.population.getPersons().values()){
			
			if(((PersonImpl)person).getAge() < 18){
				ids_to_remove.add(person.getId());
					
			}
			
		}
		
		population.getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total population over 18: \t\t" + this.population.getPersons().size());
		out.newLine();
		genders = this.getGenderIdsForCohort(new ArrayList(this.population.getPersons().keySet()));
		out.write("Total male: \t" + genders[0].size() + "\t" + (float)genders[0].size()*100/population.getPersons().size() );
		out.newLine();
		out.write("Total female: \t" + genders[1].size() + "\t" + (float)genders[1].size()*100/population.getPersons().size() );
		out.newLine();
		out.newLine();
		
		
	}
	
	private ArrayList<Id>[] getIdsByCohort() throws IOException {
		
		ArrayList<Id>[] ids_cohort = new ArrayList[this.cohorts.length];
		int[] max_age = new int[this.cohorts.length];
		
		for(int i=0;i<this.cohorts.length;i++){
			ids_cohort[i] = new ArrayList<Id>();
			max_age[i] = this.year-this.cohorts[i];
		}
		max_age[this.cohorts.length-1] = Integer.MAX_VALUE;
				
		for(Person person:population.getPersons().values()){
			
			for(int i=0;i<this.cohorts.length;i++){
				if(((PersonImpl)person).getAge() < max_age[i]){
					ids_cohort[i].add(person.getId());
					break;
				}
			}			
		}

	
		return ids_cohort;
	}

	
	private ArrayList<Id>[] getGenderIdsForCohort(ArrayList<Id> ids){
		
		ArrayList<Id>[] ids_gender = new ArrayList[2];
		ids_gender[0] = new ArrayList<Id>();
		ids_gender[1] = new ArrayList<Id>();
		
		for(Id id:ids){
			
			if(((PersonImpl)population.getPersons().get(id)).getSex().equals("m")){
				ids_gender[0].add(id);
			}else if(((PersonImpl)population.getPersons().get(id)).getSex().equals("f")){
				ids_gender[1].add(id);
			}else{Gbl.errorMsg("This should neber happen!: Gender "+ ((PersonImpl)population.getPersons().get(id)).getSex() +" unknown!");}
			
		}
		
		
		return ids_gender;
	}
	
	private ArrayList<Id>[] getGroupIds(ArrayList<Id> ids){
		
		ArrayList<Id>[] ids_groups = new ArrayList[4];
		ids_groups[0] = new ArrayList<Id>();
		ids_groups[1] = new ArrayList<Id>();
		ids_groups[2] = new ArrayList<Id>();
		ids_groups[3] = new ArrayList<Id>();
		
		for(Id id:ids){
			
			boolean hasAbonnement = (((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES) 
					| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES)
					| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)
					| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES));
			
			boolean hasCarAvailable = ((String)this.populationAttributes.getAttribute(id.toString(), "availability: car")).equals(MZConstants.ALWAYS);
					
					
			if(hasAbonnement & hasCarAvailable){
				ids_groups[0].add(id);
			} else if(!hasAbonnement & hasCarAvailable){
				ids_groups[1].add(id);
			} else if(hasAbonnement & !hasCarAvailable){
				ids_groups[2].add(id);
			} else if(!hasAbonnement & !hasCarAvailable){
				ids_groups[3].add(id);
			}
			
			

		}	
		
		return ids_groups;
	}

	

}
