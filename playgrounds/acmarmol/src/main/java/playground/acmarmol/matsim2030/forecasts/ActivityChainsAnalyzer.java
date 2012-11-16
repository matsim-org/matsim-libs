package playground.acmarmol.matsim2030.forecasts;


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.acmarmol.matsim2030.microcensus2010.MZPopulationUtils;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;
import playground.acmarmol.utils.MyCollectionUtils;


public class ActivityChainsAnalyzer {

	/**
	 * @param args
	 */
	private final static Logger log = Logger.getLogger(ActivityChainsAnalyzer.class);
	private  Population population;
	private ObjectAttributes populationAttributes;
	private ObjectAttributes householdAttributes;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		
		String populationInputFile = inputBase + "population.12.MZ2005.xml";
		String populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2005.xml";
		String householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2005.xml";
		ActivityChainsAnalyzer mz2005 = new ActivityChainsAnalyzer(populationInputFile, populationAttributesInputFile,householdAttributesInputFile);
		populationInputFile = inputBase + "population.12.MZ2010.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2010.xml";
		ActivityChainsAnalyzer mz2010 = new ActivityChainsAnalyzer(populationInputFile, populationAttributesInputFile,householdAttributesInputFile);
		
		
		mz2005.analyze();
		mz2010.analyze();
		                                 

	}


	public ActivityChainsAnalyzer(String populationInputFile, String populationAttributesInputFile, String householdAttributesInputFile){
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
		
		final int[] COHORTS = {1990,1980,19701960,1950,1940,0};
		final String[] COHORTS_STRINGS =  {"1990-1999","1980-1989","1970-1979","1960-1969","1950-1959","1940-1949","<1940"};

		
	}
	
	private void analyze() {
		
		workFromHomeStats();
		makeLengthHistogram();
		makeActivityTypeHistogram();
		
		
		
		
	}
	
	private void makeLengthHistogram(){
		
		TreeMap<Integer, Double> frequencies = new TreeMap<Integer, Double>();
		
		double size = population.getPersons().size();
		for(Person person:population.getPersons().values()){
			
			Plan plan = person.getSelectedPlan();
			int length = getChainLength(plan);
			
			if(!frequencies.containsKey(length)){
				frequencies.put(length, 100.0/size);
			}else{
				double total = frequencies.get(length);
				frequencies.put(length, total+100.0/size);
			}
						
		}
		

		MyCollectionUtils.printMap(frequencies);
		
	}
	
	private void makeActivityTypeHistogram(){
		
		TreeMap<String, Double> frequencies = new TreeMap<String, Double>();
		
		for(Person person:population.getPersons().values()){
			
			Plan plan = person.getSelectedPlan();
			if(plan!=null){
				List<PlanElement> planElements = plan.getPlanElements();
				for (PlanElement pe : planElements) {
					if (pe instanceof Activity){
						
					String type = ((Activity) pe).getType();	
						if(!frequencies.containsKey(type)){
							frequencies.put(type, 1.0);
						}else{
							double cum = frequencies.get(type);
							frequencies.put(type, cum+1.0);
						}
						
					}									
				}		
			}
		}
	
		
		MyCollectionUtils.printMap(frequencies);
		
	}
	
	private int getChainLength(Plan plan){
		
		int total=0;
		
		if(plan!=null){
			List<PlanElement> planElements = plan.getPlanElements();
			for (PlanElement pe : planElements) {
				if (pe instanceof Activity)
					total++;			
			}		
		}		
		return total;
	}
	
	public void workFromHomeStats(){
		
		double cum = 0;
		double sum_weight=0;
		
		for(Person person:population.getPersons().values()){

			double pw = Double.parseDouble((String) this.populationAttributes.getAttribute(person.getId().toString(), "person weight"));
			Coord workCoord = (CoordImpl)this.populationAttributes.getAttribute(person.getId().toString(), "work: location coord");
			Coord homeCoord = (CoordImpl) this.householdAttributes.getAttribute((String)this.populationAttributes.getAttribute(person.getId().toString(), "household number"), "coord");
			
			if(homeCoord.equals(workCoord))			
				cum+= pw;
			
			sum_weight+= pw;		
		}
		
		log.info("Total working from home: " + cum/sum_weight*100 + "%");
	}

	
	
	
	
	
}



		
	

