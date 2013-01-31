package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;

public class Microcensus {

	private Population population;
	private Households households;
	private ObjectAttributes populationAttributes;
	private ObjectAttributes householdAttributes;
	private int year;
	
	
	public Microcensus(String populationInputFile, String householdInputFile, String populationAttributesInputFile, String householdAttributesInputFile, int year){
		
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseHouseholds(true);
		config.setParam("plans", "inputPlansFile", populationInputFile);
		config.setParam("households", "inputFile", householdInputFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		this.population = scenario.getPopulation();
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		this.populationAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(populationAttributes);
		reader.putAttributeConverter(CoordImpl.class, new CoordConverter());
		reader.parse(populationAttributesInputFile);
		this.householdAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader readerHH = new ObjectAttributesXmlReader(householdAttributes);
		readerHH.putAttributeConverter(CoordImpl.class, new CoordConverter());
		readerHH.parse(householdAttributesInputFile);
		this.setYear(year);
		
		
		
		
	}
	
	public Population getPopulation() {
		return population;
	}
	

	public Households getHouseholds() {
		return households;
	}
	
	
	public ObjectAttributes getPopulationAttributes() {
		return populationAttributes;
	}
	
	
	public ObjectAttributes getHouseholdAttributes() {
		return householdAttributes;
	}

	public int getYear() {
		return year;
	}

	private void setYear(int year) {
		this.year = year;
	}
	

	
}
