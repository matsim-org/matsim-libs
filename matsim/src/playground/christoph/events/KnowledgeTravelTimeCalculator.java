package playground.christoph.events;

import org.apache.log4j.Logger;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.Link;
import org.matsim.router.util.TravelTime;

import playground.christoph.events.algorithms.KnowledgeReplanner;

public class KnowledgeTravelTimeCalculator implements TravelTime{
	
	double tbuffer = 2.0;		// zeitlicher Abstand ("Sicherheitsabstand") zwischen zwei Fahrzeugen
	double vehicleLength = 7.5;	// Länge eines Fahrzeugs
	
	protected QueueNetwork queueNetwork;
	
	private static final Logger log = Logger.getLogger(KnowledgeReplanner.class);
	
	public KnowledgeTravelTimeCalculator(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
	}
	
	public KnowledgeTravelTimeCalculator()
	{
		log.info("No QueueNetwork was commited - FreeSpeedTravelTimes will be calculated and returned!");
	}
	
	// ideale Fahrzeit zurückgeben, falls nur ein Link übergeben wurde
	public double getLinkTravelTime(Link link, double time)
	{
		if(queueNetwork == null)
		{
			log.info("No QueueNetwork found - FreeSpeedTravelTime is calculated and returned!");
			return link.getFreespeedTravelTime(time);
		}
			
		double vehicles = getVehiclesOnLink(link);
		return getLinkTravelTime(link, time, vehicles);
	}
	
	// reale Fahrzeit in Sekunden zurück geben
	//public double getLinkTravelTime(QueueLink queueLink, double time, double vehicles)
	public double getLinkTravelTime(Link link, double time, double vehicles)
	{
		// keine Fahrzeuge auf dem Link -> FreeSpeed!
		if(vehicles == 0.0) return link.getFreespeedTravelTime(time);
		
		// auf eine Fahrspur normieren
		vehicles = vehicles / link.getLanes(time);
		
		double length = link.getLength();
		
		double vmax = link.getFreespeed(time);
		
		// Geschwindigkeit eines Fahrzeugs auf dem Link
		double v = (length/vehicles - vehicleLength)/tbuffer;
		
		// Geschwindigkeit begrenzen
		if(v > vmax) v = vmax;
		
		double travelTime = length / v;
//		log.info("vehicles " + vehicles + " length " + length + " vmax " + vmax + " v " + v + " traveltime " + travelTime);
		log.info("Calculating TravelTime! TravelTime is " + travelTime + ", FreeSpeedTravelTime would be " + link.getFreespeedTravelTime(time));
		
		// Ergebnis prüfen
		if(travelTime < link.getFreespeedTravelTime(time))
		{
			log.info("TravelTime is shorter than FreeSpeedTravelTime - looks like something is wrong here. Using FreeSpeedTravelTime instead!");
			return link.getFreespeedTravelTime(time);
		}
		
		return travelTime;
	}

	private double getVehiclesOnLink(Link link)
	{
		QueueLink queueLink = queueNetwork.getQueueLink(link.getId());
		
		// maximale Anzahl an Fahrzeugen auf dem Link
		double maxVehiclesOnLink = queueLink.getSpaceCap();
		
		// Workaround!!! Prüfen, welche Anzahl an Fahrzeugen tatsächlich relevant ist.
		// (mit bzw. ohne Buffer)
		// Rückgabewert: veh count / space capacity -> * space capacity
		double vehiclesOnLink = queueLink.getDisplayableSpaceCapValue() * maxVehiclesOnLink;
		
		return vehiclesOnLink;
	}
	
}
