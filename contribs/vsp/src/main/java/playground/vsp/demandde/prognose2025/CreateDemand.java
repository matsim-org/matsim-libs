package playground.vsp.demandde.prognose2025;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.demandde.pendlermatrix.PersonVerschmiererTask;
import playground.vsp.demandde.pendlermatrix.PopulationGenerator;
import playground.vsp.demandde.pendlermatrix.TravelTimeToWorkCalculator;
import playground.vsp.pipeline.PopulationWriterTask;

/**
 * @author nagel
 *
 */
public class CreateDemand {
	private CreateDemand(){}

	private static final String NETWORK_FILENAME = "../../shared-svn/studies/countries/de/prognose_2025/osm_zellen/motorway_germany.xml";

	private static final String LANDKREISE = "../../shared-svn/studies/countries/de/prognose_2025/osm_zellen/landkreise.shp";

	private static final String OUTPUT_POPULATION_FILENAME = "/Users/nagel/kw/pop.xml.gz";

	public static void main(String[] args) {
		Scenario osmNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(osmNetwork.getNetwork()).readFile(NETWORK_FILENAME);
		
		PersonVerschmiererTask personVerschmiererTask = new PersonVerschmiererTask(LANDKREISE);	
		PopulationWriterTask populationWriter = new PopulationWriterTask(OUTPUT_POPULATION_FILENAME, osmNetwork.getNetwork());
		PopulationGenerator populationBuilder = new PopulationGenerator();		
		TravelTimeToWorkCalculator routerFilter = new TravelTimeToWorkCalculator(osmNetwork.getNetwork());
		DemandMatrixReader pvMatrixReader = new DemandMatrixReader(LANDKREISE);

		pvMatrixReader.setFlowSink(routerFilter);
		routerFilter.setSink(populationBuilder);
		populationBuilder.setSink(personVerschmiererTask);
		personVerschmiererTask.setSink(populationWriter);		
		pvMatrixReader.run();

		System.err.println("some landkreises do not work because of gebietsreform; check!") ;
	}

}
