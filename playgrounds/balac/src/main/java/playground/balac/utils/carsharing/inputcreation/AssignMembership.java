package playground.balac.utils.carsharing.inputcreation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;




public class AssignMembership extends MatsimXmlWriter {
	
	private Scenario scenario;
	
	public AssignMembership(Scenario scenario) {
		this.scenario = scenario;
	}
	
	public void write(String file) {
		openFile(file);
		
		writeXmlHead();
		
		writeStartTag("membership", null);
		writeMembership();
		writeEndTag("membership");

		close();
	}
	
	private void writeMembership() {
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {			
			
			writePerson(person);
			
		}
	}
	
	private void writePerson(Person person) {
		List<Tuple<String, String>> attsP = new ArrayList<Tuple<String, String>>();
		
		attsP.add(new Tuple<>("id", person.getId().toString()));
		
		List<Tuple<String, String>> attsC = new ArrayList<Tuple<String, String>>();
		
		attsC.add(new Tuple<>("id", "Catchacar"));
		
		List<Tuple<String, String>> attsF = new ArrayList<Tuple<String, String>>();

		attsF.add(new Tuple<>("name", "freefloating"));

		writeStartTag("person", attsP);
		writeStartTag("company", attsC);
		writeStartTag("carsharing", attsF, true);
		writeEndTag("company");
		writeEndTag("person");

	}
	

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		
		MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
		
		MatsimReader populationReader = new PopulationReader(scenario);		
		populationReader.readFile(args[0]);	
		
		AssignMembership as = new AssignMembership(scenario);
		as.write(args[1]);
		
	}

}
