package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate;


import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;



public class MicrocensusV2 extends Microcensus{

	private ObjectAttributes householdpersonsAttributes;
	
	
	
	public MicrocensusV2(String populationInputFile, String householdInputFile, String populationAttributesInputFile, String householdAttributesInputFile, String householdpersonsAttributesInputFile,int year){
		
		super(populationInputFile,householdInputFile,populationAttributesInputFile, householdAttributesInputFile,year);
		this.householdpersonsAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader readerHHP = new ObjectAttributesXmlReader(householdpersonsAttributes);
		readerHHP.parse(householdpersonsAttributesInputFile);
		
		
		
	}
	
	public ObjectAttributes getHouseholdPersonsAttributes() {
		return householdpersonsAttributes;
	}
	
}
