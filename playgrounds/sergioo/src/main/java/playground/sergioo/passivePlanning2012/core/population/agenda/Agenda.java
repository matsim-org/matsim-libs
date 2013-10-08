package playground.sergioo.passivePlanning2012.core.population.agenda;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class Agenda {
	
	//Classes
	public class AgendaElement {
		
		//Attributes
		private final String type;
		private final Distribution numHourWeek;
		private final Distribution duration;
		private double currentPerformedTime = 0;
		
		//Constructors
		public AgendaElement(String type, Distribution numHourWeek, Distribution duration) {
			super();
			this.type = type;
			this.numHourWeek = numHourWeek;
			this.duration = duration;
		}
		public AgendaElement(String type, double meanNumHourWeek, double sdNumHourWeek, double meanDuration, double sdDuration) {
			super();
			this.type = type;
			this.numHourWeek = new NormalDistributionImpl(meanNumHourWeek, sdNumHourWeek);
			this.duration = new NormalDistributionImpl(meanDuration, sdDuration);
		}
		public double getCurrentPerformedTime() {
			return currentPerformedTime;
		}
		public void addCurrentPerformedTime(double currentPerformedTime) {
			this.currentPerformedTime += currentPerformedTime;
		}
		public String getType() {
			return type;
		}
		public Distribution getNumHourWeek() {
			return numHourWeek;
		}
		public Distribution getDuration() {
			return duration;
		}
		public void substractCurrentPerformedTime(double currentPerformedTime) {
			this.currentPerformedTime -= currentPerformedTime;
		}
		
	}
	
	//Attributes
	private final Map<String, AgendaElement> elements = new HashMap<String, AgendaElement>();
	
	//Methods
	public void addElement(String type, Distribution numHourWeek, Distribution duration) {
		AgendaElement element = new AgendaElement(type, numHourWeek, duration); 
		elements.put(element.getType(), element);
	}
	public void addElement(String type, double meanNumHourWeek, double sdNumHourWeek, double meanDuration, double sdDuration) {
		AgendaElement element = new AgendaElement(type, meanNumHourWeek, sdNumHourWeek, meanDuration, sdDuration); 
		elements.put(element.getType(), element);
	}
	public Map<String, AgendaElement> getElements() {
		return elements;
	}
	
}
