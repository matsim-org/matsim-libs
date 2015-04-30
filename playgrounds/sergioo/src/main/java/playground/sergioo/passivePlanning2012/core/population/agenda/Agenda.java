package playground.sergioo.passivePlanning2012.core.population.agenda;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

public class Agenda {
	
	//Classes
	public class AgendaElement {
		
		//Attributes
		private final String type;
		private RealDistribution numTimesPerPeriod;
		private RealDistribution duration;
		private int numObservations = 0;
		private double currentPerformedTime;
		
		//Constructors
		public AgendaElement(String type, RealDistribution numTimesPerPeriod, RealDistribution duration) {
			super();
			this.type = type;
			this.numTimesPerPeriod = numTimesPerPeriod;
			this.duration = duration;
			this.currentPerformedTime = numTimesPerPeriod.sample()*duration.getNumericalMean();
		}
		public AgendaElement(String type, double meanNumTimesPerPeriod, double sdNumTimesPerPeriod, double meanDuration, double sdDuration) {
			super();
			this.type = type;
			this.numTimesPerPeriod = new NormalDistribution(meanNumTimesPerPeriod, sdNumTimesPerPeriod);
			this.duration = new NormalDistribution(meanDuration, sdDuration);
			this.currentPerformedTime = numTimesPerPeriod.sample()*duration.getNumericalMean();
		}
		public String getType() {
			return type;
		}
		public RealDistribution getDuration() {
			return duration;
		}
		public void setNumObservations(int numObservations) {
			this.numObservations = numObservations;
		}
		public RealDistribution getNumTimesPerPeriod() {
			return numTimesPerPeriod;
		}
		public double getCurrentPerformedTime() {
			return currentPerformedTime;
		}
		public void performActivityTime(double duration) {
			currentPerformedTime -= duration;
		}
		public void unperformActivityTime(double duration) {
			currentPerformedTime += duration;
		}
		public void resetCurrentPerformedTime() {
			currentPerformedTime = numTimesPerPeriod.sample()*duration.getNumericalMean();
		}
		public void setDuration(RealDistribution duration) {
			this.duration = duration;
		}
		public void setNumTimesPerPeriod(RealDistribution numTimesPerPeriod) {
			this.numTimesPerPeriod = numTimesPerPeriod;
		}
		
	}
	
	//Attributes
	private final Map<String, AgendaElement> elements = new HashMap<String, AgendaElement>();
	
	//Methods
	public void addElement(String type, double x) {
		AgendaElement element = new AgendaElement(type, new NormalDistribution(x/3600, 1), new NormalDistribution(x, 3600));
		element.setNumObservations(1);
		elements.put(type, element);
	}
	public void addElement(String type, RealDistribution duration) {
		AgendaElement element = new AgendaElement(type, new NormalDistribution(duration.inverseCumulativeProbability(0.75)/3600, 1), duration);
		element.setNumObservations(1);
		elements.put(type, element);
	}
	public void addElement(String type, double xA, double xB) {
		double mean = (xA+xB)/2;
		double sd = Math.abs(xA-xB)/2;
		AgendaElement element = new AgendaElement(type, new NormalDistribution((xA+xB)/3600, 1), new NormalDistribution(mean, sd));
		element.setNumObservations(2);
		elements.put(type, element);
	}
	public void addElement(String type, RealDistribution numHourPeriod, RealDistribution duration) {
		AgendaElement element = new AgendaElement(type, numHourPeriod, duration); 
		elements.put(element.getType(), element);
	}
	public void addElement(String type, double meanNumHourPeriod, double sdNumHourPeriod, double meanDuration, double sdDuration) {
		AgendaElement element = new AgendaElement(type, meanNumHourPeriod, sdNumHourPeriod, meanDuration, sdDuration); 
		elements.put(element.getType(), element);
	}
	public Map<String, AgendaElement> getElements() {
		return elements;
	}
	public boolean containsType(String type) {
		return elements.keySet().contains(type);
	}
	public void addObservation(String type, double x) {
		AgendaElement element = elements.get(type); 
		double n=element.numObservations;
		if(n>0) {
			double meanP = element.getDuration().getNumericalMean();
			double varP = element.getDuration().getNumericalVariance();
			double mean = n*meanP/(n+1)+x/(n+1);
			double sd = n==1?Math.abs(meanP-x)/2:Math.sqrt((n*varP+(x-mean)*(x-meanP))/(n+1));
			if(sd<1000)
				sd=1000;
			NormalDistribution newDuration = new NormalDistribution(mean, sd);
			element.setDuration(newDuration);
			element.numObservations++;
		}
		NormalDistribution newNumTimesPerPeriod = new NormalDistribution((element.numTimesPerPeriod.getNumericalMean()+1)/n, Math.sqrt(element.numTimesPerPeriod.getNumericalVariance()));
		element.setNumTimesPerPeriod(newNumTimesPerPeriod);
	}
	public void reset() {
		for(AgendaElement agendaElement:elements.values())
			agendaElement.resetCurrentPerformedTime();
	}
	
}
