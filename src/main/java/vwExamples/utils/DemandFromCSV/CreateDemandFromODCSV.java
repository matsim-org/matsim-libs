package vwExamples.utils.DemandFromCSV;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.opencsv.CSVReader;

import playground.vsp.demandde.cemdap.output.ActivityTypes;

public class CreateDemandFromODCSV {

	String csvDemandFile;
	String epsgForDemandFile;
	String plansFile;
	List<String[]> ODData;
	ArrayList<Trip> Trips;
	Population population;
	int tripcounter;
	int subtripcounter;
	String outputplansfile;

	CreateDemandFromODCSV(String csvDemandFile, String epsgForDemandFile, String plansFile, String outputplansfile) {
		this.ODData = new ArrayList<String[]>();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.csvDemandFile = csvDemandFile;
		this.epsgForDemandFile = epsgForDemandFile;
		this.Trips = new ArrayList<Trip>();
		this.population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		this.tripcounter = 0;
		this.subtripcounter = 0;
		this.outputplansfile = outputplansfile;

	}

	public static void main(String[] args) {
		CreateDemandFromODCSV Demand = new CreateDemandFromODCSV(
				"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\simulation_input_data_to_steffen\\demand.csv", "epsg:4236", null,"D:\\Matsim\\Axer\\Hannover\\MOIA\\input\\plans\\plans.xml.gz");
		Demand.readDemandCSV();
		Demand.addTravlerToPoulation();

		//Write plans file
		new PopulationWriter(Demand.population).write(Demand.outputplansfile);
	}

	public void addTravlerToPoulation() {
		//Add for each OD-Pair an simple trip from Home to Home activity
		for (Trip trip : this.Trips) {
			
			double numberOfTotalPassangers = trip.adult_passengers;
			for (int i =1;i<= numberOfTotalPassangers;i++)
			{
				CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
						"EPSG:25832");
				Id<Person> personId = Id.create(this.tripcounter+"_"+i, Person.class);
				Person person = this.population.getFactory().createPerson(personId);

				Coord destination = ct.transform(new Coord(trip.destination_lon,trip.destination_lat));
				Coord origin = ct.transform(new Coord(trip.origin_lon,trip.origin_lat));
				double departure = trip.earliest_departure_time;
				Activity startHomeAct = PopulationUtils.createActivityFromCoord(ActivityTypes.HOME, origin);
				Activity endHomeAct = PopulationUtils.createActivityFromCoord(ActivityTypes.HOME, destination);

				Plan travelerPlan = PopulationUtils.createPlan();
				startHomeAct.setEndTime(departure);

				travelerPlan.addActivity(startHomeAct);
				travelerPlan.addLeg(PopulationUtils.createLeg(TransportMode.drt));
				travelerPlan.addActivity(endHomeAct);

				person.addPlan(travelerPlan);
				
				//Add generated person to population
				this.population.addPerson(person);
				this.subtripcounter++;
				
			}
			this.tripcounter++;
		}
	}

	public void readDemandCSV() {
		// List<String[]> lines = new ArrayList<String[]>();
		// request_time,origin_lon,origin_lat,destination_lon,destination_lat,adult_passengers,earliest_departure_time
		// 3.83,9.748710662806856,52.37641117305442,9.724524282164197,52.387478853652404,1,3.83

		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(this.csvDemandFile));
			ODData = reader.readAll();
			for (int i = 1; i < ODData.size(); i++) {
				String[] lineContents = ODData.get(i);
				double request_time = Double.parseDouble(lineContents[0]); // request_time,
				double origin_lon = Double.parseDouble(lineContents[1]); // origin_lon,
				double origin_lat = Double.parseDouble(lineContents[2]); // origin_lat,
				double destination_lon = Double.parseDouble(lineContents[3]); // destination_lon,
				double destination_lat = Double.parseDouble(lineContents[4]); // destination_lat,
				double adult_passengers = Double.parseDouble(lineContents[5]); // adult_passengers,
				double earliest_departure_time = Double.parseDouble(lineContents[6]); // earliest_departure_time
				Trip trip = new Trip(request_time, origin_lon, origin_lat, destination_lon, destination_lat,
						adult_passengers, earliest_departure_time);
				Trips.add(trip);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static void addDummyAgents() {

	}

}
