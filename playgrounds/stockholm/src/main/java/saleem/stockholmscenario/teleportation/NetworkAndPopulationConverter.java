package saleem.stockholmscenario.teleportation;

public class NetworkAndPopulationConverter {
	public static void main(String[] args) {
		String path = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\initial_plans_car_and_pt_onepercent.xml";
		String path1 = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\initial_plans_car_and_pt_onepercent_modified.xml";//UTM + Scaling done
		XMLReaderWriter xml = new XMLReaderWriter();
		//xml.writeDocumentInXML(xml.modifyNetwork(xml.readFile(path)), path1);//Read, modify and save Network
		//xml.writeDocumentInXML(xml.modifyPopulation(xml.readFile(path)), path1);//Read, modify and save Population
	}
}
