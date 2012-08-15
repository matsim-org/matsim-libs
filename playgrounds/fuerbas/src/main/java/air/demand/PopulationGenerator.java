package air.demand;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;



/**
 * 
 * Creates a population based on the Data of DeStatis Passagierzahlen September 2010
 * 
 * @author treczka
 * 
 *
 */
public class PopulationGenerator {
	
	public static void main(String[] args) {
					
		/*
		 * First, load config and scenario
		 */
		Config config = ConfigUtils.loadConfig("./input/config_de.xml");
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		/*
		 * Initialize randomizer
		 * Setze startzeit (3 Uhr UTC)
		 * airportoffen (15 Stunden) 
		 * 5-20 Uhr => Ausgabewert 15:00:00 (15 Stunden) 
		 * bzw. auf UTC umgerechnet Zeitfenster 03-18 Uhr UTC
		 */
		Random random = MatsimRandom.getLocalInstance();

		double startzeit = 10800;	
		double airportoffen = 54000;	
		
		/*
		 * Pick the Network and the Population out of the Scenario for convenience. 
		 */
		Network network = sc.getNetwork();
		Population population = sc.getPopulation();

		
		/*
		 * Pick the PopulationFactory out of the Population for convenience.
		 * It contains methods to create new Population items.
		 */
		PopulationFactory populationFactory = population.getFactory();

		
		/*
		 * Eingangsdaten importieren
		 */
		Eingangsdaten_parser eingang;
		eingang = new Eingangsdaten_parser();
		List<String> airportdaten = null;
		
		try {
			airportdaten = eingang.EingangAirport();
		} catch (IOException e) {
			
			e.printStackTrace();
		}	

			
		int eingangsdatearraynindex = 0;
		int personid_zaehler = 0;
		for (int j=0; j< airportdaten.size()/3 ; j++){	//für alle Datensätze
			String linkIdStringStart = airportdaten.get(eingangsdatearraynindex + 0);	// Startflughafen ID
			Id linkId = sc.createId(linkIdStringStart);
			Link link = sc.getNetwork().getLinks().get(linkId);
			if (link == null) {
				continue;
			}
			
			int aktuellepassagierzahl = Integer.valueOf((airportdaten.get(eingangsdatearraynindex+2))); // aktuelle Passagieranzahl in Variable schreiben
			
//			double startzeit = 10800;	// 3 Uhr UTC (Angabe in Sekunden)
//			double airportoffen = 54000;	// 5-20 Uhr => Ausgabewert 15:00:00 (15 Stunden) bzw auf UTC umgerechnet Öffnungszeiten 03-18 Uhr UTC
			
			
		
//			System.out.println("Passagiere: "+aktuellepassagierzahl);	// visuelle Prüfung in Konsole
//			double verteilungsfaktor = (double) aktuellepassagierzahl/ (double)(airportoffen/3600);	// Berechnung wieviele Passagiere pro Stunde erscheinen sollen wenn man 18Stunden Öffnungszeiten zu Grunde legt
//			System.out.println("Verteilungsfaktor: "+verteilungsfaktor);	// visuelle Prüfung in Konsole
//			double angepasstestartzeit = startzeit;
			
			
			for ( int k=0; k< aktuellepassagierzahl; k++){		// eingangsdatearraynindex+2 ist aktuelle Passagieranzahl
				Person person = populationFactory.createPerson(sc.createId(String.valueOf(personid_zaehler + k)));	// ID für aktuellen Passagier
				
				

				/*
				 * Create a of Plan for the Person
				 */
				Plan plan = populationFactory.createPlan();
			
				/*
				 * Create a "home" Activity for the Person. In order to have the Person end its day at the same location,
				 * we keep the home coordinates for later use (see below).
				 * Note that we use the CoordinateTransformation created above.
				 */


				
//				Activity activity1 = populationFactory.createActivityFromCoord("home", link.getCoord());
				ActivityImpl activity1 = (ActivityImpl) populationFactory.createActivityFromLinkId("home", link.getId());
				activity1.setCoord(link.getCoord());
				
				double actstart = random.nextDouble() * airportoffen + startzeit;
				activity1.setEndTime(actstart); // zufällig generierte ActivityStartTime als Endzeit gesetzt

				plan.addActivity(activity1); // add the Activity to the Plan

				/*
				 * Create a Leg. A Leg initially hasn't got many attributes. It just says that a pt will be used.
				 */
				plan.addLeg(populationFactory.createLeg("pt"));
				
				/*
				 * Create another Activity, at a different location.
				 */
				String linkIdString2 = airportdaten.get(eingangsdatearraynindex + 1);		// ZielfLughafen ID
				Id linkId2 = sc.createId(linkIdString2);
				Link destinationLink = sc.getNetwork().getLinks().get(linkId2);
				if (destinationLink == null) {	// abfangen von Flughäfen die in den Passagierdaten von DeStatis vorkommen, allerdings nicht im verwendeten Flugnetzwerk vorkommen
					continue;
				}
					

				ActivityImpl activity2 = (ActivityImpl) populationFactory.createActivityFromLinkId("home", destinationLink.getId());
				activity2.setCoord(destinationLink.getCoord());
				
				plan.addActivity(activity2);
				
				
				/*
				 * Wichtig! Fuege Plaene und Personen hinzu
				 */
				person.addPlan(plan);
				population.addPerson(person);
			}
			
			personid_zaehler = personid_zaehler + Integer.valueOf(airportdaten.get(eingangsdatearraynindex+2)); //ID Zaehler hochsetzen mit akt. Passagierzahlen
			eingangsdatearraynindex= eingangsdatearraynindex+3;		// Wichtig damit der korrekte Wert in der Arraylist angesprochen wird

			
		}
		
		/*
		 * Write the population  to a file.
		 */
		MatsimWriter popWriter = new org.matsim.api.core.v01.population.PopulationWriter(population, network);
		popWriter.write("./input/population.xml");

		
	}
}		
