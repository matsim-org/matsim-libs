package playground.mzilske.prognose2025;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mzilske.pipeline.PopulationWriterTask;

public abstract class CreatePendlerDemand {

	private static final String NETWORK_FILENAME = "../../shared-svn/studies/countries/de/prognose_2025/osm_zellen/motorway_germany.xml";

	private static final String LANDKREISE = "../../shared-svn/studies/countries/de/prognose_2025/osm_zellen/landkreise.shp";

	private static final String OUTPUT_POPULATION_FILENAME = "../../detailedEval/pop/pendlerVerkehr/pendlermatrizen/inAndOut/pendlerverkehr_10pct_scaledAndMode_workStartingTimePeak0800Var2h_dhdn_gk4.xml.gz";

	public static void main(String[] args) {
		Scenario osmNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(osmNetwork).readFile(NETWORK_FILENAME);		
		PersonVerschmiererTask personVerschmiererTask = new PersonVerschmiererTask(LANDKREISE);	
		PopulationWriterTask populationWriter = new PopulationWriterTask(OUTPUT_POPULATION_FILENAME, osmNetwork.getNetwork());
		PopulationGenerator populationBuilder = new PopulationGenerator();		
		TravelTimeToWorkCalculator routerFilter = new TravelTimeToWorkCalculator(osmNetwork.getNetwork());
		PendlerMatrixReader pvMatrixReader = new PendlerMatrixReader();
		pvMatrixReader.setFlowSink(routerFilter);
		routerFilter.setSink(populationBuilder);
		populationBuilder.setSink(personVerschmiererTask);
		personVerschmiererTask.setSink(populationWriter);		
		pvMatrixReader.run();
		System.err.println("some landkreises do not work because of gebietsreform; check!") ;
	}

}
