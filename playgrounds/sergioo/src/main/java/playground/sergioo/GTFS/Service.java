package playground.sergioo.GTFS;

import java.util.ArrayList;
import java.util.Collection;

public class Service {
	
	//Attributes
	private boolean[] days;
	private String initialDate;
	private String finalDate;
	private Collection<String> additions;
	private Collection<String> exceptions;
	
	//Methods
	/**
	 * @param days
	 * @param initialDate
	 * @param finalDate
	 */
	public Service(boolean[] days, String initialDate, String finalDate) {
		super();
		this.days = days;
		this.initialDate = initialDate;
		this.finalDate = finalDate;
		this.additions = new ArrayList<String>();
		this.exceptions = new ArrayList<String>();
	}
	/**
	 * @return the days
	 */
	public boolean[] getDays() {
		return days;
	}
	/**
	 * @return the initialDate
	 */
	public String getInitialDate() {
		return initialDate;
	}
	/**
	 * @return the finalDate
	 */
	public String getFinalDate() {
		return finalDate;
	}
	/**
	 * @return the additions
	 */
	public Collection<String> getAdditions() {
		return additions;
	}
	/**
	 * @return the exceptions
	 */
	public Collection<String> getExceptions() {
		return exceptions;
	}
	/**
	 * Adds a new addition date
	 * @param addition
	 */
	public void addAddition(String addition) {
		additions.add(addition);
	}
	/**
	 * Adds a new exception date
	 * @param exception
	 */
	public void addException(String exception) {
		additions.add(exception);
	}
	
}
