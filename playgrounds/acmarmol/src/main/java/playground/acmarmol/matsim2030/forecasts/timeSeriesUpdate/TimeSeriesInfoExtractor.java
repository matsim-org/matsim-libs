package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.EtappenLoader;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.Wege;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.weges.WegeLoader;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

public class TimeSeriesInfoExtractor {
	
	
	private Microcensus microcensus;
	private int[] cohorts;
	private String[] cohorts_strings;
	private int[] age_groups;
	private String[] age_group_strings;
	final  Logger log = Logger.getLogger(TimeSeriesInfoExtractor.class);
	private BufferedWriter out;
	
	public TimeSeriesInfoExtractor(Microcensus microcensus,int[] cohorts, String[] cohorts_strings, int[] age_groups, String[] age_group_strings){

		this.microcensus = microcensus;
		this.cohorts = cohorts;
		this.cohorts_strings = cohorts_strings;
		this.age_group_strings = age_group_strings;
		this.age_groups = age_groups;
		
	}
	
	
	public static void main(String[] args) throws Exception {
		
				
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		
		final int[] COHORTS = {1990,1980,1970,1960,1950,1940,1930,1910,0};
		final String[] COHORTS_STRINGS =  {"1990-1999","1980-1989","1970-1979","1960-1969","1950-1959","1940-1949","1930-1939","1910-1929","<1910"};
				
//		final int[] COHORTS = {2000,1990,1980,1970,1960,1950,1940,1930,1920,1910,0};
//		final String[] COHORTS_STRINGS =  {">1999","1990-1999","1980-1989","1970-1979","1960-1969","1950-1959","1940-1949","1930-1939","1920-1929","1910-1919","<1910"};
		
		//5-year cohorts
		//final int[] COHORTS = {1990,1985,1980,1975,1970,1965,1960,1955,1950,1945,1940,0};
		//final String[] COHORTS_STRINGS =  {"1990-1994"  ,"1985-1989","1980-1984","1975-1979","1970-1974","1965-1969","1960-1964","1955-1959","1950-1954","1945-1949","1940-1944","<1940"};
		
		
		final int[] AGE_GROUPS = {25,45,65,Integer.MAX_VALUE};
		final String[] AGE_GROUPS_STRINGS = {"18-24","25-44","45-64","65+"};
		
		String populationInputFile;
		String householdInputFile;
		String populationAttributesInputFile;
		String householdAttributesInputFile;
		String householdpersonsAttributesInputFile;
		Microcensus microcensus;
		
		populationInputFile = inputBase + "population.05.MZ1989.xml";	
		householdInputFile = inputBase + "households.04.MZ1989.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ1989.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ1989.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 1989);
		TimeSeriesInfoExtractor extractorMZ1989 = new TimeSeriesInfoExtractor(microcensus, COHORTS, COHORTS_STRINGS, AGE_GROUPS, AGE_GROUPS_STRINGS);	
				
		populationInputFile = inputBase + "population.04.MZ1994.xml";	
		householdInputFile = inputBase + "households.04.MZ1994.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ1994.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ1994.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 1994);
		TimeSeriesInfoExtractor extractorMZ1994 = new TimeSeriesInfoExtractor(microcensus, COHORTS, COHORTS_STRINGS, AGE_GROUPS, AGE_GROUPS_STRINGS);	
		
		//---------------------------------------------------------
//		populationInputFile = inputBase + "population.03.zid.MZ2000.xml";
//		populationAttributesInputFile = inputBase + "populationAttributes.04.zid.MZ2000.xml";
		
		populationInputFile = inputBase + "population.09.MZ2000.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.itnr.MZ2000.xml";
		
		
		householdInputFile = inputBase + "households.04.MZ2000.xml";
				
		//populationInputFile = inputBase + "population.03.MZ2000.xml";
		//populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2000.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2000.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 2000);
		TimeSeriesInfoExtractor extractorMZ2000 = new TimeSeriesInfoExtractor(microcensus, COHORTS, COHORTS_STRINGS, AGE_GROUPS, AGE_GROUPS_STRINGS);	
		
		
		populationInputFile = inputBase + "population.12.MZ2005.xml";
		householdInputFile = inputBase + "households.04.MZ2005.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2005.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2005.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 2005);
		TimeSeriesInfoExtractor extractorMZ2005= new TimeSeriesInfoExtractor(microcensus, COHORTS, COHORTS_STRINGS, AGE_GROUPS, AGE_GROUPS_STRINGS);	
				
		populationInputFile = inputBase + "population.12.MZ2010.xml";
		householdInputFile = inputBase + "households.04.MZ2010.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2010.xml";
		householdpersonsAttributesInputFile = inputBase + "householdpersonsAttributes.01.MZ2010.xml";
		microcensus = new MicrocensusV2(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, householdpersonsAttributesInputFile, 2010);
		TimeSeriesInfoExtractor extractorMZ2010 = new TimeSeriesInfoExtractor(microcensus, COHORTS, COHORTS_STRINGS, AGE_GROUPS, AGE_GROUPS_STRINGS);	
		
//		extractorMZ1989.extractAndPrint(outputBase + "TimeSeriesInfoMZ1989.txt");
//		extractorMZ1994.extractAndPrint(outputBase + "TimeSeriesInfoMZ1994.txt");
		extractorMZ2000.extractAndPrint(outputBase + "TimeSeriesInfoMZ2000.txt");
		extractorMZ2005.extractAndPrint(outputBase + "TimeSeriesInfoMZ2005.txt");
		extractorMZ2010.extractAndPrint(outputBase + "TimeSeriesInfoMZ2010.txt");		
		
		 

	}
	
	
	public void extractAndPrint(String outputFile) throws Exception {
		
		
		out = IOUtils.getBufferedWriter(outputFile);
		
//		printTitle();
		printPopulationSize("Total population size");
		
		filterPopulationEmployed();
		filterPopulationWeekday();
//		filterByGender(MZConstants.MALE);
//		filterByAgeRange(30,49);
//		filterByIncome(7000);
		
//		filterPopulationWithWorkActivity();
		filterPopulationOver18();
		ArrayList<Id>[] ids_cohort = getIdsByCohort();
//		printTotalPersonsPerCohort(ids_cohort);
		printMeanAgePerCohort(ids_cohort);
//		printDriverLicenseByCohortAndGender(ids_cohort);
//		printDriverLicenseByCohortAndResidence(ids_cohort);
//		printDriverLicenseByCohortAndResidenceBigCities(ids_cohort);
//		printDriverLicenseByCohortAndSeasonTicket(ids_cohort);
//		printDriverLicenseByCohortAndPW_GA(ids_cohort);
//		printCarAvailabilityByCohort(ids_cohort);
//		printAbonnementOwnership(ids_cohort);
//		printHalbTaxOwnershipByCohort(ids_cohort);
//		printGAOwnershipByCohort(ids_cohort);
//		printRegionalTicketOwnershipByCohort(ids_cohort);
//		printAverageTripsByCohort(ids_cohort);
//		printDailyDistanceByCohort(ids_cohort);
//		printDailyTripsDurationByCohort(ids_cohort);
//		printGroups();
//		printAverageTripsByGroup(ids_cohort);
//		printAverageDistanceByGroup(ids_cohort);
//		printAverageDurationByGroup(ids_cohort);
//		//-------------------------------------------
//		//new graphs
//		ArrayList<Id>[] ids_age_group = this.getIdsByAgeGroup();
//		printDrivingLicenseByAgeGroup(ids_age_group);
//		printCarAvailavilityByAgeGroup(ids_age_group);
//		
//		ArrayList<Id>[] ids_PW = this.getIdsByPWOwnership();
//		printAbonnementsByPWOwnership(ids_PW);
//		
//		ArrayList<Id>[] ids_PW_season = this.getIdsByPWAndSeasonTicket();
//		printMotorizedVsPTTrips(ids_PW_season);
		
		//newer graphs
//		printOccupancyRateData();
//		printAverageTripsByCohortAndPWAvailability(ids_cohort);
//		printDailyDistanceByCohortAndPWAvailability(ids_cohort);

//		printHoursInAndOutOfHomeByCohort(ids_cohort);
//		printOutOfHomeTripsActivitiesRatioByCohortAndGender(ids_cohort);
		
//		printNumberOfJourneysByCohort(ids_cohort);		
//		printNumberOfActivitiesByCohort(ids_cohort);
//		printNumberOfActivitiesByCohortAndGender(ids_cohort);
//		printHoursInActivitiesByCohort(ids_cohort);
//		printHoursSpentbyActivity(ids_cohort);
		
//		printLegDepartureTimes();
//		printGoingToActivityHours(MZConstants.LEISURE);
//		printReturningFromActivityHours(MZConstants.SHOPPING);
		
//		printIncomeMean();
//		printDriverLicenseByCohortAndIncomeTercile(ids_cohort);
//		printCarAvailabilityByCohortAndIncomeTercile(ids_cohort);
//		printAbonnementOwnershipByCohortAndIncomeTercile(ids_cohort); //use MZ2000 with zid as id
//		printAverageTripsByCohortAndIncomeTercile(ids_cohort);
//		printDailyTripsDurationByCohortAndIncomeTercile(ids_cohort);
		printDailyDistancesByCohortAndIncomeTercile(ids_cohort);	
		
//		printIncomeTerciles();
		
//		printTripsOutOfHomeActivitesRatioByCohortAndAge(ids_cohort);
//		printShareOfCarTripsByCohortAndGroup(ids_cohort);
		
//		printPopulationIDs();
		
//		printNCarsAndNDrivingLicenseInHouseholdRatio(ids_cohort);
		
		out.close();
	
	}
	
	private void printNCarsAndNDrivingLicenseInHouseholdRatio(ArrayList<Id>[] ids_cohort) throws IOException {
		
		out.write("Number of Cars and Driving License Ratio (per household)"); 
		out.newLine();

		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
		
			for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
				String id = household.getId().toString();
				double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
			
				int total_cars = Integer.parseInt((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.TOTAL_CARS));
				int total_licenses = 0;
				
				List<Id<Person>> members = household.getMemberIds();
				
				for(int j=0;j<=members.size()-1;j++){
					
					Id m_id = members.get(j);
					int license = (((String)((MicrocensusV2) microcensus).getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES))?1:0;
					total_licenses+= license;
					
				}
				
			
			}
		}
		
	}
	
	private void printPopulationIDs() throws IOException {
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			out.write(person.getId().toString());
			out.newLine();
			
		}
		
	}


	private void printTripsOutOfHomeActivitesRatioByCohortAndAge(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("TRIPS - OUT OF HOME ACTIVITIES RATIO BY COHORT AND GENDER"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t M  \t F" ); 
		out.newLine();
		
		
		
//		TreeMap<String, ArrayList<Wege>> all_weges= WegeLoader.loadData(microcensus.getYear());
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] groups = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> group: groups){ 
			
			double total = 0;
			double sum_weight=0;
			for(Id id:group){
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
//				total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
				
				double nr_trips = 0;
				double ooh_activities = 0;	
				
				Person person = microcensus.getPopulation().getPersons().get(id);
								
				if(person.getSelectedPlan()==null)
					continue;
						
					for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
						
						if(!(pe instanceof Activity)){
							nr_trips++;
							continue;
						}
							
						
						Activity activity = (Activity)pe;
						if(!activity.getType().equals(MZConstants.HOME))
							ooh_activities++;
					}
				
						
//				ArrayList<Wege> weges = all_weges.get(id.toString());
//				if(weges!=null){
//					nr_trips =weges.size();
//				}
					
				if(nr_trips==0){
					System.out.println("");
				}
				
				System.out.println(ooh_activities/nr_trips);
				
				total += ooh_activities/nr_trips*pw;
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + total/sum_weight);
			
			}
			
			out.newLine();
		}
		
		
	}
	
	private void printIncomeTerciles() throws IOException {
		
		double[] terciles = this.getIncomeTerciles();
		
		out.write("------------------------------"); 
		out.newLine();
		out.write("INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Year: " + microcensus.getYear());
		out.newLine();
		out.write("First Tercile:  0<= income <=" + terciles[0]);
		out.newLine();
		out.write("Second Tercile:  " + terciles[0] + "< income <=" +terciles[1]);
		out.newLine();
		out.write("Third Tercile:  " + terciles[1] + "< income ");
		out.newLine();
		
//		ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(new ArrayList<Id>(microcensus.getPopulation().getPersons().keySet()));
//		System.out.println(this.microcensus.getYear());
//		System.out.println();
//		System.out.println("\t\t" + this.microcensus.getPopulation().getPersons().size());
//		System.out.println( "\t" + terciles[0].size());
//		System.out.println("\t" +terciles[1].size());
//		System.out.println("\t" +terciles[2].size());
//		System.out.println("\t\t" +(terciles[0].size()+terciles[1].size()+terciles[2].size()));
//		System.out.println();
		
	}


	private void printShareOfCarTripsByCohortAndGroup(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("SHARE OF CAR TRIPS PER COHORT AND OTHER GROUP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tShare M  \tShare F" ); 
		out.newLine();
		
		TreeMap<String, ArrayList<Wege>> all_weges= WegeLoader.loadData(microcensus.getYear());
		TreeMap<String, ArrayList<Etappe>> etappes = EtappenLoader.loadData(this.microcensus.getYear());
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] groups = getIncomeTercilesIdsForCohort(ids);
			//getIdsByResidenceBigCities(ids);
			// getGenderIdsForCohort(ids);
			 
		 
			for(ArrayList<Id> group: groups){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:group){
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				
				//WEGE
//				ArrayList<Wege> weges = all_weges.get(id.toString());
//				
//				if(weges!=null){
//					for(Wege wege:weges){
//						
//						if(wege.getMode().equals(MZConstants.CAR))
//						counter+= pw*hhw;
//						
//						sum_weight +=  pw*hhw;
//					}										
//				}
				
				
				//ETAPPEN
				if(etappes.containsKey(id.toString())){
					ArrayList<Etappe> etappen = etappes.get(id.toString());
					
					for(Etappe etappe: etappen){
						
						if(etappe.getMode().equals(MZConstants.CAR_FAHRER)||etappe.getMode().equals(MZConstants.CAR_MITFAHRER))
							counter+= pw*hhw;
						
						sum_weight +=  pw*hhw;
					}
				}
				
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
	}


	private void printDriverLicenseByCohortAndIncomeTercile(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t First Tercile  \t Second Tercile \t Third Tercile" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(ids);
			 
			for(ArrayList<Id> tercile: terciles){ 
			
			double counter = 0;
			double sum_weight=0;
			
			
			
			for(Id id:tercile){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
		
	}
	
	private ArrayList<Id>[] getIncomeTercilesIdsForCohort(ArrayList<Id> ids) {
		
		
		double[] terciles = getIncomeTerciles();
		
		
		ArrayList<Id>[] ids_terciles = new ArrayList[3];
		ids_terciles[0] = new ArrayList<Id>();
		ids_terciles[1] = new ArrayList<Id>();
		ids_terciles[2] = new ArrayList<Id>();
		
		for(Id id:ids){
			
			String hhnr =	(String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER);
			
			if(((String) microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL)).equals(MZConstants.UNSPECIFIED))
				continue;
			
			double income = Double.parseDouble((String) microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL));
			
//			if(income<0)
//				continue;
			
			if(income<=terciles[0]){
				ids_terciles[0].add(id);
			}else if(income<=terciles[1]){
				ids_terciles[1].add(id);
			}else{
				ids_terciles[2].add(id);
			}
			
		}
		
		
		return ids_terciles;

	}


	private double[]  getIncomeTerciles() {
		
		double[] terciles = new double[2];
		TreeMap<Double, Double> incomes =  new TreeMap<Double, Double>();
		
		double total_weight = 0;
		
		for(Household household : microcensus.getHouseholds().getHouseholds().values()){
			String income_string = (String) microcensus.getHouseholdAttributes().getAttribute(household.getId().toString(), MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL);
			if(income_string.equals(MZConstants.UNSPECIFIED))
				continue;
			double income = Double.parseDouble(income_string);
			double weight = Double.parseDouble((String) microcensus.getHouseholdAttributes().getAttribute(household.getId().toString(), MZConstants.HOUSEHOLD_WEIGHT));
				if(incomes.get(income)==null)
					incomes.put(income, 0.0);
			
				incomes.put(income, incomes.get(income)+ weight);
				total_weight+=weight;
		}
		
		double tercile_limit = total_weight/3;
		double first_tercile_income = getPercentileWithLimit(incomes, tercile_limit);
		double second_tercile_income = getPercentileWithLimit(incomes, 2*tercile_limit);
		
		terciles[0] = first_tercile_income;
		terciles[1] = second_tercile_income;
		
		return terciles;
		
	}

	private void  printIncomeMean() throws IOException {
		
		double total_weight = 0;
		double total_income = 0;
		double counter1 = 0;
		double counter2 = 0;
		
		for(Household household : microcensus.getHouseholds().getHouseholds().values()){
			
			counter1++;

			String income_string = (String) microcensus.getHouseholdAttributes().getAttribute(household.getId().toString(), MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL);
			if(income_string.equals(MZConstants.UNSPECIFIED)){
				counter2++;
			}else{
			double income = Double.parseDouble(income_string);
			double weight = Double.parseDouble((String) microcensus.getHouseholdAttributes().getAttribute(household.getId().toString(), MZConstants.HOUSEHOLD_WEIGHT));
			total_weight += weight;
			total_income += income*weight;		
			
			}	
		}
		out.write( "Average household income: " + total_income/total_weight);
		out.newLine();
		out.write( "% of households considered: " +  (1-counter2/counter1)*100);
	}
	
	
	private double getPercentileWithLimit(TreeMap<Double, Double> incomes, Double limit){
		
		double percentile = 0;
		double cum_sum=0;

		for(Double j : incomes.keySet()){
			cum_sum+= incomes.get(j);
			
			if(cum_sum>limit){
				percentile = j;
				break;
			}
		}
		
		return percentile;
	}
	
	private void printLegDepartureTimes() throws IOException{
		
		out.write("------------------------------"); 
		out.newLine();
		out.write("LEG DEPARTURE TIMES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		
		Double[] distribution = new Double[24]; //time intervals of 1-hour
		//Double[] distribution = new Double[6*24];
		java.util.Arrays.fill(distribution, 0.0);
		
		int counter = 0;
		
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.PERSON_WEIGHT));
			Plan plan = person.getSelectedPlan();
			
			if(plan==null)
				continue; //avoid persons without plan
			
			
			for(PlanElement pe: plan.getPlanElements()){
				
				if(!(pe instanceof Leg))
					continue;
				
				Leg leg = (Leg)pe;
				double dep_t = leg.getDepartureTime();
					
					//1 hour intervals
					int pos = (int) (dep_t/3600);
					if(pos>23)
						pos-=24;
					
					//10 min intervals
//					int pos = (int) (dep_t/600);
//					if(pos>23*6+5)
//						pos-=24*6;

				counter++;						
				distribution[pos]+=pw;						
							
			}			
		}	

//		double sum=0.0;
//		LinkedList<Double> mov_sum = new LinkedList<Double>();
//		for(int j=0;j<=6;j++){
//		mov_sum.addFirst(distribution[j]);
//		sum+=distribution[j];
//		}
//		out.write( "\t" + sum);	
//		
//		for(int j=6; j<distribution.length;j++){
//		
//			sum +=  distribution[j] - mov_sum.pollLast();
//			mov_sum.addFirst(distribution[j]);
//			
//			out.write( "\t" + sum);		
//		}
		
		for(int j=0; j<distribution.length;j++){
			
			out.write( "\t" + distribution[j]);		
		}
		out.write( "\n\n Number of LEGS: " + counter);	
	}

	private void printReturningFromActivityHours(String activityType) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("RETURN FROM " + activityType); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		
		//Double[] distribution = new Double[24]; //time intervals of 1-hour
		Double[] distribution = new Double[6*24];
		java.util.Arrays.fill(distribution, 0.0);
		
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.PERSON_WEIGHT));
			Plan plan = person.getSelectedPlan();
			
			if(plan==null)
				continue; //avoid persons without plan
			
			PlanElement[] pe = plan.getPlanElements().toArray(new PlanElement[plan.getPlanElements().size()]);
			
			for(int i=2;i<pe.length-1;i+=2){
				
				ActivityImpl activity = (ActivityImpl) pe[i];
				if(activity.getType().equals(activityType)){
					
					LegImpl leg = (LegImpl) pe[i+1];
					double dep_t = leg.getDepartureTime();
					
					//1 hour intervals
//					int pos = (int) (dep_t/3600);
//					if(pos>23)
//						pos-=24;
					
					//10 min intervals
					int pos = (int) (dep_t/600);
					if(pos>23*6+5)
						pos-=24*6;
					
					distribution[pos]+=pw;						
				}				
			}			
		}	

//		double sum=0.0;
//		LinkedList<Double> mov_sum = new LinkedList<Double>();
//		for(int j=0;j<=6;j++){
//		mov_sum.addFirst(distribution[j]);
//		sum+=distribution[j];
//		}
//		out.write( "\t" + sum);	
//		
//		for(int j=6; j<distribution.length;j++){
//		
//			sum +=  distribution[j] - mov_sum.pollLast();
//			mov_sum.addFirst(distribution[j]);
//			
//			out.write( "\t" + sum);		
//		}
		
		for(int j=0; j<distribution.length;j++){
			
			out.write( "\t" + distribution[j]);		
		}
		
	}


	private void printGoingToActivityHours(String activityType) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("GOING " + activityType); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		int counter = 0;

		Double[] distribution = new Double[24]; //time intervals of 1-hour
		//Double[] distribution = new Double[6*24];
		java.util.Arrays.fill(distribution, 0.0);
		
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.PERSON_WEIGHT));
			Plan plan = person.getSelectedPlan();
			
			if(plan==null)
				continue; //avoid persons without plan
			
			PlanElement[] pe = plan.getPlanElements().toArray(new PlanElement[plan.getPlanElements().size()]);
			
			for(int i=2;i<pe.length;i+=2){
				
				ActivityImpl activity = (ActivityImpl) pe[i];
				//if(activity.getType().equals(activityType)){
					
					LegImpl leg = (LegImpl) pe[i-1];
					counter++;
					double dep_t = leg.getDepartureTime();
		
					//1 hour intervals
					int pos = (int) (dep_t/3600);
					if(pos>23)
						pos-=24;
					
					//10 min intervals
//					int pos = (int) (dep_t/600);
//					if(pos>23*6+5)
//						pos-=24*6;
					
					distribution[pos]+=pw;					
				//}				
			}	
			
			
		}	
		
//		double sum=0.0;
//		LinkedList<Double> mov_sum = new LinkedList<Double>();
//		for(int j=0;j<6;j++){
//		mov_sum.addFirst(distribution[j]);
//		sum+=distribution[j];
//		}
//		out.write( "\t" + sum);	
//		
//		for(int j=6; j<distribution.length;j++){
//		
//			sum +=  distribution[j] - mov_sum.pollLast();
//			mov_sum.addFirst(distribution[j]);
//			
//			out.write( "\t" + sum);		
//		}
		
		for(int j=0; j<distribution.length;j++){
			
			out.write( "\t" + distribution[j]);		
		}
		
		System.out.println("TOTAL LEGS: " + counter);
		
	}

	

	private void printHoursSpentbyActivity(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("HOURS SPENT BY ACTIVITY"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("\t work  \teducation \tshopping \tbusiness \tleisure \tother \thome" ); 
		out.newLine();
		
		for(int i=cohorts_strings.length-1;i>=0;i--){	
			
			
			out.write(cohorts_strings[i]);
			ArrayList<Id> ids = ids_cohort[i];
			
			double[] time_activities = new double[7];
			
			double sum_weight = 0;

			for(Id id:ids){
				
//				if(!((PersonImpl)population.getPersons().get(id)).isEmployed())
//					continue
//					;
			
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				Plan plan = microcensus.getPopulation().getPersons().get(id).getSelectedPlan();
					
				if(plan!=null){
		
					for(PlanElement pe: plan.getPlanElements()){
						if(!(pe instanceof Activity))
							continue;
						Activity activity = (Activity)pe;
						//[0]=work , [1]=education, [2]=shopping, [3]=business, [4]=leisure, [5]=other, [6]=home
						int indexType = this.getIndexByActivityType(activity.getType());
						double startTime = activity.getStartTime();
						double endTime = activity.getEndTime();
						if(startTime== Time.UNDEFINED_TIME)
							startTime=0;
						if(endTime== Time.UNDEFINED_TIME)
							endTime= 24*3600;
						time_activities[indexType]+=  (endTime - startTime)*pw;

					}
				}else{
					time_activities[6] += 24*3600*pw;
				}
				
				
				sum_weight +=  pw*hhw;
				
			}	
			
			for(int j=0; j<time_activities.length;j++){
				out.write( "\t" + time_activities[j]/sum_weight/3600);		
			}
			
			out.newLine();
		}

		
			
		
		
	}


	private void printNumberOfActivitiesByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("NUMBER OF ACTIVITIES "); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("\t work  \teducation \tshopping \tbusiness \tleisure \tother \thome" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			double[] number_activities = new double[7];
			double sum_weight = 0;
			double total = 0;
			for(Id id:ids){
			
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				Plan plan = microcensus.getPopulation().getPersons().get(id).getSelectedPlan();
				
				int counter=0;
				if(plan!=null){
									
			
					for(PlanElement pe: plan.getPlanElements()){
						if(!(pe instanceof Activity))
							continue;
						Activity activity = (Activity)pe;
						
						if(activity.getType().equals(MZConstants.HOME))
							continue;
						
						//[0]=work , [1]=education, [2]=shopping, [3]=business, [4]=leisure, [5]=other, [6]=home
						int indexType = this.getIndexByActivityType(activity.getType());
						number_activities[indexType]+=  pw;
						
						counter++;
					}
				}
				total += counter*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
			//Per Activity Type
//			for(int j=0; j<number_activities.length;j++){
//				out.write( "\t" + number_activities[j]/sum_weight);		
//			}
//			out.newLine();
			
			//TOTALS
			out.write( "\t" + total/sum_weight);		
			out.newLine();
		}
			
		
	}

	private void printNumberOfActivitiesByCohortAndGender(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("NUMBER OF ACTIVITIES "); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("\t M  \t F" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double sum_weight=0;
			double total = 0;
			double[] number_activities = new double[7];
						
				for(Id id:gender){
					
					double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
					double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
					
					Plan plan = microcensus.getPopulation().getPersons().get(id).getSelectedPlan();
					
					int counter=0;
					if(plan!=null){
										
				
						for(PlanElement pe: plan.getPlanElements()){
							if(!(pe instanceof Activity))
								continue;
							Activity activity = (Activity)pe;
							
							if(activity.getType().equals(MZConstants.HOME))
								continue;
							
							//[0]=work , [1]=education, [2]=shopping, [3]=business, [4]=leisure, [5]=other, [6]=home
							int indexType = this.getIndexByActivityType(activity.getType());
							number_activities[indexType]+=  pw;
							
							counter++;
						}
					}
				total += counter*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
			
//				for(int j=0; j<number_activities.length;j++){
//					out.write( "\t" + number_activities[j]/sum_weight);		
//				}
//			out.newLine();
			out.write( "\t" + total/sum_weight);		
			
		
			}
			out.newLine();
		}
		
	}

	private void printNumberOfJourneysByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("NUMBER OF JOURNEYS (HOME-TO-HOME)"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("\t work  \teducation \tshopping \tbusiness \tleisure \tother \thome" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			double sum_weight = 0;
			double total = 0;
			double[] cum_sum = new double[7];
			for(Id id:ids){
			
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				//[0]=work , [1]=education, [2]=shopping, [3]=business, [4]=leisure, [5]=other, [6]=home
				double[] activities = new double[7];
				
				Plan plan = microcensus.getPopulation().getPersons().get(id).getSelectedPlan();
				
				int counter=0;
				if(plan!=null){
									
					boolean isFirst = true;
					
					for(PlanElement pe: plan.getPlanElements()){
						if(!(pe instanceof Activity))
							continue;
						Activity activity = (Activity)pe;
						if(!activity.getType().equals(MZConstants.HOME)){
							int index = getIndexByActivityType(activity.getType());
							activities[index] = 1;
							continue;
						}
						if(isFirst){
							isFirst=false;
							activities = new double[7];
							continue;
						}
						for(int j=0; j<activities.length;j++){
							cum_sum[j]+=pw*activities[j];	
						}
						counter++;
						//activities = new double[7];
					}
				}

				total += counter*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
			
//			for(int j=0; j<cum_sum.length;j++){
//				out.write( "\t" + cum_sum[j]/sum_weight);		
//			}
			out.write( "\t TOTAL \t " + total/sum_weight);		
			out.newLine();
		}
			
	}

	
	private void printHoursInActivitiesByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("HOURS AT ACTIVITIES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \t M \t F" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			double sum_weight = 0;
			double total = 0;
			for(Id id:ids){
			
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				Plan plan = microcensus.getPopulation().getPersons().get(id).getSelectedPlan();
				
				double time_activities=0;
				if(plan!=null){
								
					
					for(PlanElement pe: plan.getPlanElements()){
						if(!(pe instanceof Activity))
							continue;
						Activity activity = (Activity)pe;
						if(activity.getType().equals(MZConstants.HOME))
							continue;
						double startTime = activity.getStartTime();
						double endTime = activity.getEndTime();
						if (startTime == Time.UNDEFINED_TIME)
							startTime = 0;
						if(endTime == Time.UNDEFINED_TIME)
							endTime = 86400;		
						time_activities+=endTime-startTime;
						
					}
				
				}
				total += time_activities/3600*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
			out.write( "\t" + total/sum_weight);		
			out.newLine();
		}
		
		
	}
	
	
	private void printHoursInAndOutOfHomeByCohort(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("HOURS IN AND OUT OF HOME"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			double sum_weight = 0;
			double total = 0;
			for(Id id:ids){
			
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				Plan plan = microcensus.getPopulation().getPersons().get(id).getSelectedPlan();
				
				double time_home=24*3600;
				if(plan!=null){
					
					time_home=0;
					
					for(PlanElement pe: plan.getPlanElements()){
						if(!(pe instanceof Activity))
							continue;
						Activity activity = (Activity)pe;
						if(!activity.getType().equals(MZConstants.HOME))
							continue;
						double startTime = activity.getStartTime();
						double endTime = activity.getEndTime();
						if (startTime == Time.UNDEFINED_TIME)
							startTime = 0;
						if(endTime == Time.UNDEFINED_TIME)
							endTime = 86400;		
						time_home+=endTime-startTime;
						
					}
				
				}
				total += time_home/3600*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
			out.write( "\t" + total/sum_weight);		
			out.newLine();
		}
		
		
	}

	private void printOutOfHomeTripsActivitiesRatioByCohortAndGender(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND GENDER"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tShare M  \tShare F" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
		
	}
	

	private void printOccupancyRateData() throws Exception  {
		out.write("------------------------------"); 
		out.newLine();
		out.write("OCCUPANCY RATE BY ACTIVITY PURPOSE"); 
		out.newLine();
		out.write("------------------------------"); 
	
		
		TreeMap<String, ArrayList<Etappe>> etappes = EtappenLoader.loadData(this.microcensus.getYear());
		
		//[0]=leisure
		//[1]=other/services
		//[2]=shopping
		//[3]=all
		//[4]=business
		//[5]=work
		
		double[] counter = new double[7];
		double[] totals = new double[7];
		double[] driver = new double[7];
		double[] passenger = new double[7];
		double[] cumulative =new double[7];

 			for(Person person:microcensus.getPopulation().getPersons().values()){
				
				
				if(etappes.containsKey(person.getId().toString())){
					ArrayList<Etappe> etappen = etappes.get(person.getId().toString());
					
					for(Etappe etappe: etappen){
						
						if(etappe.getMode().equals(MZConstants.CAR_FAHRER)||etappe.getMode().equals(MZConstants.CAR_MITFAHRER)){
							int index = getEtappenIndexByPurpose(etappe);
							if(index!=-1){
							double weight = Double.parseDouble(etappe.getWeight());
							
								if(!etappe.getTotalPeople().equals(MZConstants.NO_ANSWER)){
									double total_people = Double.parseDouble(etappe.getTotalPeople());
									//method 1: Occupancy =  Mean of persons in the car/car trips
									cumulative[index]+= weight*total_people;
									totals[index]+= weight;
																
									cumulative[3]+= weight*total_people;
									totals[3]+= weight;
								}
							
								//method 2: Occupancy = (number of trips as car driving + trips as a car passenger ) / number of trips as car driver
							    if(etappe.getMode().equals(MZConstants.CAR_FAHRER)){
							    	
							    	driver[index]+=weight;
							    	driver[3]+=weight;
							    	
							    }else if(etappe.getMode().equals(MZConstants.CAR_MITFAHRER)){
							    	
							    	passenger[index]+=weight;
							    	passenger[3]+=weight;
							    }
							}
						}
					}
				}	
			}
			
	//print results
 		System.out.println("Method1:" );	
			for(int i=0; i<=totals.length-1;i++){
				
				//System.out.println(totals[i]);
				System.out.println(cumulative[i]/totals[i]);
				out.write("" + cumulative[i]/totals[i] );
				out.newLine();
			}
			
		System.out.println("Method2:" );
			
			for(int i=0; i<=totals.length-1;i++){
				
				//System.out.println(totals[i]);
				System.out.println((driver[i]+passenger[i])/driver[i]);
				out.write("" + (driver[i]+passenger[i]/driver[i]) );
				out.newLine();
			}
			
			
			
	}

	private int getIndexByActivityType(String type) {
		
			//[0]=work , [1]=education, [2]=shopping, [3]=business, [4]=leisure, [5]=other, [6]=home
		
		if(type.contains(MZConstants.WORK)){
			return 0;}
		
		else if(type.contains(MZConstants.EDUCATION))
			return 1;
		else if(type.contains(MZConstants.SHOPPING))
			return 2;
		
		else if(type.contains(MZConstants.BUSINESS)
				||type.contains(MZConstants.DIENSTFAHRT)) 
			return 3;
		
		else if(type.contains(MZConstants.LEISURE)) 
			return 4;
		
		else if(type.contains(MZConstants.ACCOMPANYING_CHILDREN)
				|| type.contains(MZConstants.ACCOMPANYING_NOT_CHILDREN)
				|| type.contains(MZConstants.ACCOMPANYING)
				|| type.contains(MZConstants.ERRANDS)
				|| type.contains(MZConstants.OTHER)
				|| type.contains(MZConstants.FOREIGN_PROPERTY)
				|| type.contains(MZConstants.OVERNIGHT)
				|| type.contains(MZConstants.PSEUDOETAPPE)
				|| type.contains(MZConstants.CHANGE)
				|| type.contains(MZConstants.NO_ANSWER))
				
			return 5;
		else if(type.contains(MZConstants.HOME))
			return 6;
		else{
			throw new RuntimeException("No type known for: " + type);
		}
	
	}
	
	private int getEtappenIndexByPurpose(Etappe etappe) {
		//[0]=leisure
		//[1]=other/services
		//[2]=shopping
		//[3]=all
		//[4]=business
		//[5]=work
		String purpose = etappe.getPurpose();
		
		//[0]=leisure
		if(purpose.equals(MZConstants.LEISURE)){
			return 0;
		
		//[1]=other/services
		}else if(purpose.equals(MZConstants.CHANGE)
				||purpose.equals(MZConstants.ACCOMPANYING_CHILDREN)
				||purpose.equals(MZConstants.ERRANDS)
				|| purpose.equals(MZConstants.ACCOMPANYING_NOT_CHILDREN)
				|| purpose.equals(MZConstants.FOREIGN_PROPERTY)	
				|| purpose.equals(MZConstants.OTHER)
				|| purpose.equals(MZConstants.CHANGE)
				|| purpose.equals(MZConstants.BORDER_CROSSING)
				//||purpose.equals(MZConstants.PSEUDOETAPPE)
				//purpose.equals(MZConstants.NO_ANSWER)
				){
				return 1;
		//[2]=shopping
		}else if(purpose.equals(MZConstants.SHOPPING)){
			return 2;
			
		//[4]=business	
		}else if(purpose.equals(MZConstants.BUSINESS)
				||purpose.equals(MZConstants.DIENSTFAHRT)){
			return 4;
		
		//[5]=work
		}else if(purpose.equals(MZConstants.WORK)
				|| purpose.equals(MZConstants.EDUCATION)
			
			){
				return 5;
		//[6]=education		
		}else if(purpose.equals(MZConstants.EDUCATION)

			){
				return 6;		
		}else{
			//Gbl.errorMsg("No puporse known for: " + purpose);
			return -1;
		}
		
	}


	private void printMotorizedVsPTTrips(ArrayList<Id>[] ids_PW_season) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("ABONNEMENT DIST. BY PW OWNERSHIP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("GROUP \tTrips Motorized  \tTrips PT" ); 
		out.newLine();
	
		
		String[] group_strings = {"Vehicle and season ticket", "No vehicle, but season ticket", "Vehicle, but no season ticket", "Neither"};
		
		for(int i=0;i<ids_PW_season.length;i++){
			
			out.write(group_strings[i]);
	
			ArrayList<Id> ids = ids_PW_season[i];
			
			double counter_motorized = 0;
			double counter_PT = 0;
			double sum_weight=0;
			
			for(Id id:ids){
	
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				Person person = microcensus.getPopulation().getPersons().get(id);
				
				if(person.getSelectedPlan()!=null){
						
					for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
						
						if(pe instanceof Leg){
							Leg leg = (Leg) pe;
							if(isMotorized(leg)){
								counter_motorized+=pw;
							}else if(isPT(leg)){
								counter_PT+=pw;
							}
							
						}
						
					}
					
				}
				
 
				
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter_motorized/sum_weight + "\t" + counter_PT/sum_weight);
			
			
			
			out.newLine();
		}
		
	}


	private boolean isMotorized(Leg leg) {
		String mode = leg.getMode();
		
		if(mode.equals(MZConstants.CAR) | mode.equals(MZConstants.MOTORCYCLE) | mode.equals(MZConstants.MOFA)){
			return true;
		}
		
		
		return false;
	}
	
	private boolean isPT(Leg leg) {
		String mode = leg.getMode();
		
		if(mode.equals(MZConstants.TRAIN) | mode.equals(MZConstants.POSTAUTO) | mode.equals(MZConstants.SHIP)
		  |mode.equals(MZConstants.TRAM) | mode.equals(MZConstants.BUS) | mode.equals(MZConstants.SONSTINGER_OEV)
		  | mode.equals(MZConstants.TAXI)){
			return true;
		}
		
		
		return false;
	}


	private void printAbonnementsByPWOwnership(ArrayList<Id>[] ids_PW) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("ABONNEMENT DIST. BY PW OWNERSHIP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("PW  \tGA  \tHalbTax \tHalbTax+Verbund  \t Verbund \t No Abo" ); 
		out.newLine();
	
		
		String[] ids_PW_strings = {"Yes", "No"};
		
		for(int i=0;i<ids_PW.length;i++){
	
			out.write(ids_PW_strings[i]);
			
			ArrayList<Id> ids = ids_PW[i];
			
			double[] abonnement_groups = new double[5];
			//0: GA
			//1: HalbTax
			//2: Halbtax + Verbund
			//3: Verbund
			//4: No abonnement
					
			double counter = 0;
			double sum_weight=0;
			
			for(Id id:ids){
	
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES)
						 |((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES) ){
					
						abonnement_groups[0] += pw*hhw;
				}else if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)){
					if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES) ){
						
						abonnement_groups[2] += pw*hhw;	
					}else{
						abonnement_groups[1] += pw*hhw;
					}
				}else if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES) ){
						abonnement_groups[3] += pw*hhw;
				}else{
						abonnement_groups[4] += pw*hhw;
				}
 
				
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + abonnement_groups[0]/sum_weight*100 + "\t" + abonnement_groups[1]/sum_weight*100 + "\t" + abonnement_groups[2]/sum_weight*100 + "\t" + abonnement_groups[3]/sum_weight*100 + "\t" + abonnement_groups[4]/sum_weight*100);
			
			
			
			out.newLine();
		}
		
	}


	private void printCarAvailavilityByAgeGroup(ArrayList<Id>[] ids_age_groups) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("CAR AVAILABILITY BY AGE GROUP FOR LICENSE HOLDERS"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tShare M  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<age_group_strings.length;i++){
	
			out.write(age_group_strings[i]);
			
			ArrayList<Id> ids = ids_age_groups[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
					
					sum_weight +=  pw*hhw;
					
					if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS)){
					
						counter+=  pw*hhw;
					}
				
				}
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
	}

	
	
	
	private void printDrivingLicenseByAgeGroup(ArrayList<Id>[] ids_age_groups) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE BY AGE GROUP"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tShare M  \tShare F" ); 
		out.newLine();
		
		
		for(int i=0;i<age_group_strings.length;i++){
	
			out.write(age_group_strings[i]);
			
			ArrayList<Id> ids = ids_age_groups[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
		
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
				
				Person person = microcensus.getPopulation().getPersons().get(id);
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1; //Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				total = total + ((PersonImpl)person).getAge()*pw*hhw;
				sum_weight +=  pw*hhw;
		
				
			}
			out.write(cohorts_strings[i] + "\t" + total/sum_weight);
			out.newLine();
		}
		
		
	}


	private void printTitle() throws IOException {
		out.write("\t\t\t  Moblity tool ownership and use, MZ" +this.microcensus.getYear());
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
						
						double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
						total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DURATION))*pw*hhw;
						sum_weight +=  pw*hhw;
											
					}
								
					out.write( "\t" + total/sum_weight);
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
						
						double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
						total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE))*pw*hhw;
						sum_weight +=  pw*hhw;
						
						
					}
								
					out.write( "\t" + total/sum_weight);
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
						
						double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
						total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
						sum_weight +=  pw*hhw;

						
					}
								
					out.write( "\t" + total/sum_weight );
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
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				total += Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DURATION))*pw*hhw;
				sum_weight +=  pw*hhw;
				
				
			}
						
			out.write( "\t" + total/sum_weight );
			
			}
			
			out.newLine();
		}
		
	}

	
	private void printDailyTripsDurationByCohortAndIncomeTercile(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DAILY TRIPS DURATION BY COHORT AND INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t First Tercile  \t Second Tercile \t Third Tercile" ); 
		out.newLine();
		
		
		TreeMap<String, ArrayList<Wege>> all_weges= WegeLoader.loadData(microcensus.getYear());
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(ids);
			 
			for(ArrayList<Id> tercile: terciles){ 
			
			double total = 0;
			double sum_weight=0;
			for(Id id:tercile){
				
				
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
//				total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DURATION))*pw*hhw;
				
				ArrayList<Wege> weges = all_weges.get(id.toString());
							
				if(weges!=null){
					for(Wege wege:weges){
						total+= Double.parseDouble(wege.getDuration())*pw;
					}										
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + total/sum_weight);
			
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
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE))*pw*hhw;
				sum_weight +=  pw*hhw;
				
			
			}
						
			out.write( "\t" + total/sum_weight );
			
			}
			
			out.newLine();
		}
		
	}
	
	private void printDailyDistancesByCohortAndIncomeTercile(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DAILY DISTANCES BY COHORT AND INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t First Tercile  \t Second Tercile \t Third Tercile" ); 
		out.newLine();
		
		
		TreeMap<String, ArrayList<Wege>> all_weges= WegeLoader.loadData(microcensus.getYear());
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(ids);
			 
			for(ArrayList<Id> tercile: terciles){ 
			
			double total = 0;
			double sum_weight=0;
			for(Id id:tercile){
				
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
//				total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE))*pw*hhw;
				
				ArrayList<Wege> weges = all_weges.get(id.toString());
							
				if(weges!=null){
					for(Wege wege:weges){
						total+= Double.parseDouble(wege.getDistance())*pw;
					}										
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + total/sum_weight);
			
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
			
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
						
			out.write( "\t" + total/sum_weight);
			
			}
			
			out.newLine();
		}
		
		
	}
	
	private void printAverageTripsByCohortAndIncomeTercile(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE TRIPS INLAND BY COHORT AND INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t First Tercile  \t Second Tercile \t Third Tercile" ); 
		out.newLine();
		
		
		TreeMap<String, ArrayList<Wege>> all_weges= WegeLoader.loadData(microcensus.getYear());
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(ids);
			 
			for(ArrayList<Id> tercile: terciles){ 
			
			double total = 0;
			double sum_weight=0;
			for(Id id:tercile){
				
				
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
//				total = total + Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
				
				ArrayList<Wege> weges = all_weges.get(id.toString());
				if(weges!=null){
					total+=weges.size()*pw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + total/sum_weight);
			
			}
			
			out.newLine();
		}
		
		
	}
	
	
	private void printDailyDistanceByCohortAndPWAvailability(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE DAILY DISTANCE"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \t ALWAYS \t BY ARRANGEMENT \t NEVER" ); 
		out.newLine();
		
		if(this.microcensus.getYear()==2000){
		
			TreeMap<String, ArrayList<Etappe>> etappes = EtappenLoader.loadData(this.microcensus.getYear());
			
			for(int i=cohorts_strings.length-1;i>=0;i--){
				
				out.write(cohorts_strings[i]);
				System.out.println(cohorts_strings[i]);
				
				ArrayList<Id> ids = ids_cohort[i];
				
				 ArrayList<Id>[] groups = this.getIdsByPWOwnership_3Groups(ids);
				 
				for(ArrayList<Id> group: groups){ 
			
				double sum_weight = 0;
				double total = 0;
				for(Id id:group){
	
					ArrayList<Etappe> etappen = etappes.get(id.toString());
					
					double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
					double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
					
					Person person = microcensus.getPopulation().getPersons().get(id);
					
					double dist=0;
					if(etappen!=null){ //avoid people without plan or undefined etappes
						
						for(Etappe etappe:etappen){
							
							dist+= Double.parseDouble(etappe.getDistance());
							
						}
					}					
					total +=  dist*pw*hhw;
					sum_weight +=  pw*hhw;
				}	
				out.write( "\t" + total/sum_weight);
				}
				out.newLine();
			}
		
		}else{
			
			for(int i=cohorts_strings.length-1;i>=0;i--){
				
				out.write(cohorts_strings[i]);
				System.out.println(cohorts_strings[i]);
				
				ArrayList<Id> ids = ids_cohort[i];
				
				 ArrayList<Id>[] groups = this.getIdsByPWOwnership_3Groups(ids);
				 
				for(ArrayList<Id> group: groups){ 
			
				double sum_weight = 0;
				double total = 0;
				for(Id id:group){
	
					double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
					double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
					double dist= Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE));
				
					total +=  dist*pw*hhw;
					sum_weight +=  pw*hhw;
				}	
				out.write( "\t" + total/sum_weight);
				}
				out.newLine();			
		}
		
		}
		
		
	}
	
	private void printAverageTripsByCohortAndPWAvailability(ArrayList<Id>[] ids_cohort) throws Exception {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AVERAGE TRIPS PER DAY"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort   \t ALWAYS \t BY ARRANGEMENT \t NEVER" ); 
		out.newLine();
		
		if(this.microcensus.getYear()==2000){			
			TreeMap<String, ArrayList<Etappe>> etappes = EtappenLoader.loadData(this.microcensus.getYear());
			
		
			for(int i=cohorts_strings.length-1;i>=0;i--){
				
				out.write(cohorts_strings[i]);
				
				ArrayList<Id> ids = ids_cohort[i];
				
				 ArrayList<Id>[] groups = this.getIdsByPWOwnership_3Groups(ids);
				 
				for(ArrayList<Id> group: groups){ 
				
				double sum_weight = 0;
				double total = 0;
				for(Id id:group){
	
					ArrayList<Etappe> etappen = etappes.get(id.toString());
					
					double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
					double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
					
					
					int nr_wege=0;
					if(etappen!=null){ //avoid people without plan or undefined etappes
						
						for(Etappe etappe:etappen){
							if(etappe.getWegeNr()>nr_wege){
								nr_wege = etappe.getWegeNr();
							}						
						}					
					}				
					total +=  nr_wege*pw*hhw;
					sum_weight +=  pw*hhw;				
				}							
				out.write( "\t" + total/sum_weight);			
				}			
				out.newLine();
			}
		}else{
			
			for(int i=cohorts_strings.length-1;i>=0;i--){
				
				out.write(cohorts_strings[i]);
				System.out.println(cohorts_strings[i]);
				
				ArrayList<Id> ids = ids_cohort[i];
				
				 ArrayList<Id>[] groups = this.getIdsByPWOwnership_3Groups(ids);
				 
				for(ArrayList<Id> group: groups){ 
			
				double sum_weight = 0;
				double total = 0;
				for(Id id:group){
	
					double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
					double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
					double nr_wege= Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND));
				
					total +=  nr_wege*pw*hhw;
					sum_weight +=  pw*hhw;
				}	
				out.write( "\t" + total/sum_weight);
				}
				out.newLine();			
		}
		
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
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES) ){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
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
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES)
						 |((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES) ){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
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
				
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
			
	
			}
						
			out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
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
				
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES)	
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES)
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES)
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
			
	
			}

					
			out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
			}
			
			out.newLine();
		}
		
		
	}
	
	private void printAbonnementOwnershipByCohortAndIncomeTercile(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("AT LEAST ONE ABONNEMENT OWNERSHIP BY COHORT AND INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t First Tercile  \t Second Tercile \t Third Tercile" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(ids);
			 
			for(ArrayList<Id> tercile: terciles){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:tercile){
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
						
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES)	
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES)
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES)
						|((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
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
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
			
	
			}
				
			out.write("\t" +counter/sum_weight*100 );
			
			//out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
			}
			
			out.newLine();
		}
		
	}
	
	private void printCarAvailabilityByCohortAndIncomeTercile(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("CAR AVAILABILITY PER COHORT AND INCOME TERCILES"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t First Tercile  \t Second Tercile \t Third Tercile" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] terciles = getIncomeTercilesIdsForCohort(ids);
			 
			for(ArrayList<Id> tercile: terciles){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:tercile){
				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
		
	}
	
	
	
	private void printDriverLicenseByCohortAndPW_GA(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND PW_GA"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t PW Available \t NO PW, but GA/season ticket \t rest" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] groups = this.getIdsByPW_GA(ids);
			 
			for(ArrayList<Id> group: groups){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:group){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
	}
	
	private void printDriverLicenseByCohortAndResidence(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND RESIDENCE"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t Zurich \t Basel \t Bern \t Geneva \t Other" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] residences = this.getIdsByResidence(ids);
			 
			for(ArrayList<Id> residence: residences){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:residence){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
	}
	
	private void printDriverLicenseByCohortAndResidenceBigCities(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND RESIDENCE"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \t Big cities \t Rest" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] residences = this.getIdsByResidenceBigCities(ids);
			 
			for(ArrayList<Id> residence: residences){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:residence){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
	}
		
	private void printDriverLicenseByCohortAndSeasonTicket(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND GA-SEASON TICKET"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tGA-SEASON TICKET  \t NO GA-SEASON TICKET" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] groups = this.getIdsByGA_SeasonTicket(ids);
			 
			for(ArrayList<Id> group: groups){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:group){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
		
	}

	private void printDriverLicenseByCohortAndGender(ArrayList<Id>[] ids_cohort) throws IOException {
		out.write("------------------------------"); 
		out.newLine();
		out.write("DRIVING LICENSE PER COHORT AND GENDER"); 
		out.newLine();
		out.write("------------------------------"); 
		out.newLine();
		out.write("Cohort  \tShare M  \tShare F" ); 
		out.newLine();
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){
			
			out.write(cohorts_strings[i]);
			
			ArrayList<Id> ids = ids_cohort[i];
			
			 ArrayList<Id>[] genders = getGenderIdsForCohort(ids);
			 
			for(ArrayList<Id> gender: genders){ 
			
			double counter = 0;
			double sum_weight=0;
			for(Id id:gender){
				

				
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER), "weight"));
				
				if(((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
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
		
		
		double sum_weight=0;
		double[] cum_sum = new double[cohorts_strings.length];
		
		for(int i=cohorts_strings.length-1;i>=0;i--){	
			
			ArrayList<Id> ids = ids_cohort[i];
						
			for(Id id:ids){
				double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.PERSON_WEIGHT));
				cum_sum[i]+=pw;
				sum_weight +=pw;
			}
		}
		
		
		for(int i=cohorts_strings.length-1;i>=0;i--){	
			out.write(cohorts_strings[i] + "\t" + ids_cohort[i].size() + "\t" + (float)((cum_sum[i]*100)/sum_weight)+ "%");
			out.newLine();
		}
		
		
		
	}


	public void filterPopulationOver18() throws IOException{
		
		ArrayList<Id>[] genders = this.getGenderIdsForCohort(new ArrayList<Id>(microcensus.getPopulation().getPersons().keySet()));
		out.write("Total male: \t" + genders[0].size() + "\t" + (float)genders[0].size()*100/microcensus.getPopulation().getPersons().size() );
		out.newLine();
		out.write("Total female: \t" + genders[1].size() + "\t" + (float)genders[1].size()*100/microcensus.getPopulation().getPersons().size() );
		out.newLine();
		out.newLine();
		
	
		Set<Id> ids_to_remove = new HashSet<Id>();
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			if(((PersonImpl)person).getAge() < 18){
				ids_to_remove.add(person.getId());
					
			}
			
		}
		
		microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total population over 18: \t\t" + microcensus.getPopulation().getPersons().size());
		out.newLine();
		genders = this.getGenderIdsForCohort(new ArrayList<Id>(microcensus.getPopulation().getPersons().keySet()));
		out.write("Total male: \t" + genders[0].size() + "\t" + (float)genders[0].size()*100/microcensus.getPopulation().getPersons().size() );
		out.newLine();
		out.write("Total female: \t" + genders[1].size() + "\t" + (float)genders[1].size()*100/microcensus.getPopulation().getPersons().size() );
		out.newLine();
		out.newLine();
		
		
	}
	
	public void filterPopulationWeekday() throws IOException{
		
		
		Set<Id> ids_to_remove = new HashSet<Id>();
		
		
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			String day = (String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.DAY_OF_WEEK);
			
			if(day.equals(MZConstants.SATURDAY) || day.equals(MZConstants.SUNDAY)){
				ids_to_remove.add(person.getId());					
			}
			
		}
		
		microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total weekday: \t\t" + microcensus.getPopulation().getPersons().size());
		out.newLine();
		
		new PopulationWriter(microcensus.getPopulation(), null).write("C:/local/marmolea/output/Activity Chains Forecast/population_weekday_employed.xml");
	}
	
	public void filterByGender(String gender_to_remove) throws IOException{
		
		
		Set<Id> ids_to_remove = new HashSet<Id>();
				
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			String gender = (String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.GENDER);
			
			if(gender.equals(gender_to_remove)){
				ids_to_remove.add(person.getId());					
			}
			
		}
		
		microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total after filtering all gender " + gender_to_remove + ": \t\t" + microcensus.getPopulation().getPersons().size());
		out.newLine();
		
		new PopulationWriter(microcensus.getPopulation(), null).write("C:/local/marmolea/output/Activity Chains Forecast/population_weekday_employed.xml");
	}
	
	public void filterByAgeRange(int fromAge, int toAge) throws IOException{
			
			
			Set<Id> ids_to_remove = new HashSet<Id>();
					
			
			for(Person person: microcensus.getPopulation().getPersons().values()){
				
				int age = Integer.parseInt((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.AGE));
				
				if(age<fromAge || age>toAge){
					ids_to_remove.add(person.getId());					
				}
				
			}
			
			microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
			
			out.write("Total after filtering all out of age range  " +fromAge +"-" +toAge+ ": \t\t" + microcensus.getPopulation().getPersons().size());
			out.newLine();
			
			new PopulationWriter(microcensus.getPopulation(), null).write("C:/local/marmolea/output/Activity Chains Forecast/population_weekday_employed.xml");
		}
	
	public void filterByIncome(int min_income) throws IOException{
		
		
		Set<Id> ids_to_remove = new HashSet<Id>();
				
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			String hhnr = (String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.HOUSEHOLD_NUMBER);
			int income = Integer.parseInt((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME_MIDDLE_OF_INTERVAL));
			
			if(income<min_income){
				ids_to_remove.add(person.getId());					
			}
			
		}
		
		microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total after filtering all out with hh income <  " +min_income+ ": \t\t" + microcensus.getPopulation().getPersons().size());
		out.newLine();
		
		new PopulationWriter(microcensus.getPopulation(), null).write("C:/local/marmolea/output/Activity Chains Forecast/population_weekday_employed.xml");
	}
	
	public void filterPopulationWithWorkActivity() throws IOException{
		
		
		Set<Id> ids_to_remove = new HashSet<Id>();
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			Plan plan = person.getSelectedPlan();
			
			if(plan==null){
				ids_to_remove.add(person.getId());
				continue;
			}
			
			boolean remove= true;
			for(PlanElement pe : plan.getPlanElements()){
				
				if(!(pe instanceof Activity))
					continue;
				Activity activity = (Activity)pe;
				
				if(activity.getType().equals(MZConstants.WORK))
					remove= false;
				
			}
			
			if(remove){
				ids_to_remove.add(person.getId());					
			}
			
		}
		
		microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total population with work activity: \t\t" + microcensus.getPopulation().getPersons().size());
		out.newLine();
	}
	
	
	public void filterPopulationEmployed() throws IOException{
		
	
		Set<Id> ids_to_remove = new HashSet<Id>();
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			if(!((PersonImpl)person).isEmployed()){
				ids_to_remove.add(person.getId());					
			}
			
		}
		
		microcensus.getPopulation().getPersons().keySet().removeAll(ids_to_remove);
		
		out.write("Total population employed: \t\t" + microcensus.getPopulation().getPersons().size());
		out.newLine();
	}
	
	private ArrayList<Id>[] getIdsByCohort() throws IOException {
		
		ArrayList<Id>[] ids_cohort = new ArrayList[this.cohorts.length];
		int[] max_age = new int[this.cohorts.length];
		
		for(int i=0;i<this.cohorts.length;i++){
			ids_cohort[i] = new ArrayList<Id>();
			max_age[i] = this.microcensus.getYear()-this.cohorts[i];
		}
		max_age[this.cohorts.length-1] = Integer.MAX_VALUE;
				
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			for(int i=0;i<this.cohorts.length;i++){
				if(((PersonImpl)person).getAge() <= max_age[i]){
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
			
			if(((PersonImpl)microcensus.getPopulation().getPersons().get(id)).getSex().equals("m")){
				ids_gender[0].add(id);
			}else if(((PersonImpl)microcensus.getPopulation().getPersons().get(id)).getSex().equals("f")){
				ids_gender[1].add(id);
			}else{throw new RuntimeException("This should neber happen!: Gender "+ ((PersonImpl)microcensus.getPopulation().getPersons().get(id)).getSex() +" unknown!");}
			
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
			
			boolean hasAbonnement = (((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES) 
					| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES)
					| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)
					| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES));
			
			boolean hasCarAvailable = ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS);
					
					
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

	
	private ArrayList<Id>[] getIdsByAgeGroup() throws IOException {
		

		ArrayList<Id>[] ids_age_groups = new ArrayList[this.age_groups.length];
		for(int i=0;i<this.age_groups.length;i++){
			ids_age_groups[i] = new ArrayList<Id>();
		}
		
		
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			for(int i=0;i<this.cohorts.length;i++){
				if(((PersonImpl)person).getAge() < this.age_groups[i]){
					ids_age_groups[i].add(person.getId());
					break;
				}
			}			
		}
		return ids_age_groups;
	}
	
	private ArrayList<Id>[] getIdsByPWOwnership() throws IOException {
		
		//PW[0] = YES
		//PW[1] = NO
		
		ArrayList<Id>[] ids_PW = new ArrayList[2];
		for(int i=0;i<ids_PW.length;i++){
			ids_PW[i] = new ArrayList<Id>();
		}
		
				for(Person person:microcensus.getPopulation().getPersons().values()){
			
				boolean hasCarAvailable = ((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS);
				
				if(hasCarAvailable){
					ids_PW[0].add(person.getId());
				}else{
					ids_PW[1].add(person.getId());
				}
		}
		return ids_PW;
	}
	
	private ArrayList<Id>[] getIdsByPWOwnership_3Groups(ArrayList<Id> ids) throws IOException {
		
		//PW[0] = ALWAYS
		//PW[1] = BY ARRANGEMENT
		//PW[2] = NEVER
		// keine Antwort, Keine Angabe, F40200a ungleich 1 are disregarded, ~30%
		
		ArrayList<Id>[] ids_PW = new ArrayList[3];
		for(int i=0;i<ids_PW.length;i++){
			ids_PW[i] = new ArrayList<Id>();
		}
		
			for(Id id:ids){
					
				String av = (String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.CAR_AVAILABILITY);
				if(av.equals(MZConstants.ALWAYS)){
					ids_PW[0].add(id);
				}else if(av.equals(MZConstants.ARRANGEMENT)){
					ids_PW[1].add(id);
				}else if(av.equals(MZConstants.NEVER)){
					ids_PW[2].add(id);
				}
			}
		return ids_PW;
	}	
	
	
	private ArrayList<Id>[] getIdsByPWAndSeasonTicket() throws IOException {
		
		//PW[0] = vehicle and season ticket
		//PW[1] = no vehicle but season ticket
		//PW[2] = vehicle but no season ticket
		//PW[3] = neither
		
		ArrayList<Id>[] ids_PW_Verbund = new ArrayList[4];
		for(int i=0;i<ids_PW_Verbund.length;i++){
			ids_PW_Verbund[i] = new ArrayList<Id>();
		}
		
		int counter0 =0, counter1=0,counter2=0,counter3=0;
		
				for(Person person:microcensus.getPopulation().getPersons().values()){
			
				boolean hasCarAvailable = ((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS);
				
				boolean hasSeasonTicket =  ((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES) |
										   ((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES) |
										   ((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES)|
										   ((String)microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES);
				
				if(hasCarAvailable & hasSeasonTicket){
					ids_PW_Verbund[0].add(person.getId()); counter0++;
				}else if(!hasCarAvailable & hasSeasonTicket){
					ids_PW_Verbund[1].add(person.getId());counter1++;
				}else if(hasCarAvailable & !hasSeasonTicket){
					ids_PW_Verbund[2].add(person.getId());counter2++;
				}else if(!hasCarAvailable & !hasSeasonTicket){
					ids_PW_Verbund[3].add(person.getId());counter3++;
				}
				
		}
				
		return ids_PW_Verbund;
	}

private ArrayList<Id>[] getIdsByResidence(ArrayList<Id> ids) throws IOException {
	
	//[0] = zurich
	//[1] = basel
	//[2] = bern
	//[3] = geneva
	//[4] = rest
	
	ArrayList<Id>[] ids_residence = new ArrayList[5];
	for(int i=0;i<ids_residence.length;i++){
		ids_residence[i] = new ArrayList<Id>();
	}
	
	
			for(Id id:ids){
		
			String hhnr =	(String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER);
				
			boolean zurich = ((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("261");
			boolean basel = ((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("2701");
			boolean bern = ((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("351");
			boolean geneva = ((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("6621");
			
			if(zurich){
				ids_residence[0].add(id); 
			}else if(basel){
				ids_residence[1].add(id);
			}else if(bern){
				ids_residence[2].add(id);
			}else if(geneva){
				ids_residence[3].add(id);
			}else{
				ids_residence[4].add(id);
			}
			
	}
			
	return ids_residence;
}

private ArrayList<Id>[] getIdsByResidenceBigCities(ArrayList<Id> ids) throws IOException {
	
	//[0] = big city
	//[1] = rest
	
	ArrayList<Id>[] ids_residence = new ArrayList[2];
	for(int i=0;i<ids_residence.length;i++){
		ids_residence[i] = new ArrayList<Id>();
	}
	
	
			for(Id id:ids){
		
			String hhnr =	(String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.HOUSEHOLD_NUMBER);
				
			boolean zurich = ((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("261");
			boolean basel = ((String)this.microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("2701");
			boolean bern = ((String)microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("351");
			boolean geneva = ((String)microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.MUNICIPALITY)).equals("6621");
			
			if(zurich|basel|bern|geneva){
				ids_residence[0].add(id); 
			}else{
				ids_residence[1].add(id);
			}
			
	}
			
	return ids_residence;
}


private ArrayList<Id>[] getIdsByGA_SeasonTicket(ArrayList<Id> ids) throws IOException {
	
	//[0] = yes
	//[1] = no
	
	ArrayList<Id>[] ids_ticket = new ArrayList[2];
	for(int i=0;i<ids_ticket.length;i++){
		ids_ticket[i] = new ArrayList<Id>();
	}
	
	
			for(Id id:ids){
		
			boolean hasAbonnement = (((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES) 
						| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES)
						| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)
						| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES));
			
			if(hasAbonnement){
				ids_ticket[0].add(id); 
			}else{
				ids_ticket[1].add(id);
			}
			
	}
			
	return ids_ticket;
}

private ArrayList<Id>[] getIdsByPW_GA(ArrayList<Id> ids) throws IOException {
	
	//[0] = has car available
	//[1] = no car available, but GA/season ticket
	//[2] = rest
	
	ArrayList<Id>[] ids_ticket = new ArrayList[3];
	for(int i=0;i<ids_ticket.length;i++){
		ids_ticket[i] = new ArrayList<Id>();
	}
	
	
			for(Id id:ids){
		
			boolean hasCarAvailable = ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.CAR_AVAILABILITY)).equals(MZConstants.ALWAYS);	
				
			boolean hasAbonnement = (((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA1)).equals(MZConstants.YES) 
						| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_GA2)).equals(MZConstants.YES)
						| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_HT)).equals(MZConstants.YES)
						| ((String)microcensus.getPopulationAttributes().getAttribute(id.toString(), MZConstants.ABBO_VERBUND)).equals(MZConstants.YES));
			
			if(hasCarAvailable){
				ids_ticket[0].add(id); 
			}else if(hasAbonnement){
				ids_ticket[1].add(id);
			}else{
				ids_ticket[2].add(id);
			}
			
	}
			
	return ids_ticket;
}


	private void printPopulationSize(String comment) throws IOException{

	out.write( comment + ": \t\t" + microcensus.getPopulation().getPersons().size());
	out.newLine();

	}

}
