/**
 *
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class PKMbyModeCalculatorTest {

	private int car;
	private int pt;
	private int walk;
	HashMap<String, Double> modeCalc = new HashMap<String, Double>();
	Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
	Person person2 = PopulationUtils.getFactory().createPerson(Id.create(2, Person.class));
	Person person3 = PopulationUtils.getFactory().createPerson(Id.create(3, Person.class));
	Person person4 = PopulationUtils.getFactory().createPerson(Id.create(4, Person.class));

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testPKMbyModeCalculator() {

		final IdMap<Person, Plan> map = new IdMap<>(Person.class);
		Plans plans = new Plans();

		/****************************
		 * Person - creating person 1
		 ************************************/

		Plan plan = plans.createPlanOne();

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
		Plan plan2 = plans.createPlanTwo();

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
		Plan plan3 = plans.createPlanThree();

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
		Plan plan4 = plans.createPlanFour();

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

		ControllerConfigGroup controllerConfigGroup = new ControllerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(outputDirectory,
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);
		controllerConfigGroup.setCreateGraphs(true);
		controllerConfigGroup.setFirstIteration(0);
		controllerConfigGroup.setLastIteration(10);
		PKMbyModeCalculator pkmbyModeCalculator = new PKMbyModeCalculator(controlerIO, new GlobalConfigGroup());
		// iteration 0
		pkmbyModeCalculator.addIteration(0, map);
		pkmbyModeCalculator.writeOutput(false);
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
		pkmbyModeCalculator.addIteration(1, map);
		pkmbyModeCalculator.writeOutput(false);
		totalCarDist = modeCalcDist.get("person1CarDist") + modeCalcDist.get("person3CarDist");
		totalPtDist = modeCalcDist.get("person1PtDist")  + modeCalcDist.get("person4PtDist");
		totalWalkDist = modeCalcDist.get("person1WalkDist") + modeCalcDist.get("person3WalkDist") + modeCalcDist.get("person4WalkDist");
		// reading the file and validating the output
		readAndValidateValues(1, totalCarDist, totalPtDist, totalWalkDist);

		// removing person 3
		map.remove(person3.getId());
		// iteration 2
		pkmbyModeCalculator.addIteration(2, map);
		pkmbyModeCalculator.writeOutput(false);
		totalCarDist = modeCalcDist.get("person1CarDist");
		totalPtDist = modeCalcDist.get("person1PtDist")  + modeCalcDist.get("person4PtDist");
		totalWalkDist = modeCalcDist.get("person1WalkDist") + modeCalcDist.get("person4WalkDist");
		// reading the file and validating the output
		readAndValidateValues(2, totalCarDist, totalPtDist, totalWalkDist);

	}

	/************ Reading and validating the output ************/
	private void readAndValidateValues(int itr, Double totalCar, Double totalPt, Double totalWalk) {

		String file = utils.getOutputDirectory() + "/PKMbyModeCalculator" + "/pkm_modestats.csv";
		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(file));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split(new GlobalConfigGroup().getDefaultDelimiter());
			decideColumns(columnNames);
			int iteration = 0;
			while ((line = br.readLine()) != null) {
				if (iteration == itr) {
					String[] column = line.split(new GlobalConfigGroup().getDefaultDelimiter());
					// checking if column number in greater than 0, because 0th column is always
					// 'Iteration' and we don't need that --> see decideColumns() method
					double carStat = (car > 0) ? Double.parseDouble(column[car]) : 0;
					double ptStat = (pt > 0) ? Double.parseDouble(column[pt]) : 0;
					double walkStat = (walk > 0) ? Double.parseDouble(column[walk]) : 0;

					Assertions.assertEquals(Math.round((totalCar / 1000)), carStat, 0, "Car stats score does not match");
					Assertions.assertEquals(Math.round((totalPt / 1000)), ptStat, 0, "PT stats score does not match");
					Assertions.assertEquals(Math.round((totalWalk / 1000)), walkStat, 0, "Walk stats score does not match");
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
	private void decideColumns(String[] columnNames) {

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
