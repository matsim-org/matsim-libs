package playground.andreas.fggeoinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.andreas.fggeoinfo.ReadBBIDemand.DemandBox;


public class BBIextraDemand {
	
	private static final Logger log = Logger.getLogger(BBIextraDemand.class);
	
	Coord coordBBI = new CoordImpl(4604545.48760, 5805194.68221);
	Coord coordTXL = new CoordImpl(4588068.19422, 5824668.31998);
	Coord coordSXF = new CoordImpl(4603377.91673, 5807538.81303);
	
	double fraction = 0.02;
	
	List<DemandBox> demandList;
	List<Person> personList = new ArrayList<Person>();
	double[] timeStructure;
	
	public static void main(final String[] args) {
		
		BBIextraDemand bbi = new BBIextraDemand();
		bbi.initialize();
		
		bbi.createAgents(false);
		bbi.writePopulation("d:\\Berlin\\FG Geoinformation\\Scenario\\Ausgangsdaten\\20100809_verwendet\\pop_generated_TXL_SXF.xml.gz");
		
		bbi.createAgents(true);
		bbi.writePopulation("d:\\Berlin\\FG Geoinformation\\Scenario\\Ausgangsdaten\\20100809_verwendet\\pop_generated_BBI_only.xml.gz");
				
	}

	private void initialize() {
		try {
			this.demandList = ReadBBIDemand.readBBIDemand("d:\\Berlin\\FG Geoinformation\\Scenario\\Ausgangsdaten\\20100809_verwendet\\Anreise_Autobahnauffahrten_20100804.csv");
			this.timeStructure = ReadBBITimeStructure.readBBITimeStructure("d:\\Berlin\\FG Geoinformation\\Scenario\\Ausgangsdaten\\20100809_verwendet\\Analyse_Fluege_20100804.csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	private void createAgents(boolean onlyBBI) {
		
		this.personList = new ArrayList<Person>(); 
		
		if(onlyBBI){
			
			log.info("Creating agents heading for BBI only.");
			
			for (DemandBox demandBox : this.demandList) {

				// create agents heading for BBI
				for (int i = 1; i < demandBox.numberOfPassengers() * this.fraction; i++) {

					PersonImpl person =  new PersonImpl(new IdImpl("BBI_" + demandBox.getNameBySourceAndDescription() + "_" + (i)));

					PlanImpl plan = new PlanImpl();
					ActivityImpl act = new ActivityImpl("home", demandBox.getCoord());
					act.setEndTime(getStartTime() * 3600 + Math.random() * 3600);
					plan.addActivity(act);

					plan.addLeg(new LegImpl(TransportMode.car));

					plan.addActivity(new ActivityImpl("leisure", this.coordBBI));

					person.addPlan(plan);

					this.personList.add(person);
				}	

			}
		} else {
			
			log.info("Creating agents heading for TXL and SXF.");
			
			for (DemandBox demandBox : this.demandList) {

				// create agents heading for TXL
				for (int i = 1; i < demandBox.numberOfPassengers() * this.fraction * demandBox.getShareTXL(); i++) {

					PersonImpl person =  new PersonImpl(new IdImpl("TXL_" + demandBox.getNameBySourceAndDescription() + "_" + (i)));

					PlanImpl plan = new PlanImpl();
					ActivityImpl act = new ActivityImpl("home", demandBox.getCoord());
					act.setEndTime(getStartTime() * 3600 + Math.random() * 3600);
					plan.addActivity(act);

					plan.addLeg(new LegImpl(TransportMode.car));

					plan.addActivity(new ActivityImpl("leisure", this.coordTXL));

					person.addPlan(plan);

					this.personList.add(person);
				}
				
				// create agents heading for SXF
				for (int i = 1; i < demandBox.numberOfPassengers() * this.fraction * (1 - demandBox.getShareTXL()); i++) {

					PersonImpl person =  new PersonImpl(new IdImpl("SXF_" + demandBox.getNameBySourceAndDescription() + "_" + (i)));

					PlanImpl plan = new PlanImpl();
					ActivityImpl act = new ActivityImpl("home", demandBox.getCoord());
					act.setEndTime(getStartTime() * 3600 + Math.random() * 3600);
					plan.addActivity(act);

					plan.addLeg(new LegImpl(TransportMode.car));

					plan.addActivity(new ActivityImpl("leisure", this.coordSXF));

					person.addPlan(plan);

					this.personList.add(person);
				}			

			}
		}
		
		log.info("Created: " + this.personList.size() + " agents");		
	}

	private void writePopulation(String filename) {
		PopulationImpl pop = new PopulationImpl(new ScenarioImpl());
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
		
		double rnd = Math.random() * sum;
		
		int i = 0;		
		sum = 0.0;
		
		while(sum < rnd){
			sum += this.timeStructure[i];
			i++;
		}		
		
		return Math.max(0, i - 1);
	}

}