package playground.sergioo.GTFS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Trip {
	
	//Attributes
	private Service service;
	private Shape shape;
	private HashMap<Integer,StopTime> stopTimes; 
	private List<Frequency> frequencies;
	//Methods
	/**
	 * @param stopTimes
	 * @param service
	 * @param shape
	 */
	public Trip(Service service, Shape shape) {
		super();
		this.service = service;
		this.shape = shape;
		this.stopTimes = new HashMap<Integer, StopTime>();
		this.frequencies = new ArrayList<Frequency>();
	}
	/**
	 * @return the service
	 */
	public Service getService() {
		return service;
	}
	/**
	 * @return the shape
	 */
	public Shape getShape() {
		return shape;
	}
	/**
	 * @return the stopTimes
	 */
	public HashMap<Integer, StopTime> getStopTimes() {
		return stopTimes;
	}
	/**
	 * @return the frequencies
	 */
	public List<Frequency> getFrequencies() {
		return frequencies;
	}
	/**
	 * Puts a new stopTime
	 * @param key
	 * @param stopTime
	 */
	public void putStopTime(Integer key, StopTime stopTime) {
		stopTimes.put(key, stopTime);
	}
	/**
	 * Adds a new Frequency
	 * @param frequency
	 */
	public void addFrequency(Frequency frequency) {
		frequencies.add(frequency);
	}
	
}
