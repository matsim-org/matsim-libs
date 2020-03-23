/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class PKMbyModeCalculatorTest {

	private static int car;
	private static int pt;
	private static int walk;
	HashMap<String, Double> modeCalc = new HashMap<String, Double>();
	Person person3 = PopulationUtils.getFactory().createPerson(Id.create(3, Person.class));
	Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
	Person person2 = PopulationUtils.getFactory().createPerson(Id.create(2, Person.class));
	Person person4 = PopulationUtils.getFactory().createPerson(Id.create(4, Person.class));

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testPKMbyModeCalculator() {

		final IdMap<Person, Plan> map = new IdMap<>(Person.class);

		/****************************
		 * Person - creating person 1
		 ************************************/
		final Plan plan = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));

		final Id<Link> link1 = Id.create(10723, Link.class);
		final Id<Link> link2 = Id.create(123160, Link.class);
		final Id<Link> link3 = Id.create(130181, Link.class);
		final Id<Link> link4 = Id.create(139117, Link.class);
		final Id<Link> link5 = Id.create(139100, Link.class);

		Activity act1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan.addActivity(act1);
		Leg leg1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route = RouteUtils.createGenericRouteImpl(link1, link1);
		route.setDistance(100);
		leg1.setRoute(route);
		plan.addLeg(leg1);
		Activity act2 = PopulationUtils.createActivityFromLinkId("leisure", link1);// main mode walk
		plan.addActivity(act2);
		Leg leg2 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2.setDistance(150);
		leg2.setRoute(route2);
		plan.addLeg(leg2);
		Activity act3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan.addActivity(act3);
		Leg leg3 = PopulationUtils.createLeg(TransportMode.car);
		Route route3 = RouteUtils.createGenericRouteImpl(link1, link2);
		route3.setDistance(5000);
		leg3.setRoute(route3);
		plan.addLeg(leg3);
		Activity act4 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
		plan.addActivity(act4);
		Leg leg4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4 = RouteUtils.createGenericRouteImpl(link2, link2);
		route4.setDistance(300);
		leg4.setRoute(route4);
		plan.addLeg(leg4);
		Activity act5 = PopulationUtils.createActivityFromLinkId("work", link2);// main mode car
		plan.addActivity(act5);
		Leg leg5 = PopulationUtils.createLeg(TransportMode.walk);
		Route route5 = RouteUtils.createGenericRouteImpl(link2, link2);
		route5.setDistance(300);
		leg5.setRoute(route5);
		plan.addLeg(leg5);
		Activity act6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
		plan.addActivity(act6);
		Leg leg6 = PopulationUtils.createLeg(TransportMode.car);
		Route route6 = RouteUtils.createGenericRouteImpl(link2, link3);
		route6.setDistance(7000);
		leg6.setRoute(route6);
		plan.addLeg(leg6);
		Activity act7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
		plan.addActivity(act7);
		Leg leg7 = PopulationUtils.createLeg(TransportMode.walk);
		Route route7 = RouteUtils.createGenericRouteImpl(link3, link3);
		route7.setDistance(150);
		leg7.setRoute(route7);
		plan.addLeg(leg7);
		Activity act8 = PopulationUtils.createActivityFromLinkId("leisure", link3);// main mode car
		plan.addActivity(act8);
		Leg leg8 = PopulationUtils.createLeg(TransportMode.walk);
		leg8.setRoute(route7);
		plan.addLeg(leg8);
		Activity act9 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
		plan.addActivity(act9);
		Leg leg9 = PopulationUtils.createLeg(TransportMode.car);
		Route route9 = RouteUtils.createGenericRouteImpl(link3, link4);
		route9.setDistance(6000);
		leg9.setRoute(route9);
		plan.addLeg(leg9);
		Activity act10 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		plan.addActivity(act10);
		Leg leg10 = PopulationUtils.createLeg(TransportMode.walk);
		Route route10 = RouteUtils.createGenericRouteImpl(link4, link4);
		route10.setDistance(400);
		leg10.setRoute(route10);
		plan.addLeg(leg10);
		Activity act11 = PopulationUtils.createActivityFromLinkId("shopping", link4);// main mode car
		plan.addActivity(act11);
		Leg leg11 = PopulationUtils.createLeg(TransportMode.walk);
		Route route11 = RouteUtils.createGenericRouteImpl(link4, link4);
		route11.setDistance(300);
		leg11.setRoute(route11);
		plan.addLeg(leg11);
		Activity act12 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
		plan.addActivity(act12);
		Leg leg12 = PopulationUtils.createLeg(TransportMode.pt);
		Route route12 = RouteUtils.createGenericRouteImpl(link4, link5);
		route12.setDistance(1000);
		leg12.setRoute(route12);
		plan.addLeg(leg12);
		Activity act13 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
		plan.addActivity(act13);
		Leg leg13 = PopulationUtils.createLeg(TransportMode.walk);
		Route route13 = RouteUtils.createGenericRouteImpl(link5, link5);
		route13.setDistance(200);
		leg13.setRoute(route13);
		plan.addLeg(leg13);
		Activity act14 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode pt
		plan.addActivity(act14);
		Leg leg14 = PopulationUtils.createLeg(TransportMode.walk);
		leg14.setRoute(route13);
		plan.addLeg(leg14);
		Activity act15 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
		plan.addActivity(act15);
		Leg leg15 = PopulationUtils.createLeg(TransportMode.pt);
		leg15.setRoute(route12);
		plan.addLeg(leg15);
		Activity act16 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
		plan.addActivity(act16);
		Leg leg16 = PopulationUtils.createLeg(TransportMode.walk);
		Route route16 = RouteUtils.createGenericRouteImpl(link4, link4);
		route16.setDistance(300);
		leg16.setRoute(route16);
		plan.addLeg(leg16);
		Activity act17 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		plan.addActivity(act17);
		Leg leg17 = PopulationUtils.createLeg(TransportMode.car);
		Route route17 = RouteUtils.createGenericRouteImpl(link4, link1);
		route17.setDistance(6000);
		leg17.setRoute(route17);
		plan.addLeg(leg17);
		Activity act18 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan.addActivity(act18);
		Leg leg18 = PopulationUtils.createLeg(TransportMode.walk);
		leg18.setRoute(route);
		plan.addLeg(leg18);
		Activity act19 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		plan.addActivity(act19);

		// counting the total distance traveled in each mode
		/****************************************************************************************/
		Double person1CarDist = plan.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.car ? leg.getRoute().getDistance() : 0;
				}));
		Double person1PtDist = plan.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.pt ? leg.getRoute().getDistance() : 0;
				}));
		Double person1WalkDist = plan.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.walk ? leg.getRoute().getDistance() : 0;
				}));
		/****************************************************************************************/
		modeCalc.put("person1CarDist", person1CarDist);
		modeCalc.put("person1PtDist", person1PtDist);
		modeCalc.put("person1WalkDist", person1WalkDist);
		// person1TotalNumberOfLegs =
		// plan.getPlanElements().stream().filter(Leg.class::isInstance).count();
		map.put(person1.getId(), plan);

		/********************************
		 * Person 2 - creating person 2
		 ********************************/
		final Plan plan2 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("2", Person.class)));

		Activity actp2_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan2.addActivity(actp2_1);
		Leg legp2_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2_1.setDistance(100);
		legp2_1.setRoute(route2_1);
		plan2.addLeg(legp2_1);
		Activity actp2_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan2.addActivity(actp2_2);
		Leg legp2_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route2_2 = RouteUtils.createGenericRouteImpl(link1, link4);
		route2_2.setDistance(6000);
		legp2_2.setRoute(route2_2);
		plan2.addLeg(legp2_2);
		Activity actp2_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		plan2.addActivity(actp2_3);
		Leg legp2_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_3 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_3.setDistance(200);
		legp2_3.setRoute(route2_3);
		plan2.addLeg(legp2_3);
		Activity actp2_4 = PopulationUtils.createActivityFromLinkId("work", link4);// main mode car
		plan2.addActivity(actp2_4);
		Leg legp2_4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_4 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_4.setDistance(250);
		legp2_4.setRoute(route2_4);
		plan2.addLeg(legp2_4);
		Activity actp2_5 = PopulationUtils.createActivityFromLinkId("leisure", link4);// main mode walk
		plan2.addActivity(actp2_5);
		Leg legp2_5 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_5.setRoute(route2_4);
		plan2.addLeg(legp2_5);
		Activity actp2_6 = PopulationUtils.createActivityFromLinkId("work", link4);// main mode walk
		plan2.addActivity(actp2_6);
		Leg legp2_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_6.setRoute(route2_3);
		plan2.addLeg(legp2_6);
		Activity actp2_7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.car);
		plan2.addActivity(actp2_7);
		Leg legp2_7 = PopulationUtils.createLeg(TransportMode.car);
		legp2_7.setRoute(route2_2);
		plan2.addLeg(legp2_7);
		Activity actp2_8 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan2.addActivity(actp2_8);
		Leg legp2_8 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_8.setRoute(route2_1);
		plan2.addLeg(legp2_8);
		Activity actp2_9 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		plan2.addActivity(actp2_9);

		// counting the total distance traveled in each mode
		/****************************************************************************************/
		Double person2CarDist = plan2.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.car ? leg.getRoute().getDistance() : 0;
				}));
		Double person2WalkDist = plan2.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.walk ? leg.getRoute().getDistance() : 0;
				}));
		/****************************************************************************************/
		modeCalc.put("person2CarDist", person2CarDist);
		modeCalc.put("person2WalkDist", person2WalkDist);
		// person2TotalNumberOfLegs =
		// plan2.getPlanElements().stream().filter(Leg.class::isInstance).count();
		map.put(person2.getId(), plan2);

		/*****************************
		 * Person 3 - creating person 3
		 ************************************/
		final Plan plan3 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("3", Person.class)));

		Activity actp3_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan3.addActivity(actp3_1);
		Leg legp3_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route3_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route3_1.setDistance(100);
		legp3_1.setRoute(route3_1);
		plan3.addLeg(legp3_1);
		Activity actp3_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan3.addActivity(actp3_2);
		Leg legp3_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route3_2 = RouteUtils.createGenericRouteImpl(link1, link5);
		route3_2.setDistance(8000);
		legp3_2.setRoute(route3_2);
		plan3.addLeg(legp3_2);
		Activity actp3_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.car);
		plan3.addActivity(actp3_3);
		Leg legp3_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route3_3 = RouteUtils.createGenericRouteImpl(link5, link5);
		route3_3.setDistance(300);
		legp3_3.setRoute(route3_3);
		plan3.addLeg(legp3_3);
		Activity actp3_4 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode car
		plan3.addActivity(actp3_4);
		Leg legp3_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_4.setRoute(route3_3);
		plan3.addLeg(legp3_4);
		Activity actp3_5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.car);
		plan3.addActivity(actp3_5);
		Leg legp3_5 = PopulationUtils.createLeg(TransportMode.car);
		legp3_5.setRoute(route3_2);
		plan3.addLeg(legp3_5);
		Activity actp3_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan3.addActivity(actp3_6);
		Leg legp3_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_6.setRoute(route3_1);
		plan3.addLeg(legp3_6);
		Activity actp3_8 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		plan3.addActivity(actp3_8);

		// counting the total distance traveled in each mode
		/****************************************************************************************/
		Double person3CarDist = plan3.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.car ? leg.getRoute().getDistance() : 0;
				}));
		Double person3WalkDist = plan3.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.walk ? leg.getRoute().getDistance() : 0;
				}));
		/****************************************************************************************/
		modeCalc.put("person3CarDist", person3CarDist);
		modeCalc.put("person3WalkDist", person3WalkDist);

		map.put(person3.getId(), plan3);

		/************************
		 * Person 4-----creating person 4
		 **************************************/
		final Plan plan4 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("4", Person.class)));

		Activity actp4_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan4.addActivity(actp4_1);
		Leg legp4_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route4_1.setDistance(350);
		legp4_1.setRoute(route4_1);
		plan4.addLeg(legp4_1);
		Activity actp4_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan4.addActivity(actp4_2);
		Leg legp4_2 = PopulationUtils.createLeg(TransportMode.pt);
		Route route4_2 = RouteUtils.createGenericRouteImpl(link1, link3);
		route4_2.setDistance(6500);
		legp4_2.setRoute(route4_2);
		plan4.addLeg(legp4_2);
		Activity actp4_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.pt);
		plan4.addActivity(actp4_3);
		Leg legp4_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route4_3.setDistance(250);
		legp4_3.setRoute(route4_3);
		plan4.addLeg(legp4_3);
		Activity actp4_4 = PopulationUtils.createActivityFromLinkId("shopping", link3);// main mode pt
		plan4.addActivity(actp4_4);
		Leg legp4_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_4.setRoute(route4_3);
		plan4.addLeg(legp4_4);
		Activity actp4_5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.pt);
		plan4.addActivity(actp4_5);
		Leg legp4_5 = PopulationUtils.createLeg(TransportMode.pt);
		legp4_5.setRoute(route4_2);
		plan4.addLeg(legp4_5);
		Activity actp4_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		plan4.addActivity(actp4_6);
		Leg legp4_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_6.setRoute(route4_1);
		plan4.addLeg(legp4_6);
		Activity actp4_8 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode pt
		plan3.addActivity(actp4_8);

		// counting the total distance traveled in each mode
		/****************************************************************************************/
		Double person4PtDist = plan4.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.pt ? leg.getRoute().getDistance() : 0;
				}));
		Double person4WalkDist = plan4.getPlanElements().stream().filter(Leg.class::isInstance)
				.collect(Collectors.summingDouble(l -> {
					Leg leg = (Leg) l;
					return leg.getMode() == TransportMode.walk ? leg.getRoute().getDistance() : 0;
				}));
		/****************************************************************************************/
		modeCalc.put("person4PtDist", person4PtDist);
		modeCalc.put("person4WalkDist", person4WalkDist);

		map.put(person4.getId(), plan4);
		performTest(map, modeCalc, utils.getOutputDirectory() + "/PKMbyModeCalculator");
	}

	private void performTest(IdMap<Person, Plan> map, HashMap<String, Double> modeCalcDist, String outputDirectory) {

		ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(outputDirectory,
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);
		controlerConfigGroup.setCreateGraphs(true);
		controlerConfigGroup.setFirstIteration(0);
		controlerConfigGroup.setLastIteration(10);
		PKMbyModeCalculator travelDistanceStats = new PKMbyModeCalculator(controlerConfigGroup, controlerIO);
		// iteration 0
		travelDistanceStats.addIteration(0, map);
		travelDistanceStats.writeOutput();
		Double totalCarDist = modeCalcDist.get("person1CarDist") + modeCalcDist.get("person2CarDist")
				+ modeCalcDist.get("person3CarDist");
		Double totalPtDist = modeCalcDist.get("person1PtDist") + modeCalcDist.get("person4PtDist");
		Double totalWalkDist = modeCalcDist.get("person1WalkDist") + modeCalcDist.get("person2WalkDist")
				+ modeCalcDist.get("person3WalkDist") + modeCalcDist.get("person4WalkDist");
		// reading the file and validating the output
		readAndValidateValues(0, totalCarDist, totalPtDist, totalWalkDist);

		// removing person 2
		map.remove(person2.getId());
		// iteration 1
		travelDistanceStats.addIteration(1, map);
		travelDistanceStats.writeOutput();
		totalCarDist = modeCalcDist.get("person1CarDist") + modeCalcDist.get("person3CarDist");
		totalPtDist = modeCalcDist.get("person1PtDist")  + modeCalcDist.get("person4PtDist");
		totalWalkDist = modeCalcDist.get("person1WalkDist") + modeCalcDist.get("person3WalkDist") + modeCalcDist.get("person4WalkDist");
		// reading the file and validating the output
		readAndValidateValues(1, totalCarDist, totalPtDist, totalWalkDist);

		// removing person 3
		map.remove(person3.getId());
		// iteration 2
		travelDistanceStats.addIteration(2, map);
		travelDistanceStats.writeOutput();
		totalCarDist = modeCalcDist.get("person1CarDist");
		totalPtDist = modeCalcDist.get("person1PtDist")  + modeCalcDist.get("person4PtDist");
		totalWalkDist = modeCalcDist.get("person1WalkDist") + modeCalcDist.get("person4WalkDist");
		// reading the file and validating the output
		readAndValidateValues(2, totalCarDist, totalPtDist, totalWalkDist);

	}

	/************ Reading and validating the output ************/
	private void readAndValidateValues(int itr, Double totalCar, Double totalPt, Double totalWalk) {

		String file = utils.getOutputDirectory() + "/PKMbyModeCalculator" + "/pkm_modestats.txt";
		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(file));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split("	");
			decideColumns(columnNames);
			int iteration = 0;
			while ((line = br.readLine()) != null) {
				if (iteration == itr) {
					String[] column = line.split("	");
					// checking if column number in greater than 0, because 0th column is always
					// 'Iteration' and we don't need that --> see decideColumns() method
					Double carStat = (car > 0) ? Double.valueOf(column[car]) : 0;
					Double ptStat = (pt > 0) ? Double.valueOf(column[pt]) : 0;
					Double walkStat = (walk > 0) ? Double.valueOf(column[walk]) : 0;

					Assert.assertEquals("Car stats score does not match", (totalCar / 1000), carStat, 0);
					Assert.assertEquals("PT stats score does not match", (totalPt / 1000), ptStat, 0);
					Assert.assertEquals("Walk stats score does not match", (totalWalk / 1000), walkStat, 0);
					break;
				}
				iteration++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/************ Determining the columns of the output file ************/
	private static void decideColumns(String[] columnNames) {

		Integer i = 0;
		while (i < columnNames.length) {
			String name = columnNames[i];
			switch (name) {

			case "car":
				car = i;
				break;

			case "pt":
				pt = i;
				break;

			case "walk":
				walk = i;
				break;

			}
			i++;
		}
	}

}
