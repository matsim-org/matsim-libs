package org.matsim.interfaces.basic.v01;

import org.matsim.config.Config;
import org.matsim.interfaces.core.v01.Coord;

public interface BasicScenario {

	public BasicNetwork getNetwork() ;

	public BasicFacilities getFacilities() ;

	public BasicPopulation getPopulation() ;

	public Config getConfig();

	public Id createId(String string);

	public Coord createCoord(double d, double e);

}