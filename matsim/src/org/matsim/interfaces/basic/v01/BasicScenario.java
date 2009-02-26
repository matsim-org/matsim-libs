package org.matsim.interfaces.basic.v01;

import org.matsim.config.Config;
import org.matsim.facilities.Facilities;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;

public interface BasicScenario {

	public BasicNetwork getNetwork() ;

//	@Deprecated
//	public BasicFacilities getFacilities() ;
	
	public BasicPopulation getPopulation() ;
	
	public Config getConfig();

	public Id createId(String string);

	public Coord createCoord(double d, double e);

}