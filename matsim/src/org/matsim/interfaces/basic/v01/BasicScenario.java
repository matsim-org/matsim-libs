package org.matsim.interfaces.basic.v01;

import org.matsim.config.Config;
import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;

public interface BasicScenario {

	public BasicNetwork getNetwork() ;

//	@Deprecated
//	public BasicFacilities getFacilities() ;
	
	public BasicPopulation getPopulation() ;
	
	public Config getConfig();

	public Id createId(String string);

	public Coord createCoord(double d, double e);

}