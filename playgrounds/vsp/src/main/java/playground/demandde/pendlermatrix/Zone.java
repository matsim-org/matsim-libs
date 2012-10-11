/**
 * 
 */
package playground.demandde.pendlermatrix;

import org.matsim.api.core.v01.Coord;

public class Zone {

	public final int id;

	public final int workplaces;

	public final int workingPopulation;

	public final Coord coord;

	public Zone(int id, int workplaces, int workingPopulation, Coord coord) {
		super();
		this.id = id;
		this.workplaces = workplaces;
		this.workingPopulation = workingPopulation;
		this.coord = coord;
	}

}