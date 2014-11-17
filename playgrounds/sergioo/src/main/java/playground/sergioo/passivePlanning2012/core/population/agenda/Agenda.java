package playground.sergioo.passivePlanning2012.core.population.agenda;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math3.distribution.NormalDistribution;

public class Agenda {
	
	//Classes
	public class AgendaElement {
		
		//Attributes
		private final String type;
		private final Distribution numHourWeek;
		private final Distribution duration;
		private int numObservations = 0;
		private double performedTime = 0;
		
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
		public String getType() {
			return type;
		}
		public Distribution getNumHourWeek() {
			return numHourWeek;
		}
		public Distribution getDuration() {
			return duration;
		}
		public void setNumObservations(int numObservations) {
			this.numObservations = numObservations;
		}
		public double getCurrentPerformedTime() {
			return performedTime;
		}
		public void addCurrentPerformedTime(double duration) {
			performedTime += duration;
		}
		public void substractCurrentPerformedTime(double duration) {
			performedTime -= duration;
		}
		public void resetCurrentPerformedTime() {
			performedTime = 0;
		}
		
	}
	
	//Attributes
	private final Map<String, AgendaElement> elements = new HashMap<String, AgendaElement>();
	
	//Methods
	public void addElement(String type, double x) {
		AgendaElement element = new AgendaElement(type, new NormalDistributionImpl(x/3600, 1), new NormalDistributionImpl(x, 1000));
		element.setNumObservations(1);
		elements.put(type, element);
	}
	public void addElement(String type, double xA, double xB) {
		double mean = (xA+xB)/2;
		double sd = Math.abs(xA-xB)/2;
		AgendaElement element = new AgendaElement(type, new NormalDistributionImpl((xA+xB)/3600, 1), new NormalDistributionImpl(mean, sd));
		element.setNumObservations(2);
		elements.put(type, element);
	}
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
	public boolean containsType(String type) {
		return elements.keySet().contains(type);
	}
	public void addObservation(String type, double x) {
		AgendaElement element = elements.get(type); 
		double n=element.numObservations;
		if(n>0) {
			double meanP = ((NormalDistribution)element.getDuration()).getMean();
			double varP = Math.pow(((NormalDistributionImpl)element.getDuration()).getStandardDeviation(), 2);
			double mean = n*meanP/(n+1)+x/(n+1);
			double sd = n==1?Math.abs(meanP-x)/2:Math.sqrt(n*(varP+meanP-mean)/(n+1)+(x-mean)/(n+1));
			if(sd<1000)
				sd=1000;
			((NormalDistributionImpl)element.getDuration()).setMean(mean);
			((NormalDistributionImpl)element.getDuration()).setStandardDeviation(sd);
			element.numObservations++;
		}
		((NormalDistributionImpl)element.getNumHourWeek()).setMean(((NormalDistribution)element.getNumHourWeek()).getMean()+x/3600);
	}
	public void reset() {
		for(AgendaElement agendaElement:elements.values())
			agendaElement.resetCurrentPerformedTime();
	}
	
}
