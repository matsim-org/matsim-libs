package playground.gregor.multidestpeds.io.openfoamexport;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;


public class PedestrianGroup {

	private Coordinate direction;
	private final double freespeed = 1.34;
	private final String name;
	private String orgin;
	private final List<Destination> destionations = new ArrayList<Destination>();


	public PedestrianGroup(String name) {
		this.name = name;
	}



	public void setOrigin(String origin, Coordinate direction) {
		this.orgin = origin;
		this.direction = direction;
	}

	public String getOrigin() {
		return this.orgin;
	}

	public Coordinate getOriginDirection() {
		return this.direction;
	}

	public void addDestination(String dest, double pot, Coordinate direction) {
		Destination d = new Destination();
		d.name = dest;
		d.potential = pot;
		d.direction = direction;
		this.destionations.add(d);
	}

	public double getFreespeed() {
		return this.freespeed;
	}

	/*package*/ static class Destination {
		String name;
		double potential;
		Coordinate direction;
	}

	public String getName() {
		return this.name;
	}



	public List<Destination> getDestinations() {
		return this.destionations;
	}


}
