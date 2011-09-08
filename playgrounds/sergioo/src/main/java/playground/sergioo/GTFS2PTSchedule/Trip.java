package playground.sergioo.GTFS2PTSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;

public class Trip {
	
	//Attributes
	private final Service service;
	private final Shape shape;
	private final String name;
	private final SortedMap<Integer,StopTime> stopTimes; 
	private final List<Frequency> frequencies;
	private List<Link> links;
	//Methods
	/**
	 * @param stopTimes
	 * @param service
	 * @param shape
	 */
	public Trip(Service service, Shape shape, String name) {
		super();
		this.service = service;
		this.shape = shape;
		this.name = name;
		stopTimes = new TreeMap<Integer, StopTime>();
		frequencies = new ArrayList<Frequency>();
		links = new ArrayList<Link>();
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
	 * @return the name
	 */
	public String getName() {
		return name;
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
