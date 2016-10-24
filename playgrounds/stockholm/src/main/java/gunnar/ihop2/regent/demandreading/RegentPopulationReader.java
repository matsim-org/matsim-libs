package gunnar.ihop2.regent.demandreading;

import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

@Deprecated
public class RegentPopulationReader {

	// CONSTANTS

	public static final String BIRTHYEAR_ATTRIBUTE = "birthyear";

	public static final String SEX_ATTRIBUTE = "sex";

	public static final String INCOME_ATTRIBUTE = "income";

	public static final String HOUSINGTYPE_ATTRIBUTE = "housingtype";

	public static final String HOMEZONE_ATTRIBUTE = "homezone";

	public static final String WORKZONE_ATTRIBUTE = "workzone";

	public static final String OTHERZONE_ATTRIBUTE = "otherzone";

	public static final String WORKTOURMODE_ATTRIBUTE = "worktourmode";

	public static final String OTHERTOURMODE_ATTRIBUTE = "othertourmode";

	public static final String CAR_ATTRIBUTEVALUE = "Car";

	public static final String PT_ATTRIBUTEVALUE = "PublicTransport";

	// MEMBERS

	final ObjectAttributes personAttributes;

	// final Map<String, RegentPerson> id2person;

	// final Map<String, Zone> id2usedZone;

	// CONSTRUCTION

	public RegentPopulationReader(final String populationFileName,
			final ObjectAttributes personAttributes) {
		this(populationFileName, null, personAttributes);
	}

	@Deprecated
	public RegentPopulationReader(final String populationFileName,
			final ZonalSystem zonalSystem,
			final ObjectAttributes personAttributes) {

		// int complete = 0;
		// int incomplete = 0;

		this.personAttributes = personAttributes;
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				personAttributes);
		reader.readFile(populationFileName);

		// final Set<String> housingTypes = new LinkedHashSet<String>();
		// final Set<String> workTourModes = new LinkedHashSet<String>();

		// this.id2person = new LinkedHashMap<String, RegentPerson>();
		// this.id2usedZone = new LinkedHashMap<String, Zone>();
		for (String personId : ObjectAttributeUtils2
				.allObjectKeys(personAttributes)) {

			final String homeZone = (String) personAttributes.getAttribute(
					personId, HOMEZONE_ATTRIBUTE);
			final String workZone = (String) personAttributes.getAttribute(
					personId, WORKZONE_ATTRIBUTE);

			// final String income = (String) personAttributes.getAttribute(
			// personId, INCOME_ATTRIBUTE);
			// final String sex = (String)
			// personAttributes.getAttribute(personId,
			// SEX_ATTRIBUTE);
			// final String birthYear = (String) personAttributes.getAttribute(
			// personId, BIRTHYEAR_ATTRIBUTE);
			// final RegentPerson person = new RegentPerson(personId, homeZone,
			// workZone, workTourMode, income, sex, housingType, birthYear);

			// housingTypes.add((String) personAttributes.getAttribute(personId,
			// HOUSINGTYPE_ATTRIBUTE));
			// workTourModes.add((String)
			// personAttributes.getAttribute(personId,
			// WORKTOURMODE_ATTRIBUTE));

			// if (person.isComplete()) {
			// complete++;
			// } else {
			// incomplete++;
			// System.out.println(person);
			// }

			// this.id2person.put(personId, person);

			// this.id2usedZone.put(homeZone, zonalSystem.getZone(homeZone));
			// this.id2usedZone.put(workZone, zonalSystem.getZone(workZone));
		}

		// System.out.println("COMPLETE PERSONS: " + complete);
		// System.out.println("INCOMPLETE PERSONS: " + incomplete);

		// System.out.println("HOUSING TYPES: " + housingTypes);
		// System.out.println("WORKTOURMODES: " + workTourModes);

	}

	// MAIN-FUNCTION, ONLY FOR TESTING
	//
	// public static void main(String[] args) {
	//
	// System.out.println("STARTED ...");
	//
	// final String zonesShapefile = "./data/shapes/sverige_TZ_EPSG3857.shp";
	// final ZonalSystem zonalSystem = new ZonalSystem(zonesShapefile);
	// final String regentPopulationFileName = "./150410_worktrips_small.xml";
	// final RegentPopulationReader reader = new RegentPopulationReader(
	// "./150410_worktrips_small.xml", zonalSystem, null);
	//
	// System.out.println("... DONE");
	//
	// }

}
