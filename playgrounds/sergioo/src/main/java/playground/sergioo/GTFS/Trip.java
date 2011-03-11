package playground.sergioo.GTFS;

import java.util.HashMap;

public class Trip {
	
	//Attributes
	private Service service;
	private Shape shape;
	private HashMap<Integer,StopTime> stopTimes; 
	
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
	}
	/**
	 * @return the stopTime
	 */
	public StopTime getStopTime(Integer key) {
		return stopTimes.get(key);
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
	 * Puts a new stopTime
	 * @param key
	 * @param stopTime
	 */
	public void putStopTime(Integer key, StopTime stopTime) {
		stopTimes.put(key, stopTime);
	}
	
}
