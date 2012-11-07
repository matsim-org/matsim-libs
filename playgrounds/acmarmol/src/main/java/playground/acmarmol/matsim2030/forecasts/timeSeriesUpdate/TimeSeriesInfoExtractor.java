package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
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

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.ocupancyRate.Etappe;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.ocupancyRate.EtappenLoader;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;

public class TimeSeriesInfoExtractor {
	
	
	private Population population;
	private ObjectAttributes populationAttributes;
	private ObjectAttributes householdAttributes;
	private int year;
	private int[] cohorts;
	private String[] cohorts_strings;
	private int[] age_groups;
	private String[] age_group_strings;
	final  Logger log = Logger.getLogger(TimeSeriesInfoExtractor.class);
	private BufferedWriter out;
	
	public TimeSeriesInfoExtractor(String populationInputFile, String populationAttributesInputFile, String householdAttributesInputFile, int year, int[] cohorts, String[] cohorts_strings, int[] age_groups, String[] age_group_strings){
		this.year = year;
		this.cohorts = cohorts;
		this.cohorts_strings = cohorts_strings;
		this.age_group_strings = age_group_strings;
		this.age_groups = age_groups;
		
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
	
	
	public static void main(String[] args) throws Exception {
		
				
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		final int[] COHORTS = {1990,1985,1980,1975,1970,1965,1960,1955,1950,1945,1940,0};
		final String[] COHORTS_STRINGS =  {"1990-1994"  ,"1985-1989","1980-1984","1975-1979","1970-1974","1965-1969","1960-1964","1955-1959","1950-1954","1945-1949","1940-1944","<1940"};
		final int[] AGE_GROUPS = {25,45,65,Integer.MAX_VALUE};
		final String[] AGE_GROUPS_STRINGS = {"18-24","25-44","45-64","65+"};
		
		String populationInputFile;
		String populationAttributesInputFile;
		String householdAttributesInputFile;
		
		populationInputFile = inputBase + "population.03.itnr.MZ2000.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.itnr.MZ2000.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.03.MZ2000.xml";
		TimeSeriesInfoExtractor extractorMZ2000 = new TimeSeriesInfoExtractor(populationInputFile,populationAttributesInputFile,householdAttributesInputFile, 2000, COHORTS, COHORTS_STRINGS,  AGE_GROUPS, AGE_GROUPS_STRINGS);	
		
		populationInputFile = inputBase + "population.12.MZ2005.xml";	
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2005.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2005.xml";
		TimeSeriesInfoExtractor extractorMZ2005 = new TimeSeriesInfoExtractor(populationInputFile,populationAttributesInputFile, householdAttributesInputFile, 2005, COHORTS, COHORTS_STRINGS, AGE_GROUPS, AGE_GROUPS_STRINGS);	
		
		populationInputFile = inputBase + "population.12.MZ2010.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2010.xml";
		TimeSeriesInfoExtractor extractorMZ2010 = new TimeSeriesInfoExtractor(populationInputFile,populationAttributesInputFile,householdAttributesInputFile, 2010, COHORTS, COHORTS_STRINGS,  AGE_GROUPS, AGE_GROUPS_STRINGS);	

		extractorMZ2000.extractAndPrint(outputBase + "TimeSeriesInfoMZ2000.txt");
		extractorMZ2005.extractAndPrint(outputBase + "TimeSeriesInfoMZ2005.txt");
		extractorMZ2010.extractAndPrint(outputBase + "TimeSeriesInfoMZ2010.txt");
		
		

	}
	
	
	public void extractAndPrint(String outputFile) throws Exception {
		
		
		out = IOUtils.getBufferedWriter(outputFile);
		
		printTitle();
		filterPopulationOver18();
//		ArrayList<Id>[] ids_cohort = getIdsByCohort();
//		printTotalPersonsPerCohort(ids_cohort);
//		printMeanAgePerCohort(ids_cohort);
//		printDriverLicenseByCohortAndAge(ids_cohort);
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
		printOccupancyRateData();
		
		
		
		out.close();
	
	}

	private void printOccupancyRateData() throws Exception  {
		out.write("------------------------------"); 
		out.newLine();
		out.write("OCCUPANCY RATE BY ACTIVITY PURPOSE"); 
		out.newLine();
		out.write("------------------------------"); 
	
		
		TreeMap<String, ArrayList<Etappe>> etappes = EtappenLoader.loadData(this.year);
		
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

 			for(Person person:population.getPersons().values()){
				
				
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
	
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				Person person = population.getPersons().get(id);
				
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
	
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES)
						 |((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES) ){
					
						abonnement_groups[0] += pw*hhw;
				}else if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)){
					if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES) ){
						
						abonnement_groups[2] += pw*hhw;	
					}else{
						abonnement_groups[1] += pw*hhw;
					}
				}else if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES) ){
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
					
					sum_weight +=  pw*hhw;
					
					if(((String)this.populationAttributes.getAttribute(id.toString(), "availability: car")).equals(MZConstants.ALWAYS)){
					
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
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
						
						double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
						total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DISTANCE))*pw*hhw;
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
						
						double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
						double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
						total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
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
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				total += Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_DURATION))*pw*hhw;
				sum_weight +=  pw*hhw;
				
				
			}
						
			out.write( "\t" + total/sum_weight );
			
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
						
			out.write( "\t" + total/sum_weight );
			
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
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				total = total + Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), MZConstants.TOTAL_TRIPS_INLAND))*pw*hhw;
				sum_weight +=  pw*hhw;
				
			}	
						
			out.write( "\t" + total/sum_weight);
			
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
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES) ){
				
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
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES)
						 |((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES) ){
				
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
				
				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)){
				
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

					
			out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
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
				
						
			out.write("\t" + counter +"\t" +counter/sum_weight*100 );
			
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
					counter+=  pw*hhw;
				}
				sum_weight +=  pw*hhw;
				
			}
						
			out.write( "\t" + counter/sum_weight*100);
			
			}
			
			out.newLine();
		}
		
		
	}

	private void printDriverLicenseByCohortAndAge(ArrayList<Id>[] ids_cohort) throws IOException {
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
				

				
				double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(id.toString(), "person weight"));
				double hhw = 1;//Double.parseDouble((String) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(id.toString(), "household number"), "weight"));
				
				if(((String)this.populationAttributes.getAttribute(id.toString(), "driving licence")).equals(MZConstants.YES)){
				
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

	
	private ArrayList<Id>[] getIdsByAgeGroup() throws IOException {
		

		ArrayList<Id>[] ids_age_groups = new ArrayList[this.age_groups.length];
		for(int i=0;i<this.age_groups.length;i++){
			ids_age_groups[i] = new ArrayList<Id>();
		}
		
		
		for(Person person:population.getPersons().values()){
			
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
		
				for(Person person:population.getPersons().values()){
			
				boolean hasCarAvailable = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "availability: car")).equals(MZConstants.ALWAYS);
				
				if(hasCarAvailable){
					ids_PW[0].add(person.getId());
				}else{
					ids_PW[1].add(person.getId());
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
		
				for(Person person:population.getPersons().values()){
			
				boolean hasCarAvailable = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "availability: car")).equals(MZConstants.ALWAYS);
				
				boolean hasSeasonTicket =  ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: GA first class")).equals(MZConstants.YES) |
										   ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: GA second class")).equals(MZConstants.YES) |
										   ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: Verbund")).equals(MZConstants.YES)|
										   ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: Halbtax")).equals(MZConstants.YES);
				
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
		
			String hhnr =	(String)this.populationAttributes.getAttribute(id.toString(), "household number");
				
			boolean zurich = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("261");
			boolean basel = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("2701");
			boolean bern = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("351");
			boolean geneva = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("6621");
			
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
		
			String hhnr =	(String)this.populationAttributes.getAttribute(id.toString(), "household number");
				
			boolean zurich = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("261");
			boolean basel = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("2701");
			boolean bern = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("351");
			boolean geneva = ((String)this.householdAttributes.getAttribute(hhnr, "municipality")).equals("6621");
			
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
		
			boolean hasAbonnement = (((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES) 
						| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES)
						| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)
						| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES));
			
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
		
			boolean hasCarAvailable = ((String)this.populationAttributes.getAttribute(id.toString(), "availability: car")).equals(MZConstants.ALWAYS);	
				
			boolean hasAbonnement = (((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA first class")).equals(MZConstants.YES) 
						| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: GA second class")).equals(MZConstants.YES)
						| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Halbtax")).equals(MZConstants.YES)
						| ((String)this.populationAttributes.getAttribute(id.toString(), "abonnement: Verbund")).equals(MZConstants.YES));
			
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

}
