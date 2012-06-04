/**
 * 
 */
package playground.qiuhan.sa;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

/**
 * @author Q. SUN
 * 
 */
public class MatrixToPopulation {
	Population pop;

	public MatrixToPopulation(Scenario scenario) {
		this.pop = new PopulationImpl((ScenarioImpl) scenario);
	}

	public void readMatrices(String matricesPath,
			Map<String, Coord> zoneIdCoords) {
		// output/matrices/1.mtx
		for (int i = 1; i <= 24; i++) {
			Matrix smallM = new Matrix(Integer.toString(i), "[matrix from\t"
					+ (i - 1) + "\tto\t" + i + "]");
			new VisumMatrixReader(smallM).readFile(matricesPath
					+ Integer.toString(i) + ".mtx");

			// this.smallMs.put(i, smallM);

			Map<Id, Person> persons = new MatrixToPersons(smallM, zoneIdCoords)
					.getPersons();
			for (Person per : persons.values()) {
				this.pop.addPerson(per);
			}
		}
	}

	public void writePopulation(String populationFilename, Network network) {
		new PopulationWriter(pop, network).write(populationFilename);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO read 24 matrices to population and write a population file
		String matricesPath = "output/matrices/";
		String zoneFilename = "output/matsimNetwork/Zone.log";
		String networkFilename = "output/matsimNetwork/networkBerlin.xml";
		String outputPopulationFilename = "output/population/pop.xml";

		ZoneReader zones = new ZoneReader();
		zones.readFile(zoneFilename);
		Map<String, Coord> zoneIdCoords = zones.getZoneIdCoords();

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		MatrixToPopulation mtp = new MatrixToPopulation(scenario);
		mtp.readMatrices(matricesPath, zoneIdCoords);
		mtp.writePopulation(outputPopulationFilename, scenario.getNetwork());
	}
}
