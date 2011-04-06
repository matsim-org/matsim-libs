package playground.sergioo.GTFS;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;

public class Trip {
	
	//Attributes
	private Service service;
	private Shape shape;
	private SortedMap<Integer,StopTime> stopTimes; 
	private List<Frequency> frequencies;
	private List<Link> links;
	private Route route;
	//Methods
	/**
	 * @param stopTimes
	 * @param service
	 * @param shape
	 */
	public Trip(Service service, Shape shape, Route route) {
		super();
		this.service = service;
		this.shape = shape;
		this.stopTimes = new TreeMap<Integer, StopTime>();
		this.frequencies = new ArrayList<Frequency>();
		this.links = new ArrayList<Link>();
		this.route = route;
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
	public SortedMap<Integer, StopTime> getStopTimes() {
		return stopTimes;
	}
	/**
	 * @return the frequencies
	 */
	public List<Frequency> getFrequencies() {
		return frequencies;
	}
	/**
	 * @return the route
	 */
	public List<Link> getLinks() {
		return links;
	}
	/**
	 * @return the route
	 */
	public Route getRoute() {
		return route;
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
	 * Adds a new frequency
	 * @param frequency
	 */
	public void addFrequency(Frequency frequency) {
		frequencies.add(frequency);
	}
	/**
	 * @param route the route to set
	 */
	public void setRoute(List<Link> route) {
		this.links = route;
	}
	/**
	 * Adds a new link
	 * @param link
	 */
	public void addLink(Link link) {
		links.add(link);
	}
}
