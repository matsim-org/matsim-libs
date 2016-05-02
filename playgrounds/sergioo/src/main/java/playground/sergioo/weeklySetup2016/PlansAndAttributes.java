package playground.sergioo.weeklySetup2016;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import others.sergioo.util.probability.ContinuousRealDistribution;
import playground.sergioo.passivePlanning2012.core.population.EmptyTimeImpl;

public class PlansAndAttributes {
	
	private enum SimpleCategory {
		SHOP_LOW,
		SHOP_HIGH,
		BUSINESS,
		NEED, 
		EAT_HIGH,
		EAT_LOW,
		FUN,
		SPORT,
		CULTURAL,
		HEALTH,
		RELIGION;
	}
	
	private enum PlaceType {
		SHOP_LOW,
		SHOP_HIGH,
		EAT_HIGH,
		EAT_LOW,
		BUSINESS,
		NEED, 
		CULTURAL,
		FUN,
		SPORT;
	}
	
	private static enum FlexActivity {
		SHOP("shop"),
		EAT("eat"),
		ERRANDS("errands"),
		REC("rec"),
		MEDICAL("medical"),
		RELIGION("religion");
		private String text;
		private FlexActivity(String text) {
			this.text = text;
		}
	}
	
	private static enum Parameter {
		AGE("age"),
		INCOME("income"),
		MAININCOME("income_hh"),
		HOUSEHOLDSIZE("numpax"),
		HOMETIME("homeTime"),
		WORKTIME("workTime"),
		GENDER("sex"),
		HASCAR("car"),
		CHINESE("chinese"),
		INDIAN("indian"),
		MALAY("malay"),
		MAIN("main"),
		PARTNER("partner"),
		YOUNGER("younger");
		private String text;
		private Parameter(String text) {
			this.text = text;
		}
	}

	private enum Occupation {
		I0("agricultural and fishery workers","Agriculture & fishery worker"),
		I1("associate professionals and technicians","Associate professional & technician"),
		I2("cleaners, labourers and related workers","Cleaner & labourer & related worker"),
		I3("clerical workers","Clerical worker"),
		I4("plant and machine operators and assemblers","Plant & machine operator & assembler"),
		I5("production craftsmen and related workers","Production craftsman & related worker"),
		I6("professionals","Professional"),
		I7("senior officials and managers","Legislator & senior official & manager"),
		I8("service and sales workers","Service & sales worker"),
		I9("workers not classifiable by occupation","");
		private String textPop;
		private Occupation(String text, String textPop) {
			this.textPop = textPop;
		}
		private static Occupation getOccupationPop(String text) {
			for(Occupation occupation:values())
				if(occupation.textPop.equals(text))
					return occupation;
			return Occupation.I9;
		}
	}
	
	private enum SchoolType {
		I0("polytechnic"),
		I1("post-secondary (non-tertiary)"),
		I2("pre-primary"),
		I3("primary"),
		I4("professional qualification and other diploma"),
		I5("secondary"),
		I6("university");
		private String text;
		private SchoolType(String text) {
			this.text = text;
		}
		private static SchoolType getSchool(String text) {
			for(SchoolType school:values())
				if(school.text.equals(text))
					return school;
			return null;
		}
	}
	
	private enum Age {
		I0(0,4),
		I1(5,9),
		I2(10,14),
		I3(15,19),
		I4(20,24),
		I5(25,29),
		I6(30,34),
		I7(35,39),
		I8(40,44),
		I9(45,49),
		I10(50,54),
		I11(55,59),
		I12(60,64),
		I13(65,Double.POSITIVE_INFINITY);
		private double min;
		private double max;
		private Age(double min, double max) {
			this.min = min;
			this.max = max;
		}
	}
	
	private enum Ethnicity {
		I0("chinese", 8),
		I1("malay", 10),
		I2("indian", 9);
		private int index;
		private Ethnicity(String text, int index) {
			this.index = index;
		}
	}
	
	private enum Sex {
		I0("f", 6);
		private int index;
		private Sex(String text, int index) {
			this.index = index;
		}
	}
	
	private static class School {
		private String id;
		private boolean[] type;
		private int capacity;
		public School(String id, boolean[] type, int capacity) {
			super();
			this.id = id;
			this.type = type;
			this.capacity = capacity;
		}
	}
	
	private static final int NUM_DAYS = 7;
	
	private static final double NUM_PEOPLE_PER_OPP = 100;
	
	private static final int NUM_ACC_TYPES = 7;

	private static final int MIN_LEG_TIME = 15*60;

	private static final double TIME_GAP = 3600;
	
	private enum Step {
		CLUSTERS,
		SKELETONS,
		FACILITIES,
		ACTSTYPES,
		ATTRIBUTES;
	}
	
	private static Step step = Step.FACILITIES;
	
	/**
	 * 
	 * @param args
	 * 			0	-	population data
	 * 			1	-	out cluster dem work
	 * 			2 	-	out cluster dem study
	 * 			3	-	main facilities
	 * 			4 	-	cluster prob work
	 * 			5 	-	work clusters distribution
	 * 			6 	-	cluster prob study
	 * 			7 	-	study clusters distribution
	 * 			8 	-	out population file
	 * 			9 	-	out cluster durations
	 * 			10 	-	secondary facilities
	 * 			11	-	out facilities file
	 * 			12	-	accessibilities
	 * 			13	-	out pars access file
	 * 			14	-	activities prob file
	 * 			15	-	type place prob file
	 * 			16	-	out attributes file
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		DataBaseAdmin dataBaseAdmin  = new DataBaseAdmin(new File(args[0]));
		Map<String, String> homePlaces = new HashMap<>();
		Map<String, double[]> parameters = new HashMap<>();
		PopulationFactory factory = scenario.getPopulation().getFactory();
		Set<String> homes = new HashSet<>();
		//Read population
		if(step==Step.CLUSTERS || step==Step.ACTSTYPES) {
			ResultSet result = dataBaseAdmin.executeQuery("SELECT hhid,persid,postcode,eth,numpax,age,sex,income,car_license,car,income_hh FROM ro_population.persons_compact");
			Map<String, Set<String>> households = new HashMap<>();
			while(result.next()) {
				String id = result.getString("persid");
				double[]  pars = new double[Parameter.values().length];
				parameters.put(id, pars);
				pars[Parameter.AGE.ordinal()] = Integer.parseInt(result.getString(Parameter.AGE.text).substring(3, 5));
				pars[Parameter.INCOME.ordinal()] = result.getDouble(Parameter.INCOME.text);
				pars[Parameter.MAININCOME.ordinal()] = result.getDouble(Parameter.MAININCOME.text);
				pars[Parameter.HOUSEHOLDSIZE.ordinal()] = result.getInt(Parameter.HOUSEHOLDSIZE.text);
				pars[Parameter.GENDER.ordinal()] = result.getString(Parameter.GENDER.text).equals("m")?0:1;
				pars[Parameter.HASCAR.ordinal()] = result.getDouble(Parameter.HASCAR.text);
				pars[Parameter.CHINESE.ordinal()] = result.getString("eth").equals(Parameter.CHINESE.text)?1:0;
				pars[Parameter.INDIAN.ordinal()] = result.getString("eth").equals(Parameter.INDIAN.text)?1:0;
				pars[Parameter.MALAY.ordinal()] = result.getString("eth").equals(Parameter.MALAY.text)?1:0;
				String hid = result.getString("hhid");
				Set<String> household = households.get(hid);
				if(household==null) {
					household = new HashSet<>();
					household.add(id);
					households.put(hid, household);
				}
				else
					household.add(id);
				String home = result.getString("postcode");
				homes.add(home);
				homePlaces.put(id, home);
			}
			result.close();
			//Role
			for(Set<String> household:households.values()) {
				double maxIncome = -1;
				String main = ""; 
				for(String person:household) {
					double income = parameters.get(person)[Parameter.INCOME.ordinal()];
					if(income>maxIncome) {
						maxIncome = income;
						main = person;
					}
				}
				double minDiff = Double.MAX_VALUE, mainAge=parameters.get(main)[Parameter.AGE.ordinal()];
				String partner = "";
				for(String person:household)
					if(person.equals(main)) {
						parameters.get(person)[Parameter.MAIN.ordinal()] = 1;
						parameters.get(person)[Parameter.PARTNER.ordinal()] = 0;
						parameters.get(person)[Parameter.YOUNGER.ordinal()] = 0;
					}
					else {
						double diff = Math.abs(parameters.get(person)[Parameter.AGE.ordinal()] - mainAge);
						if(diff<minDiff) {
							minDiff = diff;
							partner = person;
						}
					}
				for(String person:household)
					if(person.equals(partner)) {
						parameters.get(person)[Parameter.PARTNER.ordinal()] = 1;
						parameters.get(person)[Parameter.MAIN.ordinal()] = 0;
						parameters.get(person)[Parameter.YOUNGER.ordinal()] = 0;
					}
					else if(!person.equals(main)) {
						double diff = parameters.get(person)[Parameter.AGE.ordinal()] - mainAge;
						if(diff<=0) {
							parameters.get(person)[Parameter.YOUNGER.ordinal()] = 1;
							parameters.get(person)[Parameter.MAIN.ordinal()] = 0;
							parameters.get(person)[Parameter.PARTNER.ordinal()] = 0;
						}
						else {
							parameters.get(person)[Parameter.YOUNGER.ordinal()] = 0;
							parameters.get(person)[Parameter.MAIN.ordinal()] = 0;
							parameters.get(person)[Parameter.PARTNER.ordinal()] = 0;
						}
					}
			}
			households.clear();
			System.out.println("Role done!");
		}
		else {
			ResultSet result = dataBaseAdmin.executeQuery("SELECT persid,postcode FROM ro_population.persons_compact");
			while(result.next()) {
				String id = result.getString("persid");
				String home = result.getString("postcode");
				scenario.getPopulation().addPerson(factory.createPerson(Id.createPersonId(id)));
				homes.add(home);
				homePlaces.put(id, home);
			}
			result.close();
		}
		System.out.println("Population done!");
		
		if(step!=Step.ATTRIBUTES) {
			//Read home work school
			Set<String> freetimers = new HashSet<>();
			ResultSet result = dataBaseAdmin.executeQuery("SELECT persid FROM ro_act_chain.other_chains");
			while(result.next())
				freetimers.add(result.getString("persid"));
			result.close();
			Map<String, Set<String>> works = new HashMap<>();
			Map<String, String> workPlaces = new HashMap<>();
			result = dataBaseAdmin.executeQuery("SELECT persid,facility_id FROM ro_mainactfacilityassignment.work_assignment");
			while(result.next()) {
				String work = result.getString("facility_id");
				String id = result.getString("persid");
				Set<String> workers = works.get(work);
				if(workers==null) {
					workers = new HashSet<>();
					works.put(work, workers);
				}
				workers.add(id);
				workPlaces.put(id, work);
			}
			result.close();
			Map<String, Set<String>> studys = new HashMap<>();
			Map<String, String> studyPlaces = new HashMap<>();
			result = dataBaseAdmin.executeQuery("SELECT persid,schoolpostalcode FROM ro_p_schoollocationchoice.schooldestinations_it_10");
			while(result.next()) {
				String study = result.getString("schoolpostalcode");
				String id = result.getString("persid");
				Set<String> students = studys.get(study);
				if(students==null) {
					students = new HashSet<>();
					studys.put(study, students);
				}
				students.add(id);
				studyPlaces.put(id, study);
			}
			result.close();
			System.out.println("Main locations done!");
			
			if(step == Step.CLUSTERS) {
				writeDemFiles(args[1], args[2], workPlaces, studyPlaces, parameters, dataBaseAdmin);
				System.out.println("Cluster files written!");
				return;
			}	
			
			ActivityFacilitiesFactory facFactory = new ActivityFacilitiesFactoryImpl();
			ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();
			
			if(step == Step.SKELETONS) {
				writeSkeletons(args[4], args[5], args[6], args[7], args[8], args[9], homePlaces, workPlaces, studyPlaces, freetimers, facilities, scenario.getPopulation(), factory);
				System.out.println("Skeletons written!");
				return;
			}
			else if(step == Step.FACILITIES) {
				//Read main facilities
				Map<String, OpeningTime> openingTimes = new HashMap<>();
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[5]));
				Map<Integer, List<ContinuousRealDistribution>> clusterDistributions = (Map) ois.readObject();
				ois.close();
				for(Entry<Integer, List<ContinuousRealDistribution>> entry:clusterDistributions.entrySet())
					for(int i=0; i<NUM_DAYS; i++) {
						double baseTime = i*24*3600;
						ContinuousRealDistribution startTime = entry.getValue().get(2*i);
						ContinuousRealDistribution duration = entry.getValue().get(2*i+1);
						if(startTime.getValues().size()>=0 && duration.getValues().size()>0) {
							String actOptText = "work_"+entry.getKey()+"_"+i;
							if(!openingTimes.containsKey(actOptText)) {
								double start = baseTime + startTime.getNumericalMean();
								OpeningTime ot = new OpeningTimeImpl(start-TIME_GAP,start+duration.getNumericalMean()+TIME_GAP);
								openingTimes.put(actOptText, ot);
							}
						}
					}
				ois = new ObjectInputStream(new FileInputStream(args[7]));
				clusterDistributions = (Map) ois.readObject();
				ois.close();
				for(Entry<Integer, List<ContinuousRealDistribution>> entry:clusterDistributions.entrySet())
					for(int i=0; i<NUM_DAYS; i++) {
						double baseTime = i*24*3600;
						ContinuousRealDistribution startTime = entry.getValue().get(2*i);
						ContinuousRealDistribution duration = entry.getValue().get(2*i+1);
						if(startTime.getValues().size()>=0 && duration.getValues().size()>0) {
							String actOptText = "study_"+entry.getKey()+"_"+i;
							if(!openingTimes.containsKey(actOptText)) {
								double start = baseTime + startTime.getNumericalMean();
								OpeningTime ot = new OpeningTimeImpl(start-TIME_GAP,start+duration.getNumericalMean()+TIME_GAP);
								openingTimes.put(actOptText, ot);
							}
						}
					}
				ois = new ObjectInputStream(new FileInputStream(args[9]+"W.dat"));
				Map<String, Integer> finalClusters = (Map<String, Integer>) ois.readObject();
				ois.close();
				ois = new ObjectInputStream(new FileInputStream(args[9]+".dat"));
				finalClusters.putAll((Map<String, Integer>) ois.readObject());
				ois.close();
				BufferedReader reader = IOUtils.getBufferedReader(args[3]);
				String line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(",");
					ActivityFacility facility = facFactory.createActivityFacility(Id.create(parts[0], ActivityFacility.class), new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
					((ActivityFacilityImpl)facility).setLinkId(Id.createLinkId(parts[3]));
					facilities.addActivityFacility(facility);
					if(homes.contains(parts[0]))
						facility.addActivityOption(facFactory.createActivityOption("home"));
					Set<String> workers = works.get(parts[0]);
					if(workers!=null) {
						Set<Integer> clusters = new HashSet<>();
						for(String worker:workers)
							clusters.add(finalClusters.get(worker));
						for(Integer cluster:clusters)
							for(int i=0; i<NUM_DAYS; i++) {
								String actText = "work_"+cluster+"_"+i;
								OpeningTime openingTime = openingTimes.get(actText);
								if(openingTime!=null) {
									ActivityOption actOption = facFactory.createActivityOption(actText); 
									actOption.addOpeningTime(openingTime);
									facility.addActivityOption(actOption);
								}
							}
					}
					Set<String> students = studys.get(parts[0]);
					if(students!=null) {
						Set<Integer> clusters = new HashSet<>();
						for(String student:students)
							clusters.add(finalClusters.get(student));
						for(Integer cluster:clusters)
							for(int i=0; i<NUM_DAYS; i++) {
								String actText = "study_"+cluster+"_"+i;
								OpeningTime openingTime = openingTimes.get(actText);
								if(openingTime!=null) {
									ActivityOption actOption = facFactory.createActivityOption(actText); 
									actOption.addOpeningTime(openingTime);
									facility.addActivityOption(actOption);
								}
							}
					}
					if(facility.getActivityOptions().size()==0)
						System.out.println("No fixed place!!!"+parts[0]);
					line = reader.readLine();
				}
				reader.close();
				homes.clear();
				works.clear();
				studys.clear();
				System.out.println("Main facilities done!");
				//Read secondary facilities
				reader = IOUtils.getBufferedReader(args[10]);
				line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(",");
					ActivityFacility facility = facilities.getFacilities().get(Id.create(parts[0], ActivityFacility.class));
					if(facility==null) {
						facility = facFactory.createActivityFacility(Id.create(parts[0], ActivityFacility.class), new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
						facilities.addActivityFacility(facility);
					}
					ActivityOption option = facFactory.createActivityOption(parts[7]);
					option.setCapacity(NUM_PEOPLE_PER_OPP*Double.parseDouble(parts[8]));
					facility.addActivityOption(option);
					line = reader.readLine();
				}
				reader.close();
				new FacilitiesWriter(facilities).write(args[11]);
			}
			else if(step == Step.ACTSTYPES) {
				writeActsTypes(args[12], args[13], args[9], parameters, homePlaces, workPlaces, studyPlaces, freetimers, facilities, facFactory);
				return;
			}
		}
		else {
			writeParameters(args[14], args[15], args[16], scenario.getPopulation());
		}
	}

	private static void writeDemFiles(String fileW, String fileS, Map<String, String> workPlaces, Map<String, String> studyPlaces, Map<String, double[]> parameters, DataBaseAdmin dataBaseAdmin) throws Exception {
		//Extra parameters
		Map<String, String> extraParameters = new HashMap<>();
		ResultSet result = dataBaseAdmin.executeQuery("SELECT persid,employment FROM ro_population.persons");
		while(result.next())
			extraParameters.put(result.getString("persid"), result.getString("employment"));
		//Schools
		Map<String, School> schools = new HashMap<>();
		result = dataBaseAdmin.executeQuery("SELECT postalcode,primaryschool,secondaryschool,tertiaryschool,foreignschool,capacity FROM ro_p_schoollocationchoice.school_facilities");
		while(result.next()) {
			String id = result.getString("postalcode");
			Integer cap = result.getInt("capacity");
			schools.put(id, new School(id, new boolean[]{result.getInt("primaryschool")>0?true:false,result.getInt("secondaryschool")>0?true:false,result.getInt("tertiaryschool")>0?true:false,result.getInt("foreignschool")>0?true:false},cap==null?500:cap));
		}
		result.close();
		for(Entry<String, String> student:studyPlaces.entrySet()) { 
			School school = schools.get(student.getValue());
			if(school!=null) {
				double age = parameters.get(student.getKey())[Parameter.AGE.ordinal()];
				String type = null;
				if(school.type[0] || school.type[3])
					if(age<=5)
						type = "pre-primary";
					else if(age<=9)
						type = "primary";
					else if(school.type[1] || school.type[3] && age<=19)
						type = "secondary";
					else if(age<=14)
						type = "primary";
				else if(school.type[1] || school.type[3])
					if(age>19 && !school.type[2])
						type = "post-secondary (non-tertiary)";
					else if(age<=19)
						type = "secondary";
					else if(school.type[2])
						if(school.capacity>9000)
							type = "university";
						else if(age<29)
							type = "polytechnic";
						else
							type = "professional qualification and other diploma";
				else
					if(school.capacity>9000)
						type = "university";
					else if(age<29)
						type = "polytechnic";
					else
						type = "professional qualification and other diploma";
				if(type==null)
					throw new Exception();
				extraParameters.put(student.getKey(), type);
			}
		}
		dataBaseAdmin.close();PrintWriter writer = new PrintWriter(fileW);
		for(int i=0; i<35; i++)
			writer.print("DEM"+i+",");
		writer.println("id");
		for(String id:workPlaces.keySet()) {
			double[] pars = parameters.get(id);
			double age = pars[Parameter.AGE.ordinal()];
			int ageI = 0;
			for(Age ageR:Age.values()) {
				if(ageR.min<age && ageR.max>=age)
					break;
				ageI++;
			}
			double ethI = 0;
			for(Ethnicity ethR:Ethnicity.values()) {
				if(pars[ethR.index]==1)
					break;
				ethI++;
			}
			double sexI=1;
			if(pars[Sex.I0.index]==1)
				sexI = 0;
			int occI = Occupation.getOccupationPop(extraParameters.get(id)).ordinal();
			for(int i=0; i<Age.values().length; i++)
				writer.print(i==ageI?"1,":"0,");
			for(int i=0; i<Ethnicity.values().length+1; i++)
				for(int j=0;j<Sex.values().length+1; j++)
					writer.print(i==ethI && j==sexI?"1,":"0,");
			for(int i=0; i<Occupation.values().length; i++)
				writer.print(i==occI?"1,":"0,");
			writer.print(pars[Parameter.HASCAR.ordinal()]==1?"1,0,":"0,1,");
			writer.println("0,"+id);
		}
		writer.close();
		writer = new PrintWriter(fileS);
		for(int i=0; i<32; i++)
			writer.print("DEM"+i+",");
		writer.println("id");
		for(String id:studyPlaces.keySet()) {
			double[] pars = parameters.get(id);
			double age = pars[Parameter.AGE.ordinal()];
			int ageI = 0;
			for(Age ageR:Age.values()) {
				if(ageR.min<age && ageR.max>=age)
					break;
				ageI++;
			}
			double ethI = 0;
			for(Ethnicity ethR:Ethnicity.values()) {
				if(pars[ethR.index]==1)
					break;
				ethI++;
			}
			double sexI=1;
			if(pars[Sex.I0.index]==1)
				sexI = 0;
			int schI = SchoolType.getSchool(extraParameters.get(id)).ordinal();
			for(int i=0; i<Age.values().length; i++)
				writer.print(i==ageI?"1,":"0,");
			for(int i=0; i<Ethnicity.values().length+1; i++)
				for(int j=0;j<Sex.values().length+1; j++)
					writer.print(i==ethI && j==sexI?"1,":"0,");
			for(int i=0; i<SchoolType.values().length; i++)
				writer.print(i==schI?"1,":"0,");
			writer.print(pars[Parameter.HASCAR.ordinal()]==1?"1,0,":"0,1,");
			writer.println("0,"+id);
		}
		writer.close();
		extraParameters.clear();
	}

	private static void writeSkeletons(String wScoresFile, String wDistrFile, String sScoresFile, String sDistrFile, String popFile, String clusDurFile, Map<String, String> homePlaces, Map<String, String> workPlaces, Map<String, String> studyPlaces, Set<String> freetimers, ActivityFacilities facilities, Population population, PopulationFactory factory) throws IOException, ClassNotFoundException {
		Map<String, Integer> finalClusters = new HashMap<>();
		Map<String, Double[]> durationDistributions = new HashMap<>();
		Map<String, Map<Integer, Double>> scores = new HashMap<>();
		if(!popFile.contains("W")) {
			//Read study cluster scores
			/*BufferedReader reader = IOUtils.getBufferedReader(sScoresFile);
			String line = reader.readLine();
			List<String> cols = new LinkedList<>(Arrays.asList(line.split(",")));
			cols.remove(0);
			Integer[] clusters = new Integer[cols.size()];
			int c=0;
			for(String part:cols)
				clusters[c++] = Integer.parseInt(part.substring(2,part.length()-1));
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				int i = 1;
				Map<Integer, Double> map = new HashMap<>();
				for(Integer cluster:clusters)
					map.put(cluster, Double.parseDouble(parts[i++]));
				scores.put(parts[0].substring(1, parts[0].length()-1), map);
				line = reader.readLine();
			}
			reader.close();
			
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(sDistrFile));
			Map<Integer, List<ContinuousRealDistribution>> clusterDistributions = (Map) ois.readObject();
			ois.close();
	
			//Create study skeletons
			int p=0;
			for(String id:studyPlaces.keySet()) {
				if(++p%100000==0)
					System.out.println(p+"/"+studyPlaces.size());
				ActivityFacility homeFacility = facilities.getFacilities().get(Id.create(homePlaces.get(id), ActivityFacility.class));
				ActivityFacility studyFacility = facilities.getFacilities().get(Id.create(studyPlaces.get(id), ActivityFacility.class));
				Plan plan = factory.createPlan();
				population.getPersons().get(Id.createPersonId(id)).addPlan(plan);
				Map<Integer, Double> scoresP = scores.get(id);
				double sumScores = 0;
				for(Double score:scoresP.values())
					sumScores += score;
				double currentScore = 0;
				double rand = Math.random()*sumScores;
				Integer bestCluster = null;
				for(Entry<Integer, Double> score:scoresP.entrySet()) {
					currentScore+=score.getValue();
					if(rand<currentScore)
						bestCluster = score.getKey();
				}
				finalClusters.put(id, bestCluster);
				List<ContinuousRealDistribution> distributions = clusterDistributions.get(bestCluster);
				Tuple<ContinuousRealDistribution, ContinuousRealDistribution> durations = new Tuple<>(new ContinuousRealDistribution(), new ContinuousRealDistribution());
				double startTimeH = -1;
				if(distributions.get(NUM_DAYS*4-2).getValues().size()>0)
					do {
						startTimeH = distributions.get(NUM_DAYS*4-2).sample();
					} while(startTimeH<0 || startTimeH>24*3600);
				double durationH = 0;
				if(distributions.get(NUM_DAYS*4-1).getValues().size()>0)
					do {
						durationH = distributions.get(NUM_DAYS*4-1).sample();
					} while(durationH<0 || durationH>24*3600);
				double endStudy = -MIN_LEG_TIME;
				for(int i=0; i<NUM_DAYS+1; i++) {
					double baseTime = i*24*3600;
					double startTime = -1;
					if(distributions.get((i%NUM_DAYS)*2).getValues().size()>0)
						do {
							startTime = distributions.get((i%NUM_DAYS)*2).sample();
						} while(startTime<0 || startTime>24*3600);
					double duration = 0;
					if(distributions.get((i%NUM_DAYS)*2+1).getValues().size()>0)	
						do {
							duration = distributions.get((i%NUM_DAYS)*2+1).sample();
						} while(duration<0 || duration>24*3600);
					double endHome = endStudy;
					if(startTimeH>=0 && durationH>0) {
						startTimeH -= 24*3600;
						startTimeH = startTimeH+baseTime<endStudy?(endStudy-baseTime):startTimeH;
						if(endStudy>0)
							((PlanImpl)plan).addLeg(new EmptyTimeImpl(studyFacility.getLinkId(), Math.max(MIN_LEG_TIME,startTimeH+baseTime-endStudy)));
						endHome = startTimeH+durationH;
						endHome = endHome<0?0:endHome>24*3600?endHome%(24*3600):endHome;
						endHome = startTime>=0 && endHome>startTime?startTime:endHome;
						durations.getFirst().addValue(endHome-startTimeH);
						Activity act = factory.createActivityFromCoord("home_"+bestCluster+"_"+(i%NUM_DAYS), homeFacility.getCoord());
						((ActivityImpl)act).setFacilityId(homeFacility.getId());
						endHome += baseTime;
						act.setEndTime(endHome);
						((PlanImpl)plan).addActivity(act);
						
					}
					if(i<NUM_DAYS) {
						startTimeH = -1;
						if(distributions.get(NUM_DAYS*2+i*2).getValues().size()>0)
							do {
								startTimeH = distributions.get(NUM_DAYS*2+i*2).sample();
							} while(startTimeH<0 || startTimeH>24*3600);
						durationH = 0;
						if(distributions.get(NUM_DAYS*2+i*2+1).getValues().size()>0)
							do {
								durationH = distributions.get(NUM_DAYS*2+i*2+1).sample();
							} while(durationH<0 || durationH>24*3600);
						endStudy = endHome;
						if(startTime>=0 && duration>0) {
							if(endHome>0)
								((PlanImpl)plan).addLeg(new EmptyTimeImpl(homeFacility.getLinkId(), Math.max(MIN_LEG_TIME,startTime+baseTime-endHome)));
							endStudy = startTime + duration;
							durations.getSecond().addValue(duration);
							Activity act = factory.createActivityFromCoord("study_"+bestCluster+"_"+i, studyFacility.getCoord());
							((ActivityImpl)act).setFacilityId(studyFacility.getId());
							endStudy += baseTime;
							act.setEndTime(endStudy);
							((PlanImpl)plan).addActivity(act);
						}
					}
				}
				durationDistributions.put(id, new Double[]{durations.getFirst().getNumericalMean(),durations.getSecond().getNumericalMean()});
			}
			scores.clear();
			System.gc();
			//Create free skeletons
			p=0;
			for(String id:freetimers) {
				if(++p%100000==0)
					System.out.println(p+"/"+freetimers.size());
				ActivityFacility homeFacility = facilities.getFacilities().get(Id.create(homePlaces.get(id), ActivityFacility.class));
				Plan plan = factory.createPlan();
				population.getPersons().get(Id.createPersonId(id)).addPlan(plan);
				Integer bestCluster = 1;
				finalClusters.put(id, bestCluster);
				List<ContinuousRealDistribution> distributions = clusterDistributions.get(bestCluster);
				Tuple<ContinuousRealDistribution, ContinuousRealDistribution> durations = new Tuple<>(new ContinuousRealDistribution(), new ContinuousRealDistribution());
				double startTimeH = -1;
				if(distributions.get(NUM_DAYS*4-2).getValues().size()>0)
					do {
						startTimeH = distributions.get(NUM_DAYS*4-2).sample();
					} while(startTimeH<0 || startTimeH>24*3600);
				double durationH = 0;
				if(distributions.get(NUM_DAYS*4-1).getValues().size()>0)
					do {
						durationH = distributions.get(NUM_DAYS*4-1).sample();
					} while(durationH<0 || durationH>24*3600);
				double endWork = -MIN_LEG_TIME;
				for(int i=0; i<NUM_DAYS+1; i++) {
					double baseTime = i*24*3600;
					double endHome = endWork;
					if(startTimeH>=0 && durationH>0) {
						startTimeH -= 24*3600;
						startTimeH = startTimeH+baseTime<endWork?(endWork-baseTime):startTimeH;
						if(endWork>0)
							((PlanImpl)plan).addLeg(new EmptyTimeImpl(homeFacility.getLinkId(), Math.max(MIN_LEG_TIME,startTimeH+baseTime-endWork)));
						endHome = startTimeH+durationH;
						endHome = endHome<0?0:endHome>24*3600?endHome%(24*3600):endHome;
						durations.getFirst().addValue(endHome-startTimeH);
						Activity act = factory.createActivityFromCoord("home_"+bestCluster+"_"+(i%NUM_DAYS), homeFacility.getCoord());
						((ActivityImpl)act).setFacilityId(homeFacility.getId());
						endHome += baseTime;
						act.setEndTime(endHome);
						((PlanImpl)plan).addActivity(act);
						
					}
					if(i<NUM_DAYS) {
						startTimeH = -1;
						if(distributions.get(NUM_DAYS*2+i*2).getValues().size()>0)
							do {
								startTimeH = distributions.get(NUM_DAYS*2+i*2).sample();
							} while(startTimeH<0 || startTimeH>24*3600);
						durationH = 0;
						if(distributions.get(NUM_DAYS*2+i*2+1).getValues().size()>0)
							do {
								durationH = distributions.get(NUM_DAYS*2+i*2+1).sample();
							} while(durationH<0 || durationH>24*3600);
						endWork = endHome;
					}
				}
				durationDistributions.put(id, new Double[]{durations.getFirst().getNumericalMean(),durations.getSecond().getNumericalMean()});
			}
			//Save Population
			new PopulationWriter(population).write(popFile);
			population.getPersons().clear();*/
		}
		else {
			//Read work cluster scores
			scores = new HashMap<>();
			BufferedReader reader = IOUtils.getBufferedReader(wScoresFile);
			String line = reader.readLine();
			List<String> cols = new LinkedList<>(Arrays.asList(line.split(",")));
			cols.remove(0);
			Integer[] clusters = new Integer[cols.size()];
			int c=0;
			for(String part:cols)
				clusters[c++] = Integer.parseInt(part.substring(2,part.length()-1));
			line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split(",");
				int i = 1;
				Map<Integer, Double> map = new HashMap<>();
				for(Integer cluster:clusters)
					map.put(cluster, Double.parseDouble(parts[i++]));
				scores.put(parts[0].substring(1, parts[0].length()-1), map);
				line = reader.readLine();
			}
			reader.close();
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(wDistrFile));
			Map<Integer, List<ContinuousRealDistribution>> clusterDistributions = (Map) ois.readObject();
			ois.close();
			//Create work skeletons
			int p=0;
			for(String id:workPlaces.keySet()) {
				if(studyPlaces.containsKey("FR100002_2"))
					System.out.println();
				if(++p%100000==0)
					System.out.println(p+"/"+workPlaces.size());
				ActivityFacility homeFacility = facilities.getFacilities().get(Id.create(homePlaces.get(id),ActivityFacility.class));
				ActivityFacility workFacility = facilities.getFacilities().get(Id.create(workPlaces.get(id),ActivityFacility.class));
				Plan plan = factory.createPlan();
				population.getPersons().get(Id.createPersonId(id)).addPlan(plan);
				Map<Integer, Double> scoresP = scores.get(id);
				double sumScores = 0;
				for(Double score:scoresP.values())
					sumScores += score;
				double currentScore = 0;
				double rand = Math.random()*sumScores;
				Integer bestCluster = null;
				for(Entry<Integer, Double> score:scoresP.entrySet()) {
					currentScore+=score.getValue();
					if(rand<currentScore)
						bestCluster = score.getKey();
				}
				finalClusters.put(id, bestCluster);
				List<ContinuousRealDistribution> distributions = clusterDistributions.get(bestCluster);
				Tuple<ContinuousRealDistribution, ContinuousRealDistribution> durations = new Tuple<>(new ContinuousRealDistribution(), new ContinuousRealDistribution());
				double startTimeH = -1;
				if(distributions.get(NUM_DAYS*4-2).getValues().size()>0)
					do {
						startTimeH = distributions.get(NUM_DAYS*4-2).sample();
					} while(startTimeH<0 || startTimeH>24*3600);
				double durationH = 0;
				if(distributions.get(NUM_DAYS*4-1).getValues().size()>0)
					do {
						durationH = distributions.get(NUM_DAYS*4-1).sample();
					} while(durationH<0 || durationH>24*3600);
				double endWork = -MIN_LEG_TIME;
				for(int i=0; i<NUM_DAYS+1; i++) {
					double baseTime = i*24*3600;
					double startTime = -1;
					if(distributions.get((i%NUM_DAYS)*2).getValues().size()>0)
						do {
							startTime = distributions.get((i%NUM_DAYS)*2).sample();
						} while(startTime<0 || startTime>24*3600);
					double duration = 0;
					if(distributions.get((i%NUM_DAYS)*2+1).getValues().size()>0)	
						do {
							duration = distributions.get((i%NUM_DAYS)*2+1).sample();
						} while(duration<0 || duration>24*3600);
					double endHome = endWork;
					if(startTimeH>=0 && durationH>0) {
						startTimeH -= 24*3600;
						startTimeH = startTimeH+baseTime<endWork?(endWork-baseTime):startTimeH;
						if(endWork>0)
							((PlanImpl)plan).addLeg(new EmptyTimeImpl(workFacility.getLinkId(), Math.max(MIN_LEG_TIME,startTimeH+baseTime-endWork)));
						endHome = startTimeH+durationH-24*3600;
						endHome = endHome<0?0:endHome>24*3600?endHome%(24*3600):endHome;
						endHome = startTime>=0 && endHome>startTime?startTime:endHome;
						durations.getFirst().addValue(endHome-startTimeH);
						Activity act = factory.createActivityFromCoord("home_"+bestCluster+"_"+(i%NUM_DAYS), homeFacility.getCoord());
						((ActivityImpl)act).setFacilityId(homeFacility.getId());
						endHome += baseTime;
						act.setEndTime(endHome);
						((PlanImpl)plan).addActivity(act);
					}
					if(i<NUM_DAYS) {
						startTimeH = -1;
						if(distributions.get(NUM_DAYS*2+i*2).getValues().size()>0)
							do {
								startTimeH = distributions.get(NUM_DAYS*2+i*2).sample();
							} while(startTimeH<0 || startTimeH>24*3600);
						durationH = 0;
						if(distributions.get(NUM_DAYS*2+i*2+1).getValues().size()>0)
							do {
								durationH = distributions.get(NUM_DAYS*2+i*2+1).sample();
							} while(durationH<0 || durationH>24*3600);
						endWork = endHome;
						if(startTime>=0 && duration>0) {
							if(endHome>0)
								((PlanImpl)plan).addLeg(new EmptyTimeImpl(homeFacility.getLinkId(), Math.max(MIN_LEG_TIME,startTime+baseTime-endHome)));
							endWork = startTime+duration;
							durations.getSecond().addValue(duration);
							Activity act = factory.createActivityFromCoord("work_"+bestCluster+"_"+i, workFacility.getCoord());
							((ActivityImpl)act).setFacilityId(workFacility.getId());
							endWork += baseTime;
							act.setEndTime(endWork);
							((PlanImpl)plan).addActivity(act);
						}
					}
				}
				durationDistributions.put(id, new Double[]{durations.getFirst().getNumericalMean(),durations.getSecond().getNumericalMean()});
			}
			scores.clear();
			//Save Population workers
			new PopulationWriter(population).write(popFile);
		}
		System.out.println("Skeletons created");
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(clusDurFile));
		oos.writeObject(finalClusters);
		oos.writeObject(durationDistributions);
		oos.close();
	}
	
	private static void writeActsTypes(String accessFile, String parsAccessFile, String clusDurationsFile, Map<String, double[]> parameters, Map<String, String> homePlaces, Map<String, String> workPlaces, Map<String, String> studyPlaces, Set<String> freetimers, ActivityFacilities facilities, ActivityFacilitiesFactory facFactory) throws IOException, ClassNotFoundException {
		//Read accessibilities
		Map<String, Map<SimpleCategory, Double[]>>[] accs = new Map[NUM_ACC_TYPES];   
		for(int a=0; a<NUM_ACC_TYPES; a++)
			accs[a] = new HashMap<>();
		BufferedReader reader = new BufferedReader(new FileReader(accessFile));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			for(int a=0; a<7; a++) {	
				Map<SimpleCategory, Double[]> map = accs[a].get(parts[0]);
				if(map==null) {
					map = new HashMap<>();
					accs[a].put(parts[0], map);
				}
				Double[] nums = map.get(SimpleCategory.valueOf(parts[3]));
				if(nums==null) {
					nums = new Double[2];
					map.put(SimpleCategory.valueOf(parts[3]), nums);
				}
				int i=4+2*a;
				nums[0] = Double.parseDouble(parts[i++]);
				nums[1] = Double.parseDouble(parts[i]);
			}
			line = reader.readLine();
		}
		reader.close();
		System.out.println("Accessibilities done!");
		PrintWriter[] writers = new PrintWriter[10];
		for(int w=0; w<10; w++) {
			writers[w] = new PrintWriter(parsAccessFile+w+".csv");
			writeHeader(writers[w]);
		}
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(clusDurationsFile+"W.dat"));
		ois.readObject();
		Map<String, Double[]> durationDistributions = (Map<String, Double[]>) ois.readObject();
		ois.close();
		int w=0;
		for(Entry<String, String> id: workPlaces.entrySet()) {
			writers[w].write(id.getKey()+",");
			Map<SimpleCategory, Double[]>[][] acs = new Map[NUM_ACC_TYPES][2];
			for(int a=0; a<NUM_ACC_TYPES; a++) {
				acs[a] = new Map[2];
				acs[a][0] = accs[a].get(homePlaces.get(id.getKey()));
				acs[a][1] = accs[a].get(id.getValue());
			}
			double[] pars = parameters.get(id.getKey());
			int i=0;
			for(;i<4;i++)
				writers[w].write(pars[i]+",");
			for(int a=0; a<NUM_ACC_TYPES;a++) {
				Map<SimpleCategory, Double[]>[] maps = acs[a];
				for(SimpleCategory simpleCategory:SimpleCategory.values()) {
					Double[] numsH = maps[0].get(simpleCategory);
					Double[] numsW = maps[1].get(simpleCategory);
					for(int m=0; m<2; m++) {
						writers[w].write(Math.max(numsH[m], numsW[m])+",");
						i++;
					}
				}
			}
			Double[] distrs = durationDistributions.get(id.getKey()); 
			pars[Parameter.HOMETIME.ordinal()] = Double.isNaN(distrs[0])?0:distrs[0];
			pars[Parameter.WORKTIME.ordinal()] = Double.isNaN(distrs[1])?0:distrs[1];
			for(;i<168;i++)
				writers[w].write(pars[i-154]+",");
			writers[w++].println();
			if(w==10)
				w=0;
		}
		System.out.println("Workers done!");
		ois = new ObjectInputStream(new FileInputStream(clusDurationsFile+".dat"));
		ois.readObject();
		durationDistributions = (Map<String, Double[]>) ois.readObject();
		ois.close();
		for(Entry<String, String> id: studyPlaces.entrySet()) {
			writers[w].write(id.getKey()+",");
			Map<SimpleCategory, Double[]>[][] acs = new Map[NUM_ACC_TYPES][2];
			for(int a=0; a<NUM_ACC_TYPES; a++) {
				acs[a] = new Map[2];
				acs[a][0] = accs[a].get(homePlaces.get(id.getKey()));
				acs[a][1] = accs[a].get(id.getValue());
			}
			double[] pars = parameters.get(id.getKey());
			int i=0;
			for(;i<4;i++)
				writers[w].write(pars[i]+",");
			for(int a=0; a<NUM_ACC_TYPES;a++) {
				Map<SimpleCategory, Double[]>[] maps = acs[a];
				for(SimpleCategory simpleCategory:SimpleCategory.values()) {
					Double[] numsH = maps[0].get(simpleCategory);
					Double[] numsW = maps[1].get(simpleCategory);
					for(int m=0; m<2; m++) {
						writers[w].write(Math.max(numsH[m], numsW[m])+",");
						i++;
					}
				}
			}
			Double[] distrs = durationDistributions.get(id.getKey()); 
			pars[Parameter.HOMETIME.ordinal()] = Double.isNaN(distrs[0])?0:distrs[0];
			pars[Parameter.WORKTIME.ordinal()] = Double.isNaN(distrs[1])?0:distrs[1];
			for(;i<168;i++)
				writers[w].write(pars[i-154]+",");
			writers[w++].println();
			if(w==10)
				w=0;
		}
		System.out.println("Students done!");
		for(String id: freetimers) {
			writers[w].write(id+",");
			Map<SimpleCategory, Double[]>[] acs = new HashMap[NUM_ACC_TYPES];
			for(int a=0; a<NUM_ACC_TYPES; a++)
				acs[a] = accs[a].get(homePlaces.get(id));
			double[] pars = parameters.get(id);
			int i=0;
			for(;i<4;i++)
				writers[w].write(pars[i]+",");
			for(int a=0; a<NUM_ACC_TYPES;a++) {
				Map<SimpleCategory, Double[]> map = acs[a];
				for(SimpleCategory simpleCategory:SimpleCategory.values()) {
					Double[] nums = map.get(simpleCategory);
					for(Double num: nums) {
						writers[w].write(num+",");
						i++;
					}
				}
			}
			Double[] distrs = durationDistributions.get(id); 
			pars[Parameter.HOMETIME.ordinal()] = Double.isNaN(distrs[0])?0:distrs[0];
			pars[Parameter.WORKTIME.ordinal()] = Double.isNaN(distrs[1])?0:distrs[1];
			for(;i<168;i++)
				writers[w].write(pars[i-154]+",");
			writers[w++].println();
			if(w==10)
				w=0;
		}
		System.out.println("Freetimers done!");
		for(w=0; w<10; w++)
			writers[w].close();
	}
	
	private static void writeHeader(PrintWriter printer) {
		printer.print("ID,");
		printer.print("AGE,");
		printer.print("INCOME,");
		printer.print("MAIN_INCOME,");
		printer.print("HOUSEHOLD_SIZE,");
		for(int a=0; a<7; a++)
			for(SimpleCategory detailedType:SimpleCategory.values()) {
				printer.print("ACC_"+detailedType.name().toUpperCase()+a+"_CAR,");
				printer.print("ACC_"+detailedType.name().toUpperCase()+a+"_PT,");
			}
		printer.print("HOME_TIME,");
		printer.print("WORK_TIME,");
		printer.print("GENDER,");
		printer.print("CAR_AVAIL,");
		printer.print("CHINESE,");
		printer.print("INDIAN,");
		printer.print("MALAY,");
		printer.print("MAIN,");
		printer.print("PARTNER,");
		printer.print("YOUNGER,");
		printer.println();
	}
	
	private static void writeParameters(String actsFile, String typesFile, String attributesFile, Population population) throws IOException {
		//Calculate attributes
		ObjectAttributes attributes = population.getPersonAttributes();
		BufferedReader reader = IOUtils.getBufferedReader(actsFile);
		reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			int a=1;
			for(FlexActivity flexActivity:FlexActivity.values()) {
				for(int f=0; f<2; f++)
					attributes.putAttribute(parts[0].substring(1, parts[0].length()-1), flexActivity.text+f, Double.parseDouble(parts[a++]));
			}
			line = reader.readLine();
		}
		reader.close();
		reader = IOUtils.getBufferedReader(typesFile);
		reader.readLine();
		line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			int a=1;
			for(PlaceType flexActivity:PlaceType.values())
				attributes.putAttribute(parts[0].substring(1, parts[0].length()-1), flexActivity.name(), Double.parseDouble(parts[a++]));
			line = reader.readLine();
		}
		reader.close();
		new ObjectAttributesXmlWriter(attributes).writeFile(attributesFile);
		System.out.println("Attributes done!");
	}
	
	
}
