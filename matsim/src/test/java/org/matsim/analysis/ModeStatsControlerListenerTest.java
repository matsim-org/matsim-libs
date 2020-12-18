/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class ModeStatsControlerListenerTest {

	static int bike;
	static int car;
	static int pt;
	static int other;
	static int non_network_walk;
	static int ride;
	static int walk;
	HashMap<String, Integer> person3modes = new HashMap<String, Integer>();
	HashMap<String, Integer> person1modes = new HashMap<String, Integer>();
	HashMap<String, Integer> person2modes = new HashMap<String, Integer>();

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testModeStatsControlerListener() {

		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		final List<PlanElement> planElem = new ArrayList<PlanElement>();
		PlanCalcScoreConfigGroup scoreConfig = new PlanCalcScoreConfigGroup();
		TransportPlanningMainModeIdentifier transportId = new TransportPlanningMainModeIdentifier();
		ModeParams modeParam1 = new ModeParams(TransportMode.walk);
		ModeParams modeParam2 = new ModeParams(TransportMode.car);
		ModeParams modeParam3 = new ModeParams(TransportMode.pt);
		ModeParams modeParam4 = new ModeParams(TransportMode.non_network_walk);
		ModeParams modeParam5 = new ModeParams(TransportMode.ride);
		ModeParams modeParam6 = new ModeParams(TransportMode.other);
		ModeParams modeParam7 = new ModeParams(TransportMode.bike);
		scoreConfig.addModeParams(modeParam1);
		scoreConfig.addModeParams(modeParam2);
		scoreConfig.addModeParams(modeParam3);
		scoreConfig.addModeParams(modeParam4);
		scoreConfig.addModeParams(modeParam5);
		scoreConfig.addModeParams(modeParam6);
		scoreConfig.addModeParams(modeParam7);

		/* ########Person 1######### --- creating person 1*/
		final Plan plan = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		person1modes.put(TransportMode.walk, 0);
		person1modes.put(TransportMode.car, 0);
		person1modes.put(TransportMode.pt, 0);
		person1modes.put(TransportMode.non_network_walk, 0);
		person1modes.put(TransportMode.other, 0);
		person1modes.put(TransportMode.bike, 0);
		person1modes.put(TransportMode.ride, 0);

		final Id<Link> link1 = Id.create(10723, Link.class);
		final Id<Link> link2 = Id.create(123160, Link.class);
		final Id<Link> link3 = Id.create(130181, Link.class);
		final Id<Link> link4 = Id.create(139117, Link.class);
		final Id<Link> link5 = Id.create(139100, Link.class);

		Activity act1 = PopulationUtils.createActivityFromLinkId("home", link1);
		planElem.add(act1);
		plan.addActivity(act1);
		Leg leg1 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg1);
		plan.addLeg(leg1);
		person1modes.put(TransportMode.walk, person1modes.get(TransportMode.walk) + 1);
		Activity act2 = PopulationUtils.createActivityFromLinkId("leisure", link1);// main mode walk
		planElem.add(act2);
		plan.addActivity(act2);
		Leg leg2 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg2);
		plan.addLeg(leg2);
		Activity act3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		planElem.add(act3);
		plan.addActivity(act3);
		Leg leg3 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(leg3);
		plan.addLeg(leg3);
		Activity act4 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
		planElem.add(act4);
		plan.addActivity(act4);
		Leg leg4 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg4);
		plan.addLeg(leg4);
		person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
		Activity act5 = PopulationUtils.createActivityFromLinkId("work", link2);// main mode car
		planElem.add(act5);
		plan.addActivity(act5);
		Leg leg5 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg5);
		plan.addLeg(leg5);
		Activity act6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
		planElem.add(act6);
		plan.addActivity(act6);
		Leg leg6 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(leg6);
		plan.addLeg(leg6);
		Activity act7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
		planElem.add(act7);
		plan.addActivity(act7);
		Leg leg7 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg7);
		plan.addLeg(leg7);
		person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
		Activity act8 = PopulationUtils.createActivityFromLinkId("leisure", link3);// main mode car
		planElem.add(act8);
		plan.addActivity(act8);
		Leg leg8 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg8);
		plan.addLeg(leg8);
		Activity act9 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
		planElem.add(act9);
		plan.addActivity(act9);
		Leg leg9 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(leg9);
		plan.addLeg(leg9);
		Activity act10 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		planElem.add(act10);
		plan.addActivity(act10);
		Leg leg10 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg10);
		plan.addLeg(leg10);
		person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
		Activity act11 = PopulationUtils.createActivityFromLinkId("shopping", link4);// main mode car
		planElem.add(act11);
		plan.addActivity(act11);
		Leg leg11 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg11);
		plan.addLeg(leg11);
		Activity act12 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
		planElem.add(act12);
		plan.addActivity(act12);
		Leg leg12 = PopulationUtils.createLeg(TransportMode.pt);
		planElem.add(leg12);
		plan.addLeg(leg12);
		Activity act13 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
		planElem.add(act13);
		plan.addActivity(act13);
		Leg leg13 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg13);
		plan.addLeg(leg13);
		person1modes.put(TransportMode.pt, person1modes.get(TransportMode.pt) + 1);
		Activity act14 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode pt
		planElem.add(act14);
		plan.addActivity(act14);
		Leg leg14 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg14);
		plan.addLeg(leg14);
		Activity act15 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
		planElem.add(act15);
		plan.addActivity(act15);
		Leg leg15 = PopulationUtils.createLeg(TransportMode.pt);
		planElem.add(leg15);
		plan.addLeg(leg15);
		Activity act16 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
		planElem.add(act16);
		plan.addActivity(act16);
		Leg leg16 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg16);
		plan.addLeg(leg16);
		Activity act17 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		planElem.add(act17);
		plan.addActivity(act17);
		Leg leg17 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(leg17);
		plan.addLeg(leg17);
		Activity act18 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		planElem.add(act18);
		plan.addActivity(act18);
		Leg leg18 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(leg18);
		plan.addLeg(leg18);
		person1modes.put(TransportMode.car, person1modes.get(TransportMode.car) + 1);
		Activity act19 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		planElem.add(act19);
		plan.addActivity(act19);

		person.addPlan(plan);
		population.addPerson(person);

		/* ########Person 2######### --- creating person 2*/
		final Plan plan2 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("2", Person.class)));
		Person person2 = PopulationUtils.getFactory().createPerson(Id.create(2, Person.class));
		person2modes.put(TransportMode.walk, 0);
		person2modes.put(TransportMode.car, 0);
		person2modes.put(TransportMode.pt, 0);
		person2modes.put(TransportMode.non_network_walk, 0);
		person2modes.put(TransportMode.other, 0);
		person2modes.put(TransportMode.bike, 0);
		person2modes.put(TransportMode.ride, 0);

		Activity actp2_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		planElem.add(actp2_1);
		plan2.addActivity(actp2_1);
		Leg legp2_1 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp2_1);
		plan2.addLeg(legp2_1);
		Activity actp2_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		planElem.add(actp2_2);
		plan2.addActivity(actp2_2);
		Leg legp2_2 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(legp2_2);
		plan2.addLeg(legp2_2);
		Activity actp2_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		planElem.add(actp2_3);
		plan2.addActivity(actp2_3);
		Leg legp2_3 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp2_3);
		plan2.addLeg(legp2_3);
		person2modes.put(TransportMode.car, person2modes.get(TransportMode.car) + 1);
		Activity actp2_4 = PopulationUtils.createActivityFromLinkId("work", link4);// main mode car
		planElem.add(actp2_4);
		plan2.addActivity(actp2_4);
		Leg legp2_4 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp2_4);
		plan2.addLeg(legp2_4);
		person2modes.put(TransportMode.walk, person2modes.get(TransportMode.walk) + 1);
		Activity actp2_5 = PopulationUtils.createActivityFromLinkId("leisure", link4);// main mode walk
		planElem.add(actp2_5);
		plan2.addActivity(actp2_5);
		Leg legp2_5 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp2_5);
		plan2.addLeg(legp2_5);
		person2modes.put(TransportMode.walk, person2modes.get(TransportMode.walk) + 1);
		Activity actp2_6 = PopulationUtils.createActivityFromLinkId("work", link4);// main mode walk
		planElem.add(actp2_6);
		plan2.addActivity(actp2_6);
		Leg legp2_6 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp2_6);
		plan2.addLeg(legp2_6);
		Activity actp2_7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		planElem.add(actp2_7);
		plan2.addActivity(actp2_7);
		Leg legp2_7 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(legp2_7);
		plan2.addLeg(legp2_7);
		Activity actp2_8 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		planElem.add(actp2_8);
		plan2.addActivity(actp2_8);
		Leg legp2_8 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp2_8);
		plan2.addLeg(legp2_8);
		person2modes.put(TransportMode.car, person2modes.get(TransportMode.car) + 1);
		Activity actp2_9 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		planElem.add(actp2_9);
		plan2.addActivity(actp2_9);

		person2.addPlan(plan2);
		population.addPerson(person2);

		/* ########Person 3######### --- creating person 3*/
		final Plan plan3 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("3", Person.class)));
		Person person3 = PopulationUtils.getFactory().createPerson(Id.create(3, Person.class));
		person3modes.put(TransportMode.walk, 0);
		person3modes.put(TransportMode.car, 0);
		person3modes.put(TransportMode.pt, 0);
		person3modes.put(TransportMode.non_network_walk, 0);
		person3modes.put(TransportMode.other, 0);
		person3modes.put(TransportMode.bike, 0);
		person3modes.put(TransportMode.ride, 0);

		Activity actp3_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		planElem.add(actp3_1);
		plan3.addActivity(actp3_1);
		Leg legp3_1 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp3_1);
		plan3.addLeg(legp3_1);
		Activity actp3_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		planElem.add(actp3_2);
		plan3.addActivity(actp3_2);
		Leg legp3_2 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(legp3_2);
		plan3.addLeg(legp3_2);
		Activity actp3_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.car);
		planElem.add(actp3_3);
		plan3.addActivity(actp3_3);
		Leg legp3_3 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp3_3);
		plan3.addLeg(legp3_3);
		person3modes.put(TransportMode.car, person3modes.get(TransportMode.car) + 1);
		Activity actp3_4 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode car
		planElem.add(actp3_4);
		plan3.addActivity(actp3_4);
		Leg legp3_5 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp3_5);
		plan3.addLeg(legp3_5);
		Activity actp3_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.car);
		planElem.add(actp3_6);
		plan3.addActivity(actp3_6);
		Leg legp3_6 = PopulationUtils.createLeg(TransportMode.car);
		planElem.add(legp3_6);
		plan3.addLeg(legp3_6);
		Activity actp3_7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		planElem.add(actp3_7);
		plan3.addActivity(actp3_7);
		Leg legp3_7 = PopulationUtils.createLeg(TransportMode.walk);
		planElem.add(legp3_7);
		plan3.addLeg(legp3_7);
		person3modes.put(TransportMode.car, person3modes.get(TransportMode.car) + 1);
		Activity actp3_8 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		planElem.add(actp3_8);
		plan3.addActivity(actp3_8);

		person3.addPlan(plan3);
		population.addPerson(person3);

		performTest(population, transportId, scoreConfig, utils.getOutputDirectory() + "/ModeStatsControlerListener",
				planElem);
	}

	private void performTest(Population population, TransportPlanningMainModeIdentifier transportId,
			PlanCalcScoreConfigGroup scoreConfig, String outputDirectory, List<PlanElement> planElem) {

		ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(outputDirectory,
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);
		controlerConfigGroup.setCreateGraphs(true);
		controlerConfigGroup.setFirstIteration(0);
		ModeStatsControlerListener modStatListner = new ModeStatsControlerListener(controlerConfigGroup, population,
				controlerIO, scoreConfig, transportId);

		StartupEvent eventStart = new StartupEvent(null);
		modStatListner.notifyStartup(eventStart);

		HashMap<String, Integer> modesIter0 = new HashMap<String, Integer>();

		IterationEndsEvent event0 = new IterationEndsEvent(null, 0, false);
		modStatListner.notifyIterationEnds(event0);

		//Merging 3 maps (modes of 3 persons) to a new single map and adding together the count of each mode of all 3 persons
		person1modes.forEach((k, v) -> modesIter0.merge(k, v, Integer::sum));
		person2modes.forEach((k, v) -> modesIter0.merge(k, v, Integer::sum));
		person3modes.forEach((k, v) -> modesIter0.merge(k, v, Integer::sum));

		readAndcompareValues(modesIter0, 0);
		
		//Remove one person
		population.getPersons().remove(Id.create("2", Person.class));

		// Change mode of one trip of person 3
		PlanElement pe = population.getPersons().get(Id.create("3", Person.class)).getSelectedPlan().getPlanElements().get(3);
		Leg leg = (Leg) pe;
		person3modes.put(leg.getMode(), person3modes.get(leg.getMode()) - 1);
		// add a new mode which did not occur before
		leg.setMode(TransportMode.ride);
		person3modes.put(leg.getMode(), person3modes.get(leg.getMode()) + 1);

		IterationEndsEvent event1 = new IterationEndsEvent(null, 1, false);
		modStatListner.notifyIterationEnds(event1);

		HashMap<String, Integer> modesIter1 = new HashMap<String, Integer>();

		//Merging 2 maps (modes of 2 persons) to a new single map and adding together the count of each mode of 2 persons
		person1modes.forEach((k, v) -> modesIter1.merge(k, v, Integer::sum));
		person3modes.forEach((k, v) -> modesIter1.merge(k, v, Integer::sum));

		readAndcompareValues(modesIter1, 1);

		//Remove one more person
		population.getPersons().remove(Id.create("3", Person.class));

		IterationEndsEvent event2 = new IterationEndsEvent(null, 2, false);
		modStatListner.notifyIterationEnds(event2);

		// in the last iteration check whether all iterations can still be found
		readAndcompareValues(modesIter0, 0);
		readAndcompareValues(modesIter1, 1);
		readAndcompareValues(person1modes, 2);
	}

	//(no: of trips in a mode) / (total no: of trips) ---> should match with the text file
	//sum of the scores of each mode should add up to 1
	private void readAndcompareValues(HashMap<String, Integer> modes, int itr) {

		String file = utils.getOutputDirectory() + "/ModeStatsControlerListener" + "/modestats.txt";
		BufferedReader br;
		String line;
		int totalTrips = modes.get(TransportMode.car) + modes.get(TransportMode.bike) + modes.get(TransportMode.pt)
				+ modes.get(TransportMode.other) + modes.get(TransportMode.non_network_walk)
				+ modes.get(TransportMode.ride) + modes.get(TransportMode.walk);
		try {
			br = new BufferedReader(new FileReader(file));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split("	");
			decideColumns(columnNames);
			int iteration = 0;
			while ((line = br.readLine()) != null) {
				if (iteration == itr) {
					String[] column = line.split("	");
					// checking if column number in greater than 0, because 0th column is always 'Iteration' and we don't need that --> see decideColumns() method
					Double carvalue = (car > 0) ? Double.valueOf(column[car]) : 0;
					Double walkvalue = (walk > 0) ? Double.valueOf(column[walk]) : 0;
					Double ptvalue = (pt > 0) ? Double.valueOf(column[pt]) : 0;
					Double bikevalue = (bike > 0) ? Double.valueOf(column[bike]) : 0;
					Double non_network_walkvalue = (non_network_walk > 0) ? Double.valueOf(column[non_network_walk])
							: 0;
					Double othervalue = (other > 0) ? Double.valueOf(column[other]) : 0;
					Double ridevalue = (ride > 0) ? Double.valueOf(column[ride]) : 0;
					Assert.assertEquals("car mode has an unexpected score",
							(modes.get(TransportMode.car).doubleValue() / totalTrips), carvalue, 0);
					Assert.assertEquals("walk mode has an unexpected score",
							(modes.get(TransportMode.walk).doubleValue() / totalTrips), walkvalue, 0);
					Assert.assertEquals("pt mode has an unexpected score",
							(modes.get(TransportMode.pt).doubleValue() / totalTrips), ptvalue, 0);
					Assert.assertEquals("bike mode has an unexpected score",
							(modes.get(TransportMode.bike).doubleValue() / totalTrips), bikevalue, 0);
					Assert.assertEquals("non_network_walk mode has an unexpected score",
							(modes.get(TransportMode.non_network_walk).doubleValue() / totalTrips),
							non_network_walkvalue, 0);
					Assert.assertEquals("other mode has an unexpected score",
							(modes.get(TransportMode.other).doubleValue() / totalTrips), othervalue, 0);
					Assert.assertEquals("ride mode has an unexpected score",
							(modes.get(TransportMode.ride).doubleValue() / totalTrips), ridevalue, 0);

					Assert.assertEquals("sum of the scores of all  modes in not equal to 1", 1.0,
							carvalue + walkvalue + ptvalue + bikevalue + non_network_walkvalue + othervalue + ridevalue,
							0.01);

					break;
				}
				iteration++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	//Identifying column numbers of each mode in the text file, if any one of the modes is not in the text file it will be assigned with default 0 value
	private static void decideColumns(String[] columnNames) {

		Integer i = 0;
		while (i < columnNames.length) {
			String name = columnNames[i];
			switch (name) {

			case "bike":
				bike = i;
				break;

			case "car":
				car = i;
				break;

			case "non_network_walk":
				non_network_walk = i;
				break;

			case "other":
				other = i;
				break;

			case "pt":
				pt = i;
				break;

			case "ride":
				ride = i;
				break;

			case "walk":
				walk = i;
				break;

			}
			i++;
		}
	}
}
