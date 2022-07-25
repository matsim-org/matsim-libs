package playground.vsp.ev;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.ev.EVUtils;


import org.matsim.vehicles.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UrbanEVIT {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void run() {
		Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
		overridePopulation(scenario);
		scenario.getVehicles().getVehicles().keySet().forEach(vehicleId -> scenario.getVehicles().removeVehicle(vehicleId));
		scenario.getConfig().controler().setOutputDirectory("test/output/urbanEV/UrbanEVRun");
		CreateUrbanEVTestScenario.createAndRegisterPersonalCarAndBikeVehicles(scenario);
		///controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.run();


	}

	private static void overridePopulation(Scenario scenario) {

		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();
//		Person person = factory.createPerson(Id.createPersonId("Charge during leisure + bike"));
//
//
//		Plan plan = factory.createPlan();
//
//		Activity home1 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
//		home1.setEndTime(0);
//		plan.addActivity(home1);
//
//		plan.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work1 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
//		work1.setEndTime(10 * 3600);
//		plan.addActivity(work1);
//
//		plan.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work12 = factory.createActivityFromLinkId("work", Id.createLinkId("172"));
//		work12.setEndTime(11 * 3600);
//		plan.addActivity(work12);
//
//		plan.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity leisure1 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
//		leisure1.setEndTime(12 * 3600);
//		plan.addActivity(leisure1);
//
//		plan.addLeg(factory.createLeg(TransportMode.bike));
//
//		Activity leisure12 = factory.createActivityFromLinkId("leisure", Id.createLinkId("89"));
//		leisure12.setEndTime(13 * 3600);
//		plan.addActivity(leisure12);
//
//		plan.addLeg(factory.createLeg(TransportMode.bike));
//
//		Activity leisure13 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
//		leisure13.setEndTime(14 * 3600);
//		plan.addActivity(leisure13);
//
//		plan.addLeg(factory.createLeg(TransportMode.car));
//
//
//		Activity home12 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
//		home12.setEndTime(15 * 3600);
//		plan.addActivity(home12);
//		person.addPlan(plan);
//		person.setSelectedPlan(plan);
//
//
//		scenario.getPopulation().addPerson(person);
//

//		Person person2 = factory.createPerson(Id.createPersonId("Charging during shopping"));
//
//		Plan plan2 = factory.createPlan();
//
//		Activity home21 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home21.setEndTime(8 * 3600);
//		plan2.addActivity(home21);
//		plan2.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work21 = factory.createActivityFromLinkId("work", Id.createLinkId("175"));
//		work21.setEndTime(10 * 3600);
//		plan2.addActivity(work21);
//
//		plan2.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work22 = factory.createActivityFromLinkId("work", Id.createLinkId("60"));
//		work22.setEndTime(12 * 3600);
//		plan2.addActivity(work22);
//
//		plan2.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity shopping21 = factory.createActivityFromLinkId("shopping", Id.createLinkId("9"));
//		shopping21.setMaximumDuration(1200);
//
//		plan2.addActivity(shopping21);
//
//		plan2.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work23 = factory.createActivityFromLinkId("work", Id.createLinkId("5"));
//		work23.setEndTime(13 * 3600);
//		plan2.addActivity(work23);
//
//		plan2.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity home22 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home22.setEndTime(15 * 3600);
//		plan2.addActivity(home22);
//		person2.addPlan(plan2);
//		person2.setSelectedPlan(plan2);
//
//		scenario.getPopulation().addPerson(person2);
//
//		Person person3 = factory.createPerson(Id.createPersonId("Charger Selection long distance leg"));
//
//		Plan plan3 = factory.createPlan();
//
//		Activity home31 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home31.setEndTime(8 * 3600);
//		plan3.addActivity(home31);
//		plan3.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work31 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
//		work31.setEndTime(10 * 3600);
//		plan3.addActivity(work31);
//
//		plan3.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work32 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
//		work32.setEndTime(12 * 3600);
//		plan3.addActivity(work32);
//
//		plan3.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity home32 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home32.setEndTime(15 * 3600);
//		plan3.addActivity(home32);
//		person3.addPlan(plan3);
//		person3.setSelectedPlan(plan3);
//
//		scenario.getPopulation().addPerson(person3);
//
//		Person person4 = factory.createPerson(Id.createPersonId("Charger Selection long distance twin"));
//
//		Plan plan4 = factory.createPlan();
//
//		Activity home41 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home41.setEndTime(8 * 3605);
//		plan4.addActivity(home41);
//		plan4.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work41 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
//		work41.setEndTime(10 * 3600);
//		plan4.addActivity(work41);
//
//		plan4.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work42 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
//		work42.setEndTime(12 * 3600);
//		plan4.addActivity(work42);
//
//		plan4.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity home42 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home42.setEndTime(15 * 3600);
//		plan4.addActivity(home42);
//		person4.addPlan(plan4);
//		person4.setSelectedPlan(plan4);
//
//		scenario.getPopulation().addPerson(person4);
//
		Person person5 = factory.createPerson(Id.createPersonId("Triple Charger"));

		Plan plan5 = factory.createPlan();

		Activity home51 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home51.setMaximumDuration(3*3600);
		plan5.addActivity(home51);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work51 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work51.setMaximumDuration(1*1000);
		plan5.addActivity(work51);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work52 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work52.setMaximumDuration(1*1000);
		plan5.addActivity(work52);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work53 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work53.setMaximumDuration(1*1000);
		plan5.addActivity(work53);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home52 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home52.setMaximumDuration(1*1000);
		plan5.addActivity(home52);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work54 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work54.setMaximumDuration(1*1000);
		plan5.addActivity(work54);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work55 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work55.setMaximumDuration(1*1000);
		plan5.addActivity(work55);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work56 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work56.setMaximumDuration(1*1000);
		plan5.addActivity(work56);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home53 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home53.setMaximumDuration(1*1000);
		plan5.addActivity(home53);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work57 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work57.setMaximumDuration(1*1000);
		plan5.addActivity(work57);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work58 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work58.setMaximumDuration(1*1200);
		plan5.addActivity(work58);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work59 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work59.setMaximumDuration(1*1000);
		plan5.addActivity(work59);
		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home54 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home54.setMaximumDuration(1*1000);
		plan5.addActivity(home54);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work510 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work510.setMaximumDuration(1*1000);
		plan5.addActivity(work510);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work511 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work511.setMaximumDuration(1*1000);
		plan5.addActivity(work511);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work512 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work512.setMaximumDuration(1*1000);
		plan5.addActivity(work512);
		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home55 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home55.setMaximumDuration(1*1000);
		plan5.addActivity(home55);


		person5.addPlan(plan5);
		person5.setSelectedPlan(plan5);

		scenario.getPopulation().addPerson(person5);

//		Person person6= factory.createPerson(Id.createPersonId("Double Charger"));
//
//		Plan plan6 = factory.createPlan();
//
//		Activity home61 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
//		home61.setEndTime(6*3600);
//		plan6.addActivity(home61);
//		plan6.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work61 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
//		work61.setMaximumDuration(1200);
//		plan6.addActivity(work61);
//
//		plan6.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work62 = factory.createActivityFromLinkId("work", Id.createLinkId("2"));
//		work62.setMaximumDuration(1200);
//		plan6.addActivity(work62);
//
//		plan6.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work63 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
//		work63.setMaximumDuration(1200);
//		plan6.addActivity(work63);
//
//		plan6.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work64 = factory.createActivityFromLinkId("work", Id.createLinkId("2"));
//		work64.setMaximumDuration(1200);
//		plan6.addActivity(work64);
//
//		plan6.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work65 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
//		work65.setMaximumDuration(1200);
//		plan6.addActivity(work65);
//
//		plan6.addLeg(factory.createLeg(TransportMode.car));
//
//
//		Activity home62 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
//		home62.setMaximumDuration(1200);
//		plan6.addActivity(home62);
//
//
//
//
//		person6.addPlan(plan6);
//		person6.setSelectedPlan(plan6);
//		scenario.getPopulation().addPerson(person6);

//		Person person7= factory.createPerson(Id.createPersonId("Not enough time Charger"));
//
//		Plan plan7 = factory.createPlan();
//
//		Activity home71 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
//		home71.setEndTime(6*3600);
//		plan7.addActivity(home71);
//		plan7.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work71 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
//		work71.setMaximumDuration(1200);
//		plan7.addActivity(work61);
//
//		plan7.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work72 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
//		work72.setMaximumDuration(1200);
//		plan7.addActivity(work72);
//
//		plan7.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work73 = factory.createActivityFromLinkId("work", Id.createLinkId("92"));
//		work73.setMaximumDuration(1140);
//		plan7.addActivity(work73);
//
//		plan7.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work74 = factory.createActivityFromLinkId("work", Id.createLinkId("75"));
//		work74.setMaximumDuration(1200);
//		plan7.addActivity(work74);
//
//		plan7.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work75 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
//		work75.setMaximumDuration(1200);
//		plan7.addActivity(work75);
//
//		plan7.addLeg(factory.createLeg(TransportMode.car));
//
//
//		Activity home72 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
//		home72.setMaximumDuration(1200);
//		plan7.addActivity(home72);
//
//		person7.addPlan(plan7);
//		person7.setSelectedPlan(plan7);
//		scenario.getPopulation().addPerson(person7);
//
//
//		Person person8 = factory.createPerson(Id.createPersonId("Home Charger"));
//		Plan plan8 = factory.createPlan();
//
//		Activity home8 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
//		home8.setMaximumDuration(1200);
//		plan8.addActivity(home8);
//
//		plan8.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work8 = factory.createActivityFromLinkId("work", Id.createLinkId("180"));
//		work8.setMaximumDuration(1200);
//		plan8.addActivity(work8);
//
//		plan8.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity home81 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
//		home81.setMaximumDuration(1200);
//		plan8.addActivity(home81);
//
//		plan8.addLeg(factory.createLeg(TransportMode.bike));
//
//		Activity leisure81 = factory.createActivityFromLinkId("leisure", Id.createLinkId("5"));
//		leisure81.setMaximumDuration(1200);
//		plan8.addActivity(leisure81);
//		plan8.addLeg(factory.createLeg(TransportMode.bike));
//
//		Activity leisure82 = factory.createActivityFromLinkId("leisure", Id.createLinkId("91"));
//		leisure82.setMaximumDuration(1200);
//		plan8.addActivity(leisure82);
//
//
//		person8.addPlan(plan8);
//		person8.setSelectedPlan(plan8);
//		scenario.getPopulation().addPerson(person8);
//
//		Person person9 = factory.createPerson(Id.createPersonId("Double Charger Home Charger"));
//		Plan plan9 = factory.createPlan();
//
//		Activity home91 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
//		home91.setEndTime(5*3600);
//		plan9.addActivity(home91);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work91 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
//		work91.setMaximumDuration(1*1200);
//		plan9.addActivity(work91);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work92 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
//		work92.setMaximumDuration(1*1200);
//		plan9.addActivity(work92);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work93 = factory.createActivityFromLinkId("work", Id.createLinkId("80"));
//		work93.setMaximumDuration(1*1200);
//		plan9.addActivity(work93);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work94= factory.createActivityFromLinkId("work", Id.createLinkId("1"));
//		work94.setMaximumDuration(1*1200);
//		plan9.addActivity(work94);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work95 = factory.createActivityFromLinkId("work", Id.createLinkId("80"));
//		work95.setMaximumDuration(1*1200);
//		plan9.addActivity(work95);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity work96= factory.createActivityFromLinkId("work", Id.createLinkId("1"));
//		work96.setMaximumDuration(1*1200);
//		plan9.addActivity(work96);
//
//		plan9.addLeg(factory.createLeg(TransportMode.car));
//
//		Activity home92 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
//		home92.setEndTime(1200);
//		plan9.addActivity(home92);
//
//
//		plan9.addLeg(factory.createLeg(TransportMode.bike));
//
//		Activity leisure91 = factory.createActivityFromLinkId("leisure", Id.createLinkId("5"));
//		leisure91.setMaximumDuration(1200);
//		plan9.addActivity(leisure91);
//
//		plan9.addLeg(factory.createLeg(TransportMode.bike));
//
//		Activity leisure92 = factory.createActivityFromLinkId("leisure", Id.createLinkId("91"));
//		leisure92.setMaximumDuration(1200);
//		plan9.addActivity(leisure92);
//
//		person9.addPlan(plan9);
//		person9.setSelectedPlan(plan9);
//		scenario.getPopulation().addPerson(person9);



	}
}