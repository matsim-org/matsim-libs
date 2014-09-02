package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;

public class HouseholdStructureAnalyzer {

	private BufferedWriter out;
	private MicrocensusV2 microcensus;
	
	
	public HouseholdStructureAnalyzer(MicrocensusV2 microcensus){

		this.microcensus = microcensus;
		
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		String populationInputFile;
		String householdInputFile;
		String populationAttributesInputFile;
		String householdAttributesInputFile;
		String householdpersonsAttributesInputFile;
		MicrocensusV2 microcensus;
		
		
		populationInputFile = inputBase + "population.04.MZ1994.xml";	
		householdInputFile = inputBase + "households.04.MZ1994.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ1994.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ1994.xml";
		householdpersonsAttributesInputFile = inputBase + "householdpersonsAttributes.01.MZ1994.xml";
		microcensus = new MicrocensusV2(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile,householdpersonsAttributesInputFile, 1994);
		HouseholdStructureAnalyzer mz1994 = new HouseholdStructureAnalyzer(microcensus);

		
		populationInputFile = inputBase + "population.03.zid.MZ2000.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.zid.MZ2000.xml";
		householdInputFile = inputBase + "households.04.hpnr.MZ2000.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2000.xml";
		householdpersonsAttributesInputFile = inputBase + "householdpersonsAttributes.01.hpnr.MZ2000.xml";
		microcensus = new MicrocensusV2(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, householdpersonsAttributesInputFile, 2005);
		HouseholdStructureAnalyzer mz2000 = new HouseholdStructureAnalyzer(microcensus);
		
		
		populationInputFile = inputBase + "population.12.MZ2005.xml";
		householdInputFile = inputBase + "households.04.MZ2005.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2005.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2005.xml";
		householdpersonsAttributesInputFile = inputBase + "householdpersonsAttributes.01.MZ2005.xml";
		microcensus = new MicrocensusV2(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, householdpersonsAttributesInputFile, 2005);
		HouseholdStructureAnalyzer mz2005 = new HouseholdStructureAnalyzer(microcensus);

		
		populationInputFile = inputBase + "population.12.MZ2010.xml";
		householdInputFile = inputBase + "households.04.MZ2010.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2010.xml";
		householdpersonsAttributesInputFile = inputBase + "householdpersonsAttributes.01.MZ2010.xml";
		microcensus = new MicrocensusV2(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, householdpersonsAttributesInputFile, 2010);
		HouseholdStructureAnalyzer mz2010 = new HouseholdStructureAnalyzer(microcensus);
		
		mz1994.extractAndPrint(outputBase+ "HouseholdStructureAnalysisMZ1994.txt");
		mz2000.extractAndPrint(outputBase+ "HouseholdStructureAnalysisMZ2000.txt");
		mz2005.extractAndPrint(outputBase+ "HouseholdStructureAnalysisMZ2005.txt");
		mz2010.extractAndPrint(outputBase+ "HouseholdStructureAnalysisMZ2010.txt");
		
		
	}
	
	
	public void extractAndPrint(String outputFile) throws Exception {
		
		
		out = IOUtils.getBufferedWriter(outputFile);
		
//		printNumberOfHouseholdsBySize();
//		printSeparation();
//		printAgeRangeForSinglePersonHouseholds();
//		printSeparation();
//		printAgeRangeForNonSinglePersonHouseholds();
//		printSeparation();
//		printAgeRangeForMonoparentalHouseholds();
//		printSeparation();
//		printAgeRangeForCouplelHouseholds();
//		printSeparation();
//		printAgeRangeForChildsInHouseholds();
//		printSeparation();
//		printNumberOfChildsInHouseholdsDistribution();
//		printSeparation();		 
//		printCoupleHouseholdsAgeDifference();
//		printSeparation();	
//		printMotherBirthAges();
//		printSeparation();	
//		printAgeRangeForAllHouseholds();
//		printSeparation();	
//		printTypeOfHouseholdsDistribution();
//		printSeparation();		
		printNCarsAndNDrivingLicenseInHouseholdRatio();
		
		out.close();
		
	}

	private void printNCarsAndNDrivingLicenseInHouseholdRatio() throws IOException {
		
		out.write("Number of Cars and Driving License Ratio (per household)"); 
		out.newLine();
		int counter = 0;

	
		//[Single],[Single Parent], [Couple], [Couple with ch], [Complex]
				double[] counters = new double[5];
				double[] total = new double[5];
				
				TreeMap<Double, Double> table = new TreeMap<Double,Double>();
				double[][] car_licence = new double[6][6];
					
				for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
					
					String id = household.getId().toString();
					double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
					int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
					String anz_cars = (String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.TOTAL_CARS);
					if(anz_cars.equals(MZConstants.NO_ANSWER) || anz_cars.equals(MZConstants.NOT_KNOWN))
						continue;
					if(anz_cars.equals("more than 6"))
						anz_cars="7"; //special case for MZ2000, only 0.1% -> negligible
					double total_cars = Integer.parseInt(anz_cars);
					double total_licenses = 0;
					
//					if(total_cars==0)
//						continue;
					
					List<Id<Person>> members = household.getMemberIds();
					
					for(int i=0;i<=members.size()-1;i++){
						
						Id<Person> m_id = members.get(i);
						int license = (((String) microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.DRIVING_LICENCE)).equals(MZConstants.YES))?1:0;
						total_licenses+= license;
					}					

//					if(total_licenses==0)
//						continue;
					
					int typeOfHousehold = getTypeOfHousehold(members);
					
					if(typeOfHousehold!=4)
						continue;
					
				
					if(total_cars>5)
						total_cars=5;
					if(total_licenses>5)
						total_licenses=5;
					
					car_licence[(int) total_cars][(int) total_licenses]+=hh_weight*members.size();
				
					counters[typeOfHousehold]+= hh_weight;
					//total[typeOfHousehold] += (total_licenses/total_cars)*hh_weight;
					total[typeOfHousehold] += (total_cars/total_licenses)*hh_weight;
					
					double product = total_cars*total_licenses;
					
					if(!table.containsKey(product)){
						table.put(product, hh_weight);
					}else{
						table.put(product, table.get(product)+ hh_weight);
					}
					
				}

//				for(Entry<Double, Double> entry: table.entrySet()){
//					out.write("Product: \t" + entry.getKey() + "\t Value: \t" +  entry.getValue());
//					out.newLine();
//				}
				
//				for(double k=0;k<=50;k++){
//					if(table.get(k)==null){
//						out.write("Product: \t" + k + "\t Value: \t" +  0);
//					}else{
//						out.write("Product: \t" + k + "\t Value: \t" +  table.get(k));
//					}
//					
//					out.newLine();
//				}
				
				for(int m1=0; m1<car_licence.length;m1++){
					for(int m2 = 0; m2<car_licence.length;m2++){
						out.write(car_licence[m1][m2] + "\t");
						
					}
					
					out.newLine();
				}
				
				out.newLine();
				out.newLine();
				
				for(int a=0;a<=counters.length-1;a++){
					out.write("Type "+(a+1)+":\t" +total[a]/counters[a]);
					out.newLine();
				}		
	}

	private int getTypeOfHousehold(List<Id<Person>> members) {
		//[Single],[Single Parent], [Couple], [Couple with ch], [Complex]
		ArrayList<Integer> ages = new ArrayList<Integer>();

		for(int i=0;i<=members.size()-1;i++){
			
			Id<Person> m_id = members.get(i);
			int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
			ages.add(m_age);
		}
		
		int max_age = Collections.max(ages);
		
		List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
		ArrayList<Id<Person>> kids = new ArrayList<Id<Person>>(members);
		kids.removeAll(heads);
		
	
		if(members.size()==1){
			return 0;
		}else if(heads.size()==1){
			return 1;
		}else if(heads.size()==2 && kids.size()==0){
			return 2;
		}else if(heads.size()==2 && kids.size()!=0){
			return 3;
		}else{
			return 4;
		}
		
	}

	private void printSeparation() throws IOException {
		out.newLine();
		out.write("-------------------------------------------------------------------------------------------");
		out.newLine();
		
	}
	
	
	private void printAgeRangeForNonSinglePersonHouseholds() throws IOException {
		
		out.write("P(a_r|s>1)"); 
		out.newLine();
		
		//[15-24],[25-29], [30-49], [50-59], [60-74],[>75]
		double[] counters = new double[6];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			if(size==1)
				continue;
			
			List<Id<Person>> members = household.getMemberIds();
			if(members.size()<2){
				System.out.println("Error - non single household has only one member!");
				//Gbl.errorMsg("Error - non single household has only one member!");
				continue;
			}
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id<Person> m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			double age = this.getPrincipalHeadAge(heads);
			
		
			if(age<15){
				throw new RuntimeException("Head of household has age < 15!");
			}else if(age<25){
				counters[0]+=hh_weight;
			}else if(age<30){
				counters[1]+=hh_weight;
			}else if(age<50){
				counters[2]+=hh_weight;
			}else if(age<60){
				counters[3]+=hh_weight;
			}else if(age<75){
				counters[4]+=hh_weight;
			}else
				counters[5]+=hh_weight;
			
		}
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Age Group "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}
	
	}
	

	private double getAverageAgeOfIds(List<Id<Person>> ids) {
		
		int total = 0;
		
		for(Id<Person> id:ids){
			total += Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.AGE));

		}
		
		return total/ids.size();
		
	}


	private List<Id<Person>> getHeadsOfHouseholdIds(List<Id<Person>> members, int max_age) {
		
		 List<Id<Person>> heads = new ArrayList<Id<Person>>();
		
		if(members.size()==1)		
			return members;
		else{
					
			for(Id<Person> m_id:members){
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				if((m_age>max_age-16 && m_age>=18) || m_age>=30){
					heads.add(m_id);	
				}
			}
			
			return heads;
			
		}
				
	}

	private void printAgeRangeForSinglePersonHouseholds() throws IOException {
		
		out.write("P(a_r|s=1)"); 
		out.newLine();
		
		//[15-19],[20-24],[25-29], [30-49], [50-59], [60-74], [75-79], [>80]
		double[] counters = new double[8];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			if(size!=1)
				continue;
			
			List<Id<Person>> members = household.getMemberIds();
			
	
			if(members.size()!=1)
				throw new RuntimeException("Error - single households has more than one member!");
			
			Id person_id = members.get(0);
			
			int age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(person_id.toString(), MZConstants.AGE));
			
			if(age<15){
				throw new RuntimeException("Single person in household has age < 15!");
			}else if(age<20){
				counters[0]+=hh_weight;
			}else if(age<25){
				counters[1]+=hh_weight;
			}else if(age<30){
				counters[2]+=hh_weight;
			}else if(age<50){
				counters[3]+=hh_weight;
			}else if(age<60){
				counters[4]+=hh_weight;
			}else if(age<75){
				counters[5]+=hh_weight;
			}else if(age<80){
				counters[6]+=hh_weight;
			}else
				counters[7]+=hh_weight;
			
		}
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Age Group "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}
		
	}

	private void printNumberOfHouseholdsBySize() throws IOException {
		
		out.write("P(s)"); 
		out.newLine();
		
		//[1],[2],[3],[4],[5],[>=6]
		double[] counters = new double[6];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();

			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
			
			if(size>=6)
				size=6;
			
			counters[size-1]+= hh_weight;
		}
		
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Size "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}
		
		
	}
	
	
	
private void printAgeRangeForMonoparentalHouseholds() throws IOException {
		
		out.write("Monoparental Households"); 
		out.newLine();
		
		//[15-19],[20-24],[25-29], [30-34],[35-39],[40-44],[45-49],[50-54],[55-59], [60-64],[64-69][70-74],[75-79],[>80]
		double[] counters = new double[14];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			if(size==1)
				continue;
			
			List<Id<Person>> members = household.getMemberIds();
			if(members.size()<2){
				System.out.println("Error - non single household has only one member!");
				//Gbl.errorMsg("Error - non single household has only one member!");
				continue;
			}
				
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			
			if(heads.size()!=1)
				continue;
			
			double age = getAverageAgeOfIds(heads);
			
		
			if(age<15){
				throw new RuntimeException("Head of household has age < 15!");
			}else if(age<20){
				counters[0]+=hh_weight;
			}else if(age<25){
				counters[1]+=hh_weight;
			}else if(age<30){
				counters[2]+=hh_weight;
			}else if(age<35){
				counters[3]+=hh_weight;
			}else if(age<40){
				counters[4]+=hh_weight;
			}else if(age<45){
				counters[5]+=hh_weight;
			}else if(age<50){
				counters[6]+=hh_weight;
			}else if(age<55){
				counters[7]+=hh_weight;
			}else if(age<60){
				counters[8]+=hh_weight;
			}else if(age<65){
				counters[9]+=hh_weight;
			}else if(age<70){
				counters[10]+=hh_weight;
			}else if(age<75){
				counters[11]+=hh_weight;
			}else if(age<80){
				counters[12]+=hh_weight;	
			}else{
				counters[13]+=hh_weight;
			}
		}
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Age Group "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}
	
	}
	

private void printAgeRangeForCouplelHouseholds() throws IOException {
	
	out.write("Couple Households"); 
	out.newLine();
	
	//[15-19],[20-24],[25-29], [30-34],[35-39],[40-44],[45-49],[50-54],[55-59], [60-64],[64-69][70-74],[75-79],[>80]
	double[] counters = new double[14];
	
	for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
		
		String id = household.getId().toString();
		int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
		double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
	
		if(size==1)
			continue;
		
		List<Id<Person>> members = household.getMemberIds();
		if(members.size()<2){
			System.out.println("Error - non single household has only one member!");
			//Gbl.errorMsg("Error - non single household has only one member!");
			continue;
		}
			
		
		ArrayList<Integer> ages = new ArrayList<Integer>();
		
		for(int i=0;i<=members.size()-1;i++){
			
			Id m_id = members.get(i);
			int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
			ages.add(m_age);
		}
		int max_age = Collections.max(ages);
		
		List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
		
		if(heads.size()!=2)
			continue;
		
		double age = getPrincipalHeadAge(heads);
		
	
		if(age<15){
			throw new RuntimeException("Head of household has age < 15!");
		}else if(age<20){
			counters[0]+=hh_weight;
		}else if(age<25){
			counters[1]+=hh_weight;
		}else if(age<30){
			counters[2]+=hh_weight;
		}else if(age<35){
			counters[3]+=hh_weight;
		}else if(age<40){
			counters[4]+=hh_weight;
		}else if(age<45){
			counters[5]+=hh_weight;
		}else if(age<50){
			counters[6]+=hh_weight;
		}else if(age<55){
			counters[7]+=hh_weight;
		}else if(age<60){
			counters[8]+=hh_weight;
		}else if(age<65){
			counters[9]+=hh_weight;
		}else if(age<70){
			counters[10]+=hh_weight;
		}else if(age<75){
			counters[11]+=hh_weight;
		}else if(age<80){
			counters[12]+=hh_weight;	
		}else{
			counters[13]+=hh_weight;
		}
	}
	
	for(int a=0;a<=counters.length-1;a++){
		out.write("Age Group "+(a+1)+":\t" +counters[a]);
		out.newLine();
	}

}


	private double getPrincipalHeadAge(List<Id<Person>> heads) {
	
		boolean male_head = false;
		int head_age = 0;
		
		for(Id id:heads){
			
			boolean male  = ((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.GENDER)).equals(MZConstants.MALE);
						
			int age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.AGE));
			
			if(!male && !male_head && age>head_age){
				head_age = age;
			}else if(male && age>head_age){
				head_age = age;
			}
			
			if(male)
				male_head = true;
			
		}
			
		
	return head_age;
}

	private void printAgeRangeForChildsInHouseholds() throws IOException {
		
		out.write("Childs in Households"); 
		out.newLine();
		
		//[<15] ,[15-19],[20-24],[25-29], [30-34],[35-39],[40-44],[45-49],[50-54],[55-59], [60-64],[64-69][70-74],[75-79],[>80]
		double[] counters = new double[15];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			if(size==1)
				continue;
			
			List<Id<Person>> members = household.getMemberIds();
			if(members.size()<2){
				System.out.println("Error - non single household has only one member!");
				//Gbl.errorMsg("Error - non single household has only one member!");
				continue;
			}
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			
			ArrayList<Id> kids = new ArrayList<Id>(members);
			kids.removeAll(heads);
			
			if(kids.size()==0)
				continue;
			
			for(Id kid_id:kids){
				
				double age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(kid_id.toString(), MZConstants.AGE));	
				
				if(age<15){
					counters[0]+=hh_weight;
				}else if(age<20){
					counters[1]+=hh_weight;
				}else if(age<25){
					counters[2]+=hh_weight;
				}else if(age<30){
					counters[3]+=hh_weight;
				}else if(age<35){
					counters[4]+=hh_weight;
				}else if(age<40){
					counters[5]+=hh_weight;
				}else if(age<45){
					counters[6]+=hh_weight;
				}else if(age<50){
					counters[7]+=hh_weight;
				}else if(age<55){
					counters[8]+=hh_weight;
				}else if(age<60){
					counters[9]+=hh_weight;
				}else if(age<65){
					counters[10]+=hh_weight;
				}else if(age<70){
					counters[11]+=hh_weight;
				}else if(age<75){
					counters[12]+=hh_weight;
				}else if(age<80){
					counters[13]+=hh_weight;	
				}else{
					counters[14]+=hh_weight;
				}
			
			}
	
			
		}
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Age Group "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}
	
	}

	private void printNumberOfChildsInHouseholdsDistribution() throws IOException {
		
		out.write("Number of Childs in Households"); 
		out.newLine();
		
		//[without kids],[1 child], [2 childs], [3 childs], [4+ childs]
		double[] counters = new double[5];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			
			List<Id<Person>> members = household.getMemberIds();
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id<Person> m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			
			ArrayList<Id> kids = new ArrayList<Id>(members);
			kids.removeAll(heads);
			
		
			if(kids.size()==0){
				counters[0]+=hh_weight;
			}else if(kids.size()==1){
				counters[1]+=hh_weight;
			}else if(kids.size()==2){
				counters[2]+=hh_weight;
			}else if(kids.size()==3){
				counters[3]+=hh_weight;
			}else 
				counters[4]+=hh_weight;
			}
		
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Age Group "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}
	
	}

	
	
	private void printCoupleHouseholdsAgeDifference() throws IOException {
		int counter=0;
		int counter2=0;
		out.write("Couple Households Age Difference"); 
		out.newLine();
		
		//[15-19],[20-24],[25-29], [30-34],[35-39],[40-44],[45-49],[50-54],[55-59], [60-64],[64-69][70-74],[75-79],[>80]
		TreeMap<Integer, Double> counters = new TreeMap<Integer, Double>();
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			if(size==1)
				continue;
			
			List<Id<Person>> members = household.getMemberIds();
			if(members.size()<2){
				System.out.println("Error - non single household has only one member!");
				//Gbl.errorMsg("Error - non single household has only one member!");
				continue;
			}
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id<Person> m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			
			if(heads.size()!=2)
				continue;
			
			counter2++;
			
			int age = getCoupleAgeDifference(heads);
		
			if(age==Integer.MIN_VALUE){
				counter++;
				continue;
			}
		
			if(counters.get(age)!=null){
				double total = counters.get(age);
				counters.put(age, total+hh_weight);
				
			}else{
				counters.put(age, hh_weight);
			}
			
		

		}
		
		for(int a : counters.keySet()){
			out.write("Age Diff: \t "+ a+"\t" +counters.get(a));
			out.newLine();
		}

		out.newLine();
		out.write("Same sex couples: "+ counter + " out of: " + counter2 + " couples");
	}

	private int getCoupleAgeDifference(List<Id<Person>> heads) {
		
		if(heads.size()!=2)
			throw new RuntimeException("Couple household with more/less than 2 heads!");
		
		boolean male = false;
		boolean female = false;
		
		int age_diff=0;
		
		for(Id<Person> id:heads){
			
			boolean gender_male  = ((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.GENDER)).equals(MZConstants.MALE);
			int age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.AGE));	
			
			if(gender_male){
				age_diff+=age;
				male=true;
			}else{
				age_diff-=age;
				female=true;
				
			}
			
		}
		
		if(!(male&&female)){
			return Integer.MIN_VALUE;//Gbl.errorMsg("Couple?");
		}
			
		
		return age_diff;
		
	}
	
	
private void printMotherBirthAges() throws IOException {
		
		out.write("Mom's age at birth"); 
		out.newLine();
		//[10-14][15-19][20-24][25-29][30-34][35-39][40-44][45-49][+50]
		//[1st],[2nd], [3rd], [4th]...[11th]
		double[][] counters = new double[9][12];
		int counter = 0;
		int counter2 = 0;
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			
			List<Id<Person>> members = household.getMemberIds();
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			
			ArrayList<Id<Person>> kids = new ArrayList<Id<Person>>(members);
			kids.removeAll(heads);
			
			if(kids.size()==0)
				continue;
			
			counter2++;
			
			int mom_age = getMomAge(heads);
			
			if(mom_age==Integer.MIN_VALUE)
				counter++;
			
			
			ArrayList<Integer> kids_ages = getKidAgesInDescendingOrder(kids);
			int kid_nr=0;
				for(Integer kid_age:kids_ages){
					
					kid_nr++;
					
					int mom_birth_age = mom_age - kid_age;
					
					if(mom_birth_age<15){
						counters[0][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<20){
						counters[1][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<25){
						counters[2][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<30){
						counters[3][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<35){
						counters[4][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<40){
						counters[5][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<45){
						counters[6][kid_nr-1]+=hh_weight;
					}else if(mom_birth_age<50){
						counters[7][kid_nr-1]+=hh_weight;
					}else{
						counters[8][kid_nr-1]+=hh_weight;
					}
					
				}
			

			}
		
		for(int j=0;j<4;j++){
			
			
			for(int a=0;a<=counters.length-1;a++){
					
				out.write("Age Group "+(a+1)+"\t N° Kids \t" + (j+1) + "\t"+counters[a][j]);
				out.newLine();
			}
			out.newLine();
		
		}
			out.newLine();
			out.write("No mom found in " + counter + " out of " + counter2 + " cases." );
	
	}

	private ArrayList<Integer> getKidAgesInDescendingOrder(ArrayList<Id<Person>> kids) {
	
		ArrayList<Integer> ages = new ArrayList<Integer>();
		
			for(Id id:kids){
			
				int age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.AGE));	
			
				ages.add(age);
			}
		
			Collections.sort(ages, 
					new Comparator<Integer>(){

						@Override
						public int compare(Integer o1, Integer o2) {
							return -o1.compareTo(o2);
						}
				
			});
			
			return ages;
	
	}

	private int getMomAge(List<Id<Person>> heads) {
		
		int mom_age=0;
		boolean female = false;
		
		for(Id<Person> id:heads){
			
			boolean gender_female  = ((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.GENDER)).equals(MZConstants.FEMALE);
			int age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(id.toString(), MZConstants.AGE));	
			
			if(gender_female)
				female = true;
			
			if(gender_female&&age>mom_age)
				mom_age = age;
				
		}
		
		if(!female){
			return Integer.MIN_VALUE;
			//Gbl.errorMsg("No mom could be found -> no female head!");
		}
		
		return mom_age;
	}
	
	
	private void printAgeRangeForAllHouseholds() throws IOException {
		
		out.write("Age range for all Households"); 
		out.newLine();
		
		//[15-24],[25-29], [30-49], [50-59], [60-74],[>75]
		double[] counters = new double[6];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			int size = Integer.parseInt((String) this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_SIZE));
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			
			List<Id<Person>> members = household.getMemberIds();
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id<Person> m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			
		
			double age = getPrincipalHeadAge(heads);
			
		
			if(age<15){
				throw new RuntimeException("Head of household has age < 15!");
			}else if(age<25){
				counters[0]+=hh_weight;
			}else if(age<30){
				counters[1]+=hh_weight;
			}else if(age<50){
				counters[2]+=hh_weight;
			}else if(age<60){
				counters[3]+=hh_weight;
			}else if(age<75){
				counters[4]+=hh_weight;
			}else
				counters[5]+=hh_weight;
		}
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Age Group "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}

	}
	
	
private void printTypeOfHouseholdsDistribution() throws IOException {
		
		out.write("Type of households distribution"); 
		out.newLine();
		
		//[Single],[Single Parent], [Couple], [Couple with ch], [Complex]
		double[] counters = new double[5];
		
		for(Household household: this.microcensus.getHouseholds().getHouseholds().values()){
			
			String id = household.getId().toString();
			double hh_weight = Double.parseDouble((String)this.microcensus.getHouseholdAttributes().getAttribute(id, MZConstants.HOUSEHOLD_WEIGHT));
		
			
			List<Id<Person>> members = household.getMemberIds();
			
			ArrayList<Integer> ages = new ArrayList<Integer>();
			
			for(int i=0;i<=members.size()-1;i++){
				
				Id<Person> m_id = members.get(i);
				int m_age = Integer.parseInt((String)microcensus.getHouseholdPersonsAttributes().getAttribute(m_id.toString(), MZConstants.AGE));
				ages.add(m_age);
			}
			int max_age = Collections.max(ages);
			
			List<Id<Person>> heads = getHeadsOfHouseholdIds(members, max_age);
			ArrayList<Id<Person>> kids = new ArrayList<Id<Person>>(members);
			kids.removeAll(heads);
			
			if(members.size()==1){
				counters[0]+=hh_weight;	
			}else if(heads.size()==1){
				counters[1]+=hh_weight;
			}else if(heads.size()==2 && kids.size()==0){
				counters[2]+=hh_weight;
			}else if(heads.size()==2 && kids.size()!=0){
				counters[3]+=hh_weight;
			}else{
				counters[4]+=hh_weight;
			}
		
		}
		
		for(int a=0;a<=counters.length-1;a++){
			out.write("Type "+(a+1)+":\t" +counters[a]);
			out.newLine();
		}

	}
	
}
