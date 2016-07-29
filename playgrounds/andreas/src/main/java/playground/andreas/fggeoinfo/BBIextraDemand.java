package playground.andreas.fggeoinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.andreas.fggeoinfo.ReadBBIDemand.DemandBox;


public class BBIextraDemand {
	
	private static final Logger log = Logger.getLogger(BBIextraDemand.class);
	private static final Level logLevel = Level.INFO;
	
	// Acts target coords
	Coord coordBBI;
	Coord coordTXL;
	Coord coordSXF;
	
	double scaleFactor = 0.02;
	
	List<DemandBox> demandList;
	List<Person> personList = new ArrayList<Person>();
	double[] timeStructure;

	private String oldDemandTXLSXFoutFile;
	private String newDemandBBIoutFile;
	
	public BBIextraDemand(String demandFile, String timeStructure, String oldDemandTXLSXFoutFile, String newDemandBBIoutFile,
			double scaleFactor, Coord coordBBI, Coord coordTXL, Coord coordSXF){
		log.setLevel(logLevel);
		this.scaleFactor = scaleFactor;
		this.oldDemandTXLSXFoutFile = oldDemandTXLSXFoutFile;
		this.newDemandBBIoutFile = newDemandBBIoutFile;
		this.coordBBI = coordBBI;
		this.coordTXL = coordTXL;
		this.coordSXF = coordSXF;
		
		try {
			this.demandList = ReadBBIDemand.readBBIDemand(demandFile);
			this.timeStructure = ReadBBITimeStructure.readBBITimeStructure(timeStructure);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void createDemand(){
		// create BBI only demand -> new demand
		createAgents(true);
		writePopulation(this.newDemandBBIoutFile);
		
		// create demand heading for TXLSXF -> old demand
		createAgents(false);
		writePopulation(this.oldDemandTXLSXFoutFile);
	}

	private void createAgents(boolean onlyBBI) {
		
		this.personList = new ArrayList<Person>(); 
		
		if(onlyBBI){
			
			log.info("Creating agents heading for BBI only.");
			
			for (DemandBox demandBox : this.demandList) {

				// create agents heading for BBI
				for (int i = 1; i < demandBox.numberOfPassengers() * this.scaleFactor; i++) {

					Person person = PopulationUtils.getFactory().createPerson(Id.create("BBI_" + demandBox.getNameBySourceAndDescription() + "_" + (i), Person.class));

					Plan plan = PopulationUtils.createPlan();
					Activity act = PopulationUtils.createActivityFromCoord("home", demandBox.getCoord());
					act.setEndTime(getStartTime() * 3600 + MatsimRandom.getRandom().nextDouble() * 3600);
					plan.addActivity(act);

					plan.addLeg(PopulationUtils.createLeg(TransportMode.car));

					plan.addActivity(PopulationUtils.createActivityFromCoord("leisure", this.coordBBI));

					person.addPlan(plan);

					this.personList.add(person);
				}	

			}
		} else {
			
			log.info("Creating agents heading for TXL and SXF.");
			
			for (DemandBox demandBox : this.demandList) {

				// create agents heading for TXL
				for (int i = 1; i < demandBox.numberOfPassengers() * this.scaleFactor * demandBox.getShareTXL(); i++) {

					Person person = PopulationUtils.getFactory().createPerson(Id.create("TXL_" + demandBox.getNameBySourceAndDescription() + "_" + (i), Person.class));

					Plan plan = PopulationUtils.createPlan();
					Activity act = PopulationUtils.createActivityFromCoord("home", demandBox.getCoord());
					act.setEndTime(getStartTime() * 3600 + MatsimRandom.getRandom().nextDouble() * 3600);
					plan.addActivity(act);

					plan.addLeg(PopulationUtils.createLeg(TransportMode.car));

					plan.addActivity(PopulationUtils.createActivityFromCoord("leisure", this.coordTXL));

					person.addPlan(plan);

					this.personList.add(person);
				}
				
				// create agents heading for SXF
				for (int i = 1; i < demandBox.numberOfPassengers() * this.scaleFactor * (1 - demandBox.getShareTXL()); i++) {

					Person person = PopulationUtils.getFactory().createPerson(Id.create("SXF_" + demandBox.getNameBySourceAndDescription() + "_" + (i), Person.class));

					Plan plan = PopulationUtils.createPlan();
					Activity act = PopulationUtils.createActivityFromCoord("home", demandBox.getCoord());
					act.setEndTime(getStartTime() * 3600 + MatsimRandom.getRandom().nextDouble() * 3600);
					plan.addActivity(act);

					plan.addLeg(PopulationUtils.createLeg(TransportMode.car));

					plan.addActivity(PopulationUtils.createActivityFromCoord("leisure", this.coordSXF));

					person.addPlan(plan);

					this.personList.add(person);
				}			

			}
		}
		
		log.info("Created: " + this.personList.size() + " agents");		
	}

	private void writePopulation(String filename) {
        MutableScenario sc = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        Population pop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		for (Person person : this.personList) {
			pop.addPerson(person);
		}
		
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(filename);
	}

	private int getStartTime(){
		
		double sum = 0.0;
		for (int i = 0; i < this.timeStructure.length; i++) {
			sum += this.timeStructure[i];
		}
		
		double rnd = MatsimRandom.getRandom().nextDouble() * sum;
		
		int i = 0;		
		sum = 0.0;
		
		while(sum < rnd){
			sum += this.timeStructure[i];
			i++;
		}		
		
		return Math.max(0, i - 1);
	}

}