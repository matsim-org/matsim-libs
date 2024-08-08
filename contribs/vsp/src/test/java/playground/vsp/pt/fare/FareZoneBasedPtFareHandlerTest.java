package playground.vsp.pt.fare;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.matsim.application.ApplicationUtils.globFile;

public class FareZoneBasedPtFareHandlerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testFareZoneBasedPtFareHandler() {

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(0);

		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
		ptFareConfigGroup.setPtFareCalculationModel(PtFareConfigGroup.PtFareCalculationModels.fareZoneBased);

		DistanceBasedPtFareParams fareParams = ConfigUtils.addOrGetModule(config, DistanceBasedPtFareParams.class);
		fareParams.setFareZoneShp(IOUtils.extendUrl(context, "ptTestArea/pt-area.shp").toString());


		ScoringConfigGroup scoring = ConfigUtils.addOrGetModule(config, ScoringConfigGroup.class);

		ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home");
		ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("work");
		homeParams.setTypicalDuration(8 * 3600.);
		workParams.setTypicalDuration(8 * 3600.);
		scoring.addActivityParams(homeParams);
		scoring.addActivityParams(workParams);


		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		PopulationFactory fac = population.getFactory();

		Person person = fac.createPerson(Id.createPersonId("fareTestPerson"));
		Plan plan = fac.createPlan();

		Activity home = fac.createActivityFromCoord("home", new Coord(710300.624,5422165.737));
//		bus to Saal (Donau) work location departs at 09:14
		home.setEndTime(9 * 3600.);
		Activity work = fac.createActivityFromCoord("work", new Coord(714940.65,5420707.78));
//		rb17 to regensburg 2nd home location departs at 13:59
		work.setEndTime(13 * 3600. + 45 * 60);
		Activity home2 = fac.createActivityFromCoord("home", new Coord(726634.40,5433508.07));

		Leg leg = fac.createLeg(TransportMode.pt);

		plan.addActivity(home);
		plan.addLeg(leg);
		plan.addActivity(work);
		plan.addLeg(leg);
		plan.addActivity(home2);

		person.addPlan(plan);
		population.addPerson(person);
		scenario.setPopulation(population);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new PtFareModule());
				install(new PersonMoneyEventsAnalysisModule());
			}
		});
		controler.run();

		Assertions.assertTrue(Files.exists(Path.of(utils.getOutputDirectory())));

//		read personMoneyEvents.tsv and check if both fare entries do have the correct fare type
		String filePath = globFile(Path.of(utils.getOutputDirectory()), "*output_personMoneyEvents.tsv*").toString();
		String line;
		List<String[]> events = new ArrayList<>();

		try (BufferedReader br = IOUtils.getBufferedReader(filePath)) {
//			skip header
			br.readLine();

			while ((line = br.readLine()) != null) {
				events.add(line.split(";"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Assertions.assertEquals(2, events.size());
		Assertions.assertEquals(FareZoneBasedPtFareHandler.PT_FARE_ZONE_BASED, events.get(0)[4]);
		Assertions.assertEquals(FareZoneBasedPtFareHandler.PT_GERMANWIDE_FARE_BASED, events.get(1)[4]);
	}
}
