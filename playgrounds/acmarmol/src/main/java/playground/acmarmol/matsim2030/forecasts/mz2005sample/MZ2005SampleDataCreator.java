package playground.acmarmol.matsim2030.forecasts.mz2005sample;

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

public class MZ2005SampleDataCreator {
	

	private Population population;
	private ObjectAttributes populationAttributes;
	private ObjectAttributes householdAttributes;
	private BufferedWriter out;
//	private TreeMap<String, Integer> mun_totals;
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
		
		String populationFile = inputBase + "population.09.MZ2005.xml";
		String populationAttFile = inputBase + "populationAttributes.04.MZ2005.xml";
		String householdAttFile = inputBase + "householdAttributes.04.MZ2005.xml";		
		
		MZ2005SampleDataCreator mz2005 = new MZ2005SampleDataCreator(populationFile, populationAttFile, householdAttFile, 2005);
		mz2005.setInputBase(inputBase);
		mz2005.setOutputBase(outputBase);
		
		mz2005.loadData();
		mz2005.createDataForRegressionModels(outputBase + "mz2005SampleForRegressionModels.dat");
		
	}

	private void loadData() throws IOException {
		
//		this.mun_totals = Loader.loadMunicipalityTotals(outputBase + "municipality_population.txt");
		this.mun_changes = Loader.loadEliminatedMunicipalitiesDatabase( inputBase + "municipalities/eliminated municipalities 01.01.2005 - 01.07.2011.txt");
		this.mun_changes_const = Loader.loadCreatedMunicipalitiesDatabase2(inputBase +"municipalities/nuovi costituzione 01.01.2000-31.12.2010.txt");
		this.gemeinde_typen = Loader.loadGemeindetypologieARE(inputBase + "Gemeindetypen_ARE_2010_VZ.txt");
		ArrayList<HashMap<Id<Municipality>, Id<Municipality>>> MZandOZ = Loader.loadOZandMZDatabase(inputBase + "updated Gemeindezuordnung MZ+OZ_ARE.txt");
		this.ozs = MZandOZ.get(0);
		this.mzs = MZandOZ.get(1);
		this.travel_times_IV = Loader.loadTravelTimesToMZandOZ(inputBase + "municipalities_TT0_01-DWV_2005MIV_Masternetz_LKW_Kalibriert2010_A1_AFTER_MATLAB.txt", MZandOZ);
		this.travel_times_OV = Loader.loadTravelTimesToMZandOZ(inputBase + "municipalities_TT0_02-OEV_DWV_2005-HAFAS_A1_AFTER_MATLAB.txt", MZandOZ);
		this.ozToGrossZtr = Loader.loadOZtoGrossZtr(inputBase + "GrossZtr10.txt");
	}



	public MZ2005SampleDataCreator(String populationInputFile, String populationAttFile, String householdAttFile, int year){
		
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
			
			if(!HH_GEM[0].equals("-97")){//avoid 0.2% of households without specified location
				
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
					if(etappen!=null){	//person with all plan with "undefined" etappen	
						for(Etappe  etappe: etappen){
							
							int index = getModeType(etappe.getMode());
							
							fahrtzeiten[index] += Double.parseDouble(etappe.getDuration());
							tagesdistanzen[index] += Double.parseDouble(etappe.getDistance());
						}
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
				
				out.write(person.getId() + "\t" + kohortgr + "\t" +	Geschl +	"\t" + abo_ges + "\t" + fs_pw_zp + "\t" + rz_mz_iv + " \t" + rz_mz_ov + "\t" + rz_oz_iv  + "\t" +	rz_oz_ov  + "\t" + ver_pw+   "\t" +  this.year + "\t "  
						 + mzjahr2 + "\t" +	lAlter + "\t" +	erwerb+ "\t" + grossZtr +"\t" + RG_verkGr + "\t" + 	t_langs + "\t" +	t_pw + "\t" + t_oev  + "\t " +
						t_sonst + "\t" + d_langs + "\t" +   d_pw + "\t" + d_oev + "\t" + d_sonst);
				out.newLine();	

			}	
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
		}else if(code.equals("1060")||code.equals("1061")){//luzern-littau
			return 4;
		}else if(code.equals("2701")){//basel
			return 5;
		}else if(code.equals("3203")){//st. gallen
			return 6;
		}else if(code.equals("5147")||code.equals("5192")||code.equals("5168")||code.equals("5235")){//lugano
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
		out.write("pid \t kohortgr \t gesch \t abo_ges \t fs_pw_zp  \t iv_t \t oev_t \t iv_oz_t \t oev_oz_t \t verpw_zp\t mzjahr \t " + 
				"mzjahr2 \t lalter \t erwerb \t grossztr \t rg_verk2 \t t_langs \t t_pw \t t_oev \t" +
				"t_sonst \t d_langs \t d_pw \t d_oev \t d_sonst");
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
