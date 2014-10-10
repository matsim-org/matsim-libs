package playground.acmarmol.matsim2030.forecasts.mz2010sample;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.acmarmol.matsim2030.forecasts.Loader;
import playground.acmarmol.matsim2030.forecasts.p2030preparation.Municipality;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.Etappe;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.loaders.etappes.EtappenLoader;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;
import playground.acmarmol.utils.MyCollectionUtils;

public class MZ2010SampleDataCreator {
	

	private Population population;
	private ObjectAttributes populationAttributes;
	private ObjectAttributes householdAttributes;
	private BufferedWriter out;
	private TreeMap<String, Integer> mun_totals;
	private HashMap<String, String> mun_changes;
	private HashMap<String, String> mun_changes_const;
	private TreeMap<String, Integer> gemeinde_typen;
	private HashMap<Id<Municipality>, Id<Municipality>> ozs;
	private HashMap<Id<Municipality>, Id<Municipality>> mzs;
	private HashMap<Id<Municipality>, Id<Municipality>> ozToGrossZtr;
	private TreeMap<String, Tuple<String, String>> travel_times_IV;
	private TreeMap<String, Tuple<String, String>> travel_times_OV;
	private String inputBase;
	private String outputBase;
	private int year;
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		String populationFile = inputBase + "population.09.MZ2010.xml";
		String populationAttFile = inputBase + "populationAttributes.04.MZ2010.xml";
		String householdAttFile = inputBase + "householdAttributes.04.MZ2010.xml";		
		
		MZ2010SampleDataCreator mz2010 = new MZ2010SampleDataCreator(populationFile, populationAttFile, householdAttFile, 2010);
		mz2010.setInputBase(inputBase);
		mz2010.setOutputBase(outputBase);
		
		mz2010.loadData();
		mz2010.createDataForMNLModels(outputBase + "mz2010SampleForMNLModels.dat");
		mz2010.createDataForRegressionModels(outputBase + "mz2010SampleForRegressionModels.dat");
		
	}

	private void loadData() throws IOException {
		
		this.mun_totals = Loader.loadMunicipalityTotals(outputBase + "municipality_population.txt");
		this.mun_changes = Loader.loadEliminatedMunicipalitiesDatabase( inputBase + "municipalities/eliminated municipalities 01.01.2010 - 01.07.2011.txt");
		this.mun_changes_const = Loader.loadCreatedMunicipalitiesDatabase2(inputBase +"municipalities/nuovi costituzione 01.01.2000-31.12.2010.txt");
		this.gemeinde_typen = Loader.loadGemeindetypologieARE(inputBase + "Gemeindetypen_ARE_2010_VZ.txt");
		ArrayList<HashMap<Id<Municipality>, Id<Municipality>>> MZandOZ = Loader.loadOZandMZDatabase(inputBase + "updated Gemeindezuordnung MZ+OZ_ARE.txt");
		this.ozs = MZandOZ.get(0);
		this.mzs = MZandOZ.get(1);
		this.travel_times_IV = Loader.loadTravelTimesToMZandOZ(inputBase + "municipalities_TT0_01-DWV_2005MIV_Masternetz_LKW_Kalibriert2010_A1_AFTER_MATLAB.txt", MZandOZ);
		this.travel_times_OV = Loader.loadTravelTimesToMZandOZ(inputBase + "municipalities_TT0_02-OEV_DWV_2005-HAFAS_A1_AFTER_MATLAB.txt", MZandOZ);
		this.ozToGrossZtr = Loader.loadOZtoGrossZtr(inputBase + "GrossZtr10.txt");
	}



	public MZ2010SampleDataCreator(String populationInputFile, String populationAttFile, String householdAttFile, int year){
		
		Config config = ConfigUtils.createConfig();
		config.setParam("plans", "inputPlansFile", populationInputFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.population = scenario.getPopulation();
		this.populationAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(populationAttributes);
		reader.putAttributeConverter(CoordImpl.class, new CoordConverter());
		reader.parse(populationAttFile);
		this.householdAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader readerHH = new ObjectAttributesXmlReader(householdAttributes);
		readerHH.putAttributeConverter(CoordImpl.class, new CoordConverter());
		readerHH.parse(householdAttFile);
		this.year = year;
		
	}
	
	public void createDataForMNLModels(String outputFile) throws IOException{
		System.out.println("Creating data file for MNL Models...");
		//MNL models only use data for population > 18 years
		this.filterPopulationOver18();
		out = IOUtils.getBufferedWriter(outputFile);
				
		printHeaderMNLDataFile();		
		printDataForMNLModels();
		out.flush();
		out.close();
		System.out.println("		...done (creating data file for MNL Models)");
		
	}
	
	private void createDataForRegressionModels(String outputFile) throws Exception {
		System.out.println("Creating data file for Regression Models...");
		//Regression models use all population
		out = IOUtils.getBufferedWriter(outputFile);
		System.out.println(population.getPersons().size());		
		printHeaderForRegressionFile();		
		printDataForRegressionModels();
		out.flush();
		out.close();
		System.out.println("		...done (creating data file for Regression Models)");
		
	}
	
	
	private void printDataForRegressionModels() throws Exception {
		
		TreeMap<String, ArrayList<Etappe>> etappes = EtappenLoader.loadData(this.year);
		
		for(Person person: population.getPersons().values()){
			
			String HH_NR = (String)this.populationAttributes.getAttribute(person.getId().toString(), "household number");
			String[]  HH_GEM= getHH_GEM(HH_NR);
			
			Id<Municipality> oz = ozs.get(Id.create(HH_GEM[0], Municipality.class));
			Id<Municipality> grossZtr = ozToGrossZtr.get(oz);
			int Geschl = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "gender")).equals("m")?1:0;
			
			int abo_ges  = (((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: GA first class")).equals(MZConstants.YES)
					|| ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: GA second class")).equals(MZConstants.YES)
					||((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: Halbtax")).equals(MZConstants.YES)
					||((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: Verbund")).equals(MZConstants.YES))?1:0;
			
			String fs_pw_zp  = ((PersonImpl)person).getLicense().equals(MZConstants.YES)?"1":"0";
			
			String[] tt = getTravelTimes(HH_GEM);
			String rz_oz_iv = tt[0];
			String rz_mz_iv = tt[1];
			String rz_oz_ov = tt[2];;
			String rz_mz_ov = tt[3];;
			
			String Alter = (String)this.populationAttributes.getAttribute(person.getId().toString(), "age");
			double lAlter = Math.log(Double.parseDouble(Alter));
			
			Integer RG_verkGr;
			try{
				RG_verkGr = this.gemeinde_typen.get(HH_GEM[0])*10;
			} catch (NullPointerException e){
				RG_verkGr = this.gemeinde_typen.get(HH_GEM[1])*10;
			}
			
			if(RG_verkGr==10){ //is one of the Grosszentren
				RG_verkGr = identifyGrosszentren(HH_GEM[0]);				
			}
			
			int ver_pw  = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "availability: car")).equals(MZConstants.ALWAYS)?1:0;
			int erwerb = ((PersonImpl)person).isEmployed()?1:0;
			
			int mzjahr2 = 2030- this.year; 
			
			int kohortgr = getCohort(Alter);
			
			double[] fahrtzeiten = new double[4];  // [0: PW, 1: OEV, 2: LANGSAM, 3: SONST]
			double[] tagesdistanzen = new double[4];
			
						
			if(person.getSelectedPlan()!=null){
			ArrayList<Etappe> etappen = etappes.get(person.getId().toString());
					
				for(Etappe  etappe: etappen){
					
					int index = getModeType(etappe.getMode());
					
					fahrtzeiten[index] += Double.parseDouble(etappe.getDuration());
					tagesdistanzen[index] += Double.parseDouble(etappe.getDistance());
				}
			}
				
			double t_langs = fahrtzeiten[2];
			double t_pw = fahrtzeiten[0];
			double t_oev = fahrtzeiten[1];
			double t_sonst = fahrtzeiten[3];
			double d_langs = tagesdistanzen[2];
			double d_pw = tagesdistanzen[0];
			double d_oev = tagesdistanzen[1];
			double d_sonst = tagesdistanzen[3];	
			
			out.write(person.getId() + "\t" + kohortgr + "\t" +	Geschl +	"\t" + abo_ges + "\t" + fs_pw_zp +  "\t" + rz_mz_iv + " \t" + rz_mz_ov + "\t" + rz_oz_iv  + "\t" +	rz_oz_ov + "\t" + ver_pw+   "\t" +  this.year + "\t "  
					 + mzjahr2 + "\t" +	lAlter + "\t" +	erwerb+ "\t" + grossZtr +"\t" + RG_verkGr + "\t" + 	t_langs + "\t" +	t_pw + "\t" + t_oev  + "\t " +
					t_sonst + "\t" + d_langs + "\t" +   d_pw + "\t" + d_oev + "\t" + d_sonst);
			out.newLine();	

			
		}//end person loop
			
	}

	private int getModeType(String mode) {
		
		if(mode.equals(MZConstants.CAR_FAHRER) || mode.equals(MZConstants.CAR_MITFAHRER)){
		
			return 0;
		
		}else if(mode.equals(MZConstants.TRAIN)
				|| mode.equals(MZConstants.POSTAUTO)
				|| mode.equals(MZConstants.BUS)
				|| mode.equals(MZConstants.TRAM)
				|| mode.equals(MZConstants.TAXI)
				|| mode.equals(MZConstants.SHIP)
				|| mode.equals(MZConstants.SONSTINGER_OEV)){
			return 1;
			
		}else if(mode.equals(MZConstants.WALK)
				|| mode.equals(MZConstants.BICYCLE)
				|| mode.equals(MZConstants.CABLE_CAR)
				|| mode.equals(MZConstants.SKATEBOARD)){
			
			return 2;
			
		}else if(mode.equals(MZConstants.OTHER)
				||mode.equals(MZConstants.PLANE)
				|| mode.equals(MZConstants.MOFA)
				|| mode.equals(MZConstants.MOTORRAD_FAHRER)
				|| mode.equals(MZConstants.MOTORRAD_MITFAHRER)
				|| mode.equals(MZConstants.KLEINMOTORRAD)
				|| mode.equals(MZConstants.TRUCK)
				|| mode.equals(MZConstants.REISECAR)
				|| mode.equals(MZConstants.PSEUDOETAPPE)){
			
			return 3;
			
		}else{
			throw new RuntimeException("No mode type classification defined for mode: "+ mode);
		}
	}

	private int getCohort(String alter) {
		final int[] cohorts = {2020,2010,2000,1990,1980,1970,1960,1950,1940,1930,1920,1910,1900,0};
		
		int kohort_gr = 0;
		int[] max_age = new int[cohorts.length];
		
		for(int i=0;i<cohorts.length;i++){
			max_age[i] = this.year-cohorts[i];
		}
		max_age[cohorts.length-1] = Integer.MAX_VALUE;
		
		for(int i=0;i<cohorts.length;i++){
			kohort_gr = i;
			if(Integer.parseInt(alter) <= max_age[i]){
				break;
			}
		}	
		
		return 12-kohort_gr;
	}

	private int identifyGrosszentren(String code) {
		if(code.equals("230")){//winterthur
			return 1;
		}else if(code.equals("261")){//zürich
			return 2;
		}else if(code.equals("351")){//bern
			return 3;
		}else if(code.equals("1061")){//luzern
			return 4;
		}else if(code.equals("2701")){//basel
			return 5;
		}else if(code.equals("3203")){//st. gallen
			return 6;
		}else if(code.equals("5192")){//lugano
			return 7;
		}else if(code.equals("5586")){//laussane
			return 8;
		}else if(code.equals("6621")){//geneve
			return 9;
		}else{
			throw new RuntimeException("no grossezentren for gemeinde code: "+ code);
		}
	}

	private String[] getTravelTimes(String[] HH_GEM) {
		
		String rz_oz_iv;
		try{
		rz_oz_iv = this.travel_times_IV.get(HH_GEM[0]).getFirst();
		} catch (NullPointerException e){
			rz_oz_iv = this.travel_times_IV.get(HH_GEM[2]).getFirst();	
		}
		
		String rz_mz_iv;
		try{
			rz_mz_iv = this.travel_times_IV.get(HH_GEM[0]).getSecond();
		} catch (NullPointerException e){
			rz_mz_iv = this.travel_times_IV.get(HH_GEM[2]).getSecond();
		}
		//OV
		String rz_oz_ov;
		try{
		rz_oz_ov = this.travel_times_OV.get(HH_GEM[0]).getFirst();
		} catch (NullPointerException e){
			rz_oz_ov = this.travel_times_OV.get(HH_GEM[2]).getFirst();	
		}
	
		String rz_mz_ov;
		try{
			rz_mz_ov = this.travel_times_OV.get(HH_GEM[0]).getSecond();
		} catch (NullPointerException e){
			rz_mz_ov = this.travel_times_OV.get(HH_GEM[2]).getSecond();
		}

		String[] tt = {rz_oz_iv, rz_mz_iv, rz_oz_ov, rz_mz_ov};
		
		return tt;
	}

	private void printHeaderForRegressionFile() throws IOException {
		out.write("pid \t kohortgr \t gesch \t abo_ges \t fs_pw_zp  \t iv_t \t oev_t \t iv_oz_t \t oev_oz_t \t verpw_zp \t mzjahr \t " + 
				"mzjahr2 \t lalter \t erwerb \t grossztr \t rg_verk2 \t t_langs \t t_pw \t t_oev \t" +
				"t_sonst \t d_langs \t d_pw \t d_oev \t d_sonst");
		out.newLine();
		
	}



	private void printDataForMNLModels() throws IOException {
		
		
		for(Person person: population.getPersons().values()){
				
			
			// HH_NR	\t	P_NR	\t	W_P	
			String HH_NR = (String)this.populationAttributes.getAttribute(person.getId().toString(), "household number");
			out.write(HH_NR +"\t");
			String P_NR = person.getId().toString();
			out.write(P_NR +"\t");
			String W_P = (String)this.populationAttributes.getAttribute(person.getId().toString(), "person weight");
			out.write(W_P +"\t");
//			String Kanton = (String)this.householdAttributes.getAttribute(HH_NR, "kanton");
//			out.write(Kanton +"\t");
			
			
			String[]  HH_GEM= getHH_GEM(HH_NR);
			
			//  HH_GEM \t Geschl \t Alter \t Alter2 \t Erwerb \t Eink_imp \t Eink \t Eink_1000 \t Eink_ln \t Bev_total \t Bev_1000 \t Bev_logn
			out.write(HH_GEM[0] +"\t");
			int Geschl = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "gender")).equals("m")?1:0;
			out.write(Geschl +"\t");
			String Alter = (String)this.populationAttributes.getAttribute(person.getId().toString(), "age");
			out.write(Alter +"\t");
			out.write(Math.pow(Integer.parseInt(Alter),2) +"\t");
			int Erwerb = ((PersonImpl)person).isEmployed()?1:0;
			out.write(Erwerb +"\t");
			String Eink_imp = (String)this.householdAttributes.getAttribute(HH_NR, "income");
			out.write(Eink_imp +"\t");
			int Eink = getIncome(Eink_imp);
			out.write(Eink  +"\t");
			out.write(Eink/1000 +"\t");
			out.write(Math.log(Eink) +"\t");
			
			double bevolkerung = mun_totals.get(HH_GEM[1]);
			out.write(bevolkerung +"\t");
			out.write(bevolkerung/1000 +"\t");
			out.write(Math.log(bevolkerung) +"\t");
			
			// RG_verk1 \t RG_verk2 \t RG_verk3 \t RG_verk4 \t RG_verk5 \t RG_verk6
			String[] gem_type = {"0","0","0","0","0","0"};
			try{
				gem_type[this.gemeinde_typen.get(HH_GEM[0])-1] = "1";
			} catch (NullPointerException e){
			gem_type[this.gemeinde_typen.get(HH_GEM[1])-1] = "1";
			}
			out.write(MyCollectionUtils.arrayToTabSeparatedString(gem_type) +"\t");
			
			

			// RZ_OZ_IV  \t RZ_MZ_IV \t RZ_MZ_IV_ln \t RZ_OZ_OV  \t RZ_MZ_OV \t RZ_MZ_OV_ln  \t OZ
			
			String[] tt = this.getTravelTimes(HH_GEM);
			//IV
			String rz_oz_iv = tt[0];
			out.write(rz_oz_iv + "\t");
			String rz_mz_iv = tt[1];
			out.write(rz_mz_iv + "\t");
			String rz_mz_iv_ln = rz_mz_iv.equals("0")?"0": String.valueOf(Math.log(Double.parseDouble(rz_mz_iv)));
			out.write(rz_mz_iv_ln + "\t");
	
			
			//OV
			String rz_oz_ov = tt[2];
			out.write(rz_oz_ov + "\t");
			String rz_mz_ov = tt[3];
			out.write(rz_mz_ov + "\t");
			String rz_mz_ov_ln = rz_mz_ov.equals("0")?"0": String.valueOf(Math.log(Double.parseDouble(rz_mz_ov)));
			out.write(rz_mz_ov_ln + "\t");
						
			
			String OZ;
			OZ = HH_GEM[0].equals(ozs.get(Id.create(HH_GEM[0], Municipality.class)).toString()) ? "1":"0";
			out.write(OZ + "\t");

			
			// Grossreg \t  Gross_1 \t Gross_2  \t Gross_3 \t Gross_4  \t Gross_5  \t  Gross_6 \t Gross_7			
			String[] regions = {"0","0","0","0","0","0","0"};
			int region =  Integer.parseInt((String)this.householdAttributes.getAttribute(HH_NR, "region"));
			regions[region-1] = "1";	
			out.write(region +"\t");
			out.write(MyCollectionUtils.arrayToTabSeparatedString(regions) +"\t");
			
			// FS 
			String licence = ((PersonImpl)person).getLicense().equals(MZConstants.YES)?"1":"0";
			out.write(licence +"\t");
			// PW
			int PW  = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "availability: car")).equals(MZConstants.ALWAYS)?1:0;
			out.write(PW + "\t");
			//HT:
			int HT  = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: Halbtax")).equals(MZConstants.YES)?1:0;
			out.write(HT + "\t");
			//GA:
			int GA  = (((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: GA first class")).equals(MZConstants.YES)
					|| ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: GA second class")).equals(MZConstants.YES)) ?1:0;
			out.write(GA + "\t");
			//JMW:
			int JMW  = ((String)this.populationAttributes.getAttribute(person.getId().toString(), "abonnement: Verbund")).equals(MZConstants.YES) ?1:0;
			out.write(JMW + "\t");
			out.newLine();
			

		}
		
	}

	private int getIncome(String eink_imp) {
		
		int income = 0;
		
		if(eink_imp.equals("1")){
			income = 1000;
		}else if(eink_imp.equals("2")){
			income = 3000;
		}else if(eink_imp.equals("3")){
			income = 5000;
		}else if(eink_imp.equals("4")){
			income = 7000;
		}else if(eink_imp.equals("5")){
			income = 9000;
		}else if(eink_imp.equals("6")){
			income = 11000;
		}else if(eink_imp.equals("7")){
			income = 13000;
		}else if(eink_imp.equals("8")){
			income = 15000;
		}else if(eink_imp.equals("9")){
			income = 17000;
		}else{
			throw new RuntimeException("not know code for Eink_imp = "+ eink_imp);
		}
		
		return income;
	}

	private void printHeaderMNLDataFile() throws IOException {
		out.write("HH_NR \t P_NR \t W_P  \t HH_GEM \t Geschl \t Alter \t Alter_2 \t Erwerb \t Eink_imp  \t Eink  \t Eink_1000\t Eink_ln \t Bev \t Bev_1000 \t Bev_ln \t" +
				"RG_verk1 \t RG_verk2 \t RG_verk3 \t RG_verk4 \t RG_verk5 \t RG_verk6 \t RZ_OZ_IV  \t RZ_MZ_IV \t RZ_MZ_IV_ln \t RZ_OZ_OV  \t RZ_MZ_OV \t RZ_MZ_OV_ln  \t OZ \t"  +
				"Grossreg \t  Gross_1 \t Gross_2  \t Gross_3 \t Gross_4  \t Gross_5  \t  Gross_6 \t Gross_7 \t  FS \t  PW \t HT \t GA \t JMW");
		out.newLine();
		
	}



	
	private String[] getHH_GEM(String HH_NR) throws IOException {
		
	
		String HH_GEM1  = (String) this.householdAttributes.getAttribute(HH_NR, "municipality"); 
		String HH_GEM2 = HH_GEM1;
		String HH_GEM3 = HH_GEM2;
		 
		 if(mun_changes.containsKey(HH_GEM1)){
			 HH_GEM2 = mun_changes.get(HH_GEM1);
		 }else{
			 
		 }
		 
		 if(mun_changes_const.containsKey(HH_GEM1)){
			 HH_GEM3 = mun_changes_const.get(HH_GEM1);
		 }else{
			 
		 }
		 
		// HH_GEM[0] = number that appears in MZ2010 
		// HH_GEM[1] = changes up to 01.07.2011 considering eliminated municipalities  A + B = C;
		// HH_GEM[2] = changes (other way round) considering created municipalities    C = A + B;
		 
		String[] HH_GEM = {HH_GEM1, HH_GEM2, HH_GEM3};
		 return HH_GEM;
	}


	public void setInputBase(String inputBase) {
		this.inputBase = inputBase;
	}

	public void setOutputBase(String outputBase) {
		this.outputBase = outputBase;
	}
	

	public void filterPopulationOver18() throws IOException{
		
		System.out.println("Total population size: \t\t" + this.population.getPersons().size());
		
		Set<Id> ids_to_remove = new HashSet<Id>();
		
		for(Person person: this.population.getPersons().values()){
			
			if(((PersonImpl)person).getAge() < 18){
				ids_to_remove.add(person.getId());
					
			}
			
		}
		
		population.getPersons().keySet().removeAll(ids_to_remove);
		System.out.println("Total population over 18: \t\t" + this.population.getPersons().size());
		
		
		
	}
}
