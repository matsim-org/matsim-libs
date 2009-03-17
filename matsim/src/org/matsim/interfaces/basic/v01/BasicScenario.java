package org.matsim.interfaces.basic.v01;

import org.matsim.config.Config;
import org.matsim.interfaces.basic.v01.facilities.BasicFacilities;
import org.matsim.interfaces.basic.v01.network.BasicNetwork;
import org.matsim.interfaces.basic.v01.population.BasicPopulation;

public interface BasicScenario {

	public BasicNetwork getNetwork() ;

	public BasicFacilities getFacilities() ;

	public BasicPopulation getPopulation() ;

	public Config getConfig();

	public Id createId(String string);

	public Coord createCoord(double d, double e);

}