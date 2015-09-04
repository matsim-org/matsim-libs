package gunnar.ihop2.regent.demandreading;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class DemandAnalyzer {

	public DemandAnalyzer() {
	}

	static void run(final String demandFile) {

		final ObjectAttributes personAttributes = new ObjectAttributes();
		(new ObjectAttributesXmlReader(personAttributes)).parse(demandFile);

		System.out.println();
		System.out.println("FILE SUMMARY " + demandFile);
		System.out.println();
		System.out.println(ObjectAttributeUtils2
				.allObjectKeys(personAttributes).size()
				+ " distinct object IDs");
		System.out.println("all distinct attributes: "
				+ ObjectAttributeUtils2.allAttributeKeys(personAttributes));
		System.out.println();

		final Set<String> birthyears = new LinkedHashSet<String>();
		final Set<String> sexes = new LinkedHashSet<String>();
		final Set<String> incomes = new LinkedHashSet<String>();
		final Set<String> housingtypes = new LinkedHashSet<String>();
		final Set<String> homezones = new LinkedHashSet<String>();
		final Set<String> workzones = new LinkedHashSet<String>();
		final Set<String> otherzones = new LinkedHashSet<String>();
		final Set<String> worktourmodes = new LinkedHashSet<String>();
		final Set<String> othertourmodes = new LinkedHashSet<String>();

		for (String personId : ObjectAttributeUtils2
				.allObjectKeys(personAttributes)) {
			birthyears.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.BIRTHYEAR_ATTRIBUTE));
			sexes.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.SEX_ATTRIBUTE));
			incomes.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.INCOME_ATTRIBUTE));
			housingtypes.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE));
			homezones.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.HOMEZONE_ATTRIBUTE));
			workzones.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.WORKZONE_ATTRIBUTE));
			otherzones.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.OTHERZONE_ATTRIBUTE));
			worktourmodes.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.WORKTOURMODE_ATTRIBUTE));
			othertourmodes.add((String) personAttributes.getAttribute(personId,
					RegentPopulationReader.OTHERTOURMODE_ATTRIBUTE));
		}

		System.out.println(birthyears.size() + " distinct birthyears");
		System.out.println("sexes: " + sexes);
		System.out.println(incomes.size() + " distinct incomes");
		System.out.println("housingtypes: " + housingtypes);
		System.out.println(homezones.size() + " distinct homezones");
		System.out.println(workzones.size() + " distinct workzones");
		System.out.println(otherzones.size() + " distinct otherzones");
		System.out.println("worktourmodes: " + worktourmodes);
		System.out.println("othertourmodes: " + othertourmodes);
		System.out.println(homezones);

	}

	public static void main(String[] args) {

		run("./data/synthetic_population/150615_trips.xml");

	}

}
