package playground.vsp.openberlinscenario.cemdap.input;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.openberlinscenario.Gender;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author GabrielT on 15.11.2016.
 */
public class SynPopCreatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void TestGenerateDemand() {

		// Input and output files
		String commuterFileOutgoingTest = utils.getInputDirectory() + "Teil1BR2009Ga_Test_kurz.txt";
		String censusFile = utils.getInputDirectory() + "Zensus11_Datensatz_Bevoelkerung_BE_BB.csv";

		String[] commuterFilesOutgoing = {commuterFileOutgoingTest};

		// Parameters
		int numberOfPlansPerPerson = 1;
		List<String> idsOfFederalStatesIncluded = Arrays.asList("12");
		double defaultAdultsToEmployeesRatio = 1.23;  // Calibrated based on sum value from Zensus 2011.
		double defaultEmployeesToCommutersRatio = 2.5;  // This is an assumption, oriented on observed values, deliberately chosen slightly too high.

		SynPopCreator demandGeneratorCensus = new SynPopCreator(commuterFilesOutgoing, censusFile, utils.getOutputDirectory(),
				numberOfPlansPerPerson, idsOfFederalStatesIncluded, defaultAdultsToEmployeesRatio, defaultEmployeesToCommutersRatio);

		demandGeneratorCensus.setShapeFileForSpatialRefinement(utils.getInputDirectory() + "Bezirksregion_EPSG_25833.shp");
		demandGeneratorCensus.setIdsOfMunicipalitiesForSpatialRefinement(Arrays.asList("11000000"));
		demandGeneratorCensus.setRefinementFeatureKeyInShapefile("SCHLUESSEL");

		demandGeneratorCensus.generateDemand();


		String municipal = "Breydin";
		ArrayList<String> possibleLocationsOfWork = readPossibleLocationsOfWork(commuterFileOutgoingTest, municipal);

		String[] municipalLine = getCensusDataLine(censusFile, municipal);

		int male18_24Ref = parseInt(municipalLine[85]);
		int female18_24Ref = parseInt(municipalLine[86]);
		int male25_29Ref = parseInt(municipalLine[88]);
		int female25_29Ref = parseInt(municipalLine[89]);
		int male30_39Ref = parseInt(municipalLine[91]);
		int female30_39Ref = parseInt(municipalLine[92]);
		int male40_49Ref = parseInt(municipalLine[94]);
		int female40_49Ref = parseInt(municipalLine[95]);
		int male50_64Ref = parseInt(municipalLine[97]);
		int female50_64Ref = parseInt(municipalLine[98]);
		int male65_74Ref = parseInt(municipalLine[100]);
		int female65_74Ref = parseInt(municipalLine[101]);
		int male75PlusRef = parseInt(municipalLine[103]);
		int female75PlusRef = parseInt(municipalLine[104]);

		int male18_24 = 0;
		int female18_24 = 0;
		int male25_29 = 0;
		int female25_29 = 0;
		int male30_39 = 0;
		int female30_39 = 0;
		int male40_49 = 0;
		int female40_49 = 0;
		int male50_64 = 0;
		int female50_64 = 0;
		int male65_74 = 0;
		int female65_74 = 0;
		int male75Plus = 0;
		int female75Plus = 0;

		Population pop = demandGeneratorCensus.getPopulation();
		for (Person person : pop.getPersons().values()) {
			//collect data
			String locationOfWork = (String) person.getAttributes().getAttribute("locationOfWork");
			boolean employed = (boolean) person.getAttributes().getAttribute("employed");
			int age = (Integer) person.getAttributes().getAttribute("age");
			Gender gender = Gender.valueOf((String) person.getAttributes().getAttribute("gender")); // assumes that female = 1

			//assert
//			Assert.assertEquals("Wrong municipality", "12060034", householdId.toString().substring(0,8));
			if (!employed) {
				Assertions.assertEquals("-99", locationOfWork, "Wrong locationOfWork");
			} else if (locationOfWork.length() != 6) {
				Assertions.assertTrue(possibleLocationsOfWork.contains(locationOfWork), "Wrong locationOfWork");
			}
			if (gender == Gender.male) {
				if (isBetween(age, 18, 24)) male18_24++;
				if (isBetween(age, 25, 29)) male25_29++;
				if (isBetween(age, 30, 39)) male30_39++;
				if (isBetween(age, 40, 49)) male40_49++;
				if (isBetween(age, 50, 64)) male50_64++;
				if (isBetween(age, 65, 74)) male65_74++;
				if (age > 74) male75Plus++;
			} else if (gender == Gender.female){
				if (isBetween(age, 18, 24)) female18_24++;
				if (isBetween(age, 25, 29)) female25_29++;
				if (isBetween(age, 30, 39)) female30_39++;
				if (isBetween(age, 40, 49)) female40_49++;
				if (isBetween(age, 50, 64)) female50_64++;
				if (isBetween(age, 65, 74)) female65_74++;
				if (age > 74) female75Plus++;
			} else Assertions.fail("Wrong gender");
		}

		//System.out.println("Persons size: " + pop.getPersons().values().size());

		Assertions.assertEquals(male18_24Ref, male18_24, "Wrong male18_24 count");
		Assertions.assertEquals(male25_29Ref, male25_29, "Wrong male25_29 count");
		Assertions.assertEquals(male30_39Ref, male30_39, "Wrong male30_39 count");
		Assertions.assertEquals(male40_49Ref, male40_49, "Wrong male40_49 count");
		Assertions.assertEquals(male50_64Ref, male50_64, "Wrong male50_64 count");
		Assertions.assertEquals(male75PlusRef, male75Plus, "Wrong male75Plus count");
		Assertions.assertEquals(female18_24Ref, female18_24, "Wrong female18_24 count");
		Assertions.assertEquals(female25_29Ref, female25_29, "Wrong female25_29 count");
		Assertions.assertEquals(female30_39Ref, female30_39, "Wrong female30_39 count");
		Assertions.assertEquals(female40_49Ref, female40_49, "Wrong female40_49 count");
		Assertions.assertEquals(female50_64Ref, female50_64, "Wrong female50_64 count");
		Assertions.assertEquals(female65_74Ref, female65_74, "Wrong female65_74 count");
		Assertions.assertEquals(female75PlusRef, female75Plus, "Wrong female75Plus count");
		Assertions.assertEquals(male65_74Ref, male65_74, "Wrong male65_74 count");

		Assertions.assertTrue(new File(utils.getOutputDirectory() + "persons1.dat.gz").exists(), "");

	}

	private boolean isBetween(int x, int lower, int upper) {
		return lower <= x && x <= upper;
	}

	private ArrayList<String> readPossibleLocationsOfWork(String commuterFileOutgoingTest, String municipal) {
		ArrayList<String> result = new ArrayList<>();

		CSVReader reader = new CSVReader(commuterFileOutgoingTest, "\t");
		String[] line = reader.readLine();
		while (line.length < 2 || !Objects.equals(line[1], municipal)) line = reader.readLine();
		line = reader.readLine();
		while (Objects.equals(line[0], "")) {
			if (line[2].length() == 8) {
				result.add(line[2]);
			}
			line = reader.readLine();
		}
		return result;
	}

	private int parseInt(String value) {
		if (value.startsWith("(")) {
			value = value.substring(1, value.length()-1);
		}
		return Integer.parseInt(value);
	}

	private String[] getCensusDataLine(String censusFile, String municipal) {
		CSVReader reader = new CSVReader(censusFile, ";");
		String[] line = reader.readLine();
		while (!Objects.equals(line[6], municipal)) {
			line = reader.readLine();
		}
		return line;
	}
}
