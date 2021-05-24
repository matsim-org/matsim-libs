/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class PHbyModeCalculatorTest {

	private int car_travel;
	private int pt_travel;
	private int walk_travel;
	private int pt_wait;
	private int stageActivity_wait;
	

	Id<Person> person1 = Id.create("person1", Person.class);
	Id<Person> person2 = Id.create("person2", Person.class);
	Id<Person> person3 = Id.create("person3", Person.class);
	Id<Person> person4 = Id.create("person4", Person.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	final IdMap<Person, Plan> map = new IdMap<>(Person.class);

	@Test
	public void testPKMbyModeCalculator() {

		Plans plans = new Plans();

		/****************************
		 * Plan 1 - creating plan 1
		 ************************************/

		Plan plan1 = plans.createPlanOne();

		/********************************
		 * Plan 2 - creating plan 2
		 ********************************/
		Plan plan2 = plans.createPlanTwo();

		/*****************************
		 * Plan 3 - creating plan 3
		 ************************************/
		Plan plan3 = plans.createPlanThree();

		/************************
		 * Plan 4-----creating plan 4
		 **************************************/
		Plan plan4 = plans.createPlanFour();

		map.put(person1, plan1);
		map.put(person2, plan2);
		map.put(person3, plan3);
		map.put(person4, plan4);

		performTest(map, utils.getOutputDirectory() + "/PHbyModeCalculator");
	}

	private void performTest(IdMap<Person, Plan> map, String outputDirectory) {

		ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(outputDirectory,
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);
		controlerConfigGroup.setCreateGraphs(true);
		controlerConfigGroup.setFirstIteration(0);
		controlerConfigGroup.setLastIteration(10);
		PHbyModeCalculator phbyModeCalculator = new PHbyModeCalculator(controlerConfigGroup, controlerIO);

		phbyModeCalculator.addIteration(1, map);
		phbyModeCalculator.writeOutput();
		readAndValidateValues(1, map);
		
		// removing person 2
		map.remove(person2);
		phbyModeCalculator.addIteration(2, map);
		phbyModeCalculator.writeOutput();
		readAndValidateValues(2, map);

		// removing person 3
		map.remove(person3);
		phbyModeCalculator.addIteration(3, map);
		phbyModeCalculator.writeOutput();
		readAndValidateValues(3, map);

		// removing person 4
		map.remove(person4);
		phbyModeCalculator.addIteration(4, map);
		phbyModeCalculator.writeOutput();
		readAndValidateValues(4, map);
	}

	/************ Reading and validating the output ************/
	private void readAndValidateValues(int itr, IdMap<Person, Plan> map) {

		HashMap<String, Double> modeValues = new HashMap<String, Double>();
		modeValues.put("car", 0.0);
		modeValues.put("pt", 0.0);
		modeValues.put("walk", 0.0);
		modeValues.put("pt_wait", 0.0);
		modeValues.put("stageActivity_wait", 0.0);

		Iterator<Plan> pesronItr = map.iterator();
		while (pesronItr.hasNext()) {
			Plan plans = pesronItr.next();
			List<PlanElement> planelem = plans.getPlanElements();
			for (PlanElement elem : planelem) {
				if (elem instanceof Leg) {
					String mode = ((Leg) elem).getMode();
					Double value = modeValues.get(mode);
					value += (mode != "pt") ? ((Leg) elem).getRoute().getTravelTime().seconds()
							: ((Leg) elem).getRoute().getTravelTime().seconds() - ((Double) elem.getAttributes()
									.getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME) - ((Leg) elem).getDepartureTime().seconds()) ;
					modeValues.put(mode, value);
					if(mode == "pt") {
						Double wait_value = modeValues.get("pt_wait");
						wait_value += (Double) elem.getAttributes()
								.getAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME) - ((Leg) elem).getDepartureTime().seconds();
						modeValues.put("pt_wait", wait_value);
					}
						
				}else if(elem instanceof Activity) {
					Activity act = (Activity) elem;
					if (StageActivityTypeIdentifier.isStageActivity(act.getType())) {
						Double stageActivity_wait_value = modeValues.get("stageActivity_wait");
						stageActivity_wait_value += act.getEndTime().orElse(0) - act.getStartTime().orElse(0);
						modeValues.put("stageActivity_wait", stageActivity_wait_value);
					}
				}
			}
		}

		String file = utils.getOutputDirectory() + "/PHbyModeCalculator" + "/ph_modestats.txt";
		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(file));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split("	");
			decideColumns(columnNames);
			int iteration = 1;
			while ((line = br.readLine()) != null) {
				if (iteration == itr) {
					String[] column = line.split("	");
					// checking if column number in greater than 0, because 0th column is always
					// 'Iteration' and we don't need that --> see decideColumns() method
					Double car_travel_value = Double.valueOf(column[car_travel]);
					Double pt_travel_value = Double.valueOf(column[pt_travel]);
					Double walk_travel_value = Double.valueOf(column[walk_travel]);
					Double pt_wait_value = Double.valueOf(column[pt_wait]);
					Double stageActivity_wait_value = Double.valueOf(column[stageActivity_wait]);

					Assert.assertEquals("car_travel hour does not match", Math.round(modeValues.get("car") / 3600.0),
							car_travel_value, 0);
					Assert.assertEquals("pt_travel hour score does not match", Math.round(modeValues.get("pt") / 3600.0),
							pt_travel_value, 0);
					Assert.assertEquals("walk_travel hour does not match", Math.round(modeValues.get("walk") / 3600.0),
							walk_travel_value, 0);
					Assert.assertEquals("pt_wait hour does not match", Math.round(modeValues.get("pt_wait") / 3600.0),
							pt_wait_value, 0);
					Assert.assertEquals("stageActivity_wait hour does not match", Math.round(modeValues.get("stageActivity_wait") / 3600.0),
							stageActivity_wait_value, 0);

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

			case "car_travel":
				car_travel = i;
				break;

			case "pt_travel":
				pt_travel = i;
				break;

			case "walk_travel":
				walk_travel = i;
				break;
				
			case "pt_wait":
				pt_wait = i;
				break;
				
			case "stageActivity_wait":
				stageActivity_wait = i;
				break;

			}
			i++;
		}
	}
}
