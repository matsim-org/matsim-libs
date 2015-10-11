package gunnar.ihop2.regent.demandreading;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class DemandAnalyzer {

	public DemandAnalyzer() {
	}

	public static void run(final String demandFile) {

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

		final Set birthyears = new LinkedHashSet();
		final Set sexes = new LinkedHashSet();
		final Set incomes = new LinkedHashSet();
		final Set housingtypes = new LinkedHashSet();
		final Set homezones = new LinkedHashSet();
		final Set workzones = new LinkedHashSet();
		final Set otherzones = new LinkedHashSet();
		final Set worktourmodes = new LinkedHashSet();
		final Set othertourmodes = new LinkedHashSet();

		for (String personId : ObjectAttributeUtils2
				.allObjectKeys(personAttributes)) {
			birthyears.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.BIRTHYEAR_ATTRIBUTE));
			sexes.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.SEX_ATTRIBUTE));
			incomes.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.INCOME_ATTRIBUTE));
			housingtypes.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.HOUSINGTYPE_ATTRIBUTE));
			homezones.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.HOMEZONE_ATTRIBUTE));
			workzones.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.WORKZONE_ATTRIBUTE));
			otherzones.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.OTHERZONE_ATTRIBUTE));
			worktourmodes.add(personAttributes.getAttribute(personId,
					RegentPopulationReader.WORKTOURMODE_ATTRIBUTE));
			othertourmodes.add(personAttributes.getAttribute(personId,
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
		// run("./data/synthetic_population/150911_trips.xml");

	}

}
