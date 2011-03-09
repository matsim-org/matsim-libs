package playground.sergioo.RoutesAlternatives;

import java.util.ArrayList;
import java.util.List;

import playground.sergioo.AddressLocator.Location;

public class Route {
	
	//Attributes
	/**
	 * The initial location
	 */
	private Location origin;
	/**
	 * The end location
	 */
	private Location destination;
	/**
	 * The legs
	 */
	protected List<Leg> legs;
	/**
	 * The total time
	 */
	private double totalTime;
	
	//Methods
	/**
	 * @param origin
	 * @param destination
	 * @param distance
	 * @param totalTime
	 */
	public Route(Location origin, Location destination, double totalTime) {
		super();
		this.origin = origin;
		this.destination = destination;
		legs = new ArrayList<Leg>();
		this.totalTime = totalTime;
	}
	/**
	 * @return the legs
	 */
	public List<Leg> getLegs() {
		return legs;
	}
	/**
	 * @return the totalTime
	 */
	public double getTotalTime() {
		return totalTime;
	}
	/**
	 * 
	 * @param points
	 * @param modes
	 * @throws Exception if the number of points minus one is not the number of modes
	 */
	public void addLegs(List<String> points, List<Mode> modes) throws Exception {
		if(points.size()-1==modes.size())
			for(int i=0; i<modes.size(); i++)
				legs.add(new Leg(points.get(i), points.get(i+1), modes.get(i)));
		else
			throw new Exception("The number of points minus one must be the number of modes");
	}
	/**
	 * @return The route as a text
	 */
	public String toString() {
		String res = origin+";"+destination+";"+totalTime+"s. ";
		for(Leg leg:legs)
			res+="["+leg+"]";
		return res;
	}
}
