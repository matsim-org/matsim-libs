package cemdap4wob.planspreprocessing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class CreateSubpopulations {

	public static void main(String[] args) {
		new CreateSubpopulations().run("/Users/jb/Desktop/cemdap-vw/mergedPlans_filtered_1.0_attr.xml.gz", "/Users/jb/Desktop/cemdap-vw/mergedPlans_filtered_1.0_attr_oA,xml");
	}
	
	public void run (String populationFile, String subpopulationFile) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
		spr.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				if (isBs(person.getId())) {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "livesBs");
				}
				else if (isWob(person.getId())) {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "livesWob");

				} else 	scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "subpopulation", "default");

			}
		});
		spr.readFile(populationFile);	
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(subpopulationFile);
	}
	
	private boolean isBs(Id<Person> id) {
		if ((id.toString().startsWith("1"))&&(id.toString().split("_")[0].length()==3)) return true;
		else return false;

	}
	
	private boolean isWob(Id<Person> id) {
		if ((id.toString().startsWith("3"))&&(id.toString().split("_")[0].length()==3)) return true;
		else return false;
		
	}
	

}
