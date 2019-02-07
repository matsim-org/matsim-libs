package ft.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.math3.util.Precision;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.router.TransitActsRemover;

public class GetMobilityPatternPerGroup {

	static Scenario scenario = null;
	static Map<String, MutableInt> personGroupMap = new HashMap<>();
	static Map<String, Map<String, MutableInt>> PersonGroupActivityChainSet = new HashMap<>();

	public static void main(String[] args) throws IOException {
		GetMobilityPatternPerGroup.run();
	}

	public static void run() throws IOException {

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("C:\\Users\\VWBIDGN\\Desktop\\finishedPlans.xml.gz");

		Population population = scenario.getPopulation();

		personGroupMap.put("student", new MutableInt());
		personGroupMap.put("employed", new MutableInt());
		personGroupMap.put("unemployed", new MutableInt());
		personGroupMap.put("undefined", new MutableInt());
		personGroupMap.put("retired", new MutableInt());

		// Initialize PersonGroupActivityChainSet
		for (String personGroup : personGroupMap.keySet()) {
			PersonGroupActivityChainSet.put(personGroup, new HashMap<String, MutableInt>());
		}

		for (Entry<Id<Person>, ? extends Person> personEntry : population.getPersons().entrySet()) {
			Person person = personEntry.getValue();
			String personStatus = decidePerson(person);

			String actChain = getActivityChain(person);

			Map<String, MutableInt> personGroup = PersonGroupActivityChainSet.get(personStatus);

			if (!personGroup.containsKey(actChain)) {
				personGroup.put(actChain, new MutableInt(1));
			} else {
				personGroup.get(actChain).increment();
				;
			}

		}

		writeActivityChainPattern("C:\\Users\\VWBIDGN\\Desktop\\test", PersonGroupActivityChainSet);
	}

	public static String decidePerson(Person person)

	{
		int age;
		age = (int) person.getAttributes().getAttribute("age");
		if (hasActivity(person, "education")) {
			personGroupMap.get("student").increment();
			return "student";
		}

		else if (person.getAttributes().getAttribute("employed") != null) {
			if ((boolean) person.getAttributes().getAttribute("employed") == false && age >= 65) {
				personGroupMap.get("retired").increment();
				return "retired";
			}

			else if ((boolean) person.getAttributes().getAttribute("employed") == true) {
				personGroupMap.get("employed").increment();
				return "employed";
			}

			else if ((boolean) person.getAttributes().getAttribute("employed") == false && age < 65) {
				personGroupMap.get("unemployed").increment();
				return "unemployed";
			} else {
				personGroupMap.get("undefined").increment();
				return "undefined";
			}
		}

		else {
			personGroupMap.get("undefined").increment();
			return "undefined";
		}
	}

	public static String getActivityChain(Person person) {

		Plan plan = person.getSelectedPlan();

		new TransitActsRemover().run(plan);

		StringJoiner joiner = new StringJoiner("-");
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				String baseType = act.getType().split("_")[0];
				joiner.add(baseType);
			}
		}

		return joiner.toString();

	}

	public static void writeActivityChainPattern(String outfolder,
			Map<String, Map<String, MutableInt>> PersonGroupActivityChainSet) throws IOException {

		String sep = "\t";

		for (String personGroup : PersonGroupActivityChainSet.keySet()) {

			String personFilename = outfolder + "\\" + personGroup + ".txt";
			BufferedWriter bw = IOUtils.getBufferedWriter(personFilename);

			String header = "chainType" + sep + "absFreq" + sep +"relFreq";
			
			bw.append(header);
			bw.newLine();
			
			Map<String, MutableInt> actFreqMap = PersonGroupActivityChainSet.get(personGroup);
			
			int sum = actFreqMap.values().stream().mapToInt(Number::intValue).sum();

			for (String actChain : actFreqMap.keySet()) {
				double prop = Precision.round(actFreqMap.get(actChain).doubleValue() / (double) sum, 3);

				String writeEntry = actChain + sep + actFreqMap.get(actChain) + sep + prop;

				try {
					bw.append(writeEntry);
					bw.newLine();
					bw.flush();

				} catch (IOException e) {
					e.printStackTrace();

				}

			}
			bw.close();

		}
	}

	public static boolean hasActivity(Person person, String activityType) {

		List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan().getPlanElements(),
				EmptyStageActivityTypes.INSTANCE);

		for (Activity act : activities) {

			if (act.getType().contains(activityType)) {
				return true;
			}
		}

		return false;
	}

}
