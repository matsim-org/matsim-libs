package org.matsim.contrib.carsharing.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.PermissibleModesCalculator;

public class CarsharingSubTourPermissableModesCalculator implements PermissibleModesCalculator{

	private final Scenario scenario;
	private final List<String> availableModes;
	private final List<String> availableModesWithoutCar;
	
	public CarsharingSubTourPermissableModesCalculator(final Scenario scenario, final String[] availableModes) {
		this.scenario = scenario;
		this.availableModes = Arrays.asList(availableModes);
		
		if ( this.availableModes.contains( TransportMode.car ) ) {
			final List<String> l = new ArrayList<String>( this.availableModes );
			while ( l.remove( TransportMode.car ) ) {}
			this.availableModesWithoutCar = Collections.unmodifiableList( l );
		}
		else {
			this.availableModesWithoutCar = this.availableModes;
		}
	}
	
	@Override
	public Collection<String> getPermissibleModes(Plan plan) {
		final PersonImpl person;
		List<String> l; 
		try {
			person = (PersonImpl) plan.getPerson();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException( "I need a PersonImpl to get car availability" );
		}

		final boolean carAvail =
			!"no".equals( person.getLicense() ) &&
			!"never".equals( person.getCarAvail() );
		if (carAvail)			 
			  l = new ArrayList<String>( this.availableModes );
		  else
			  l = new ArrayList<String>( this.availableModesWithoutCar );
		
		 if (Boolean.parseBoolean(scenario.getConfig().getModule("TwoWayCarsharing").getParams().get("useTwoWayCarsharing"))
		
				 && Boolean.parseBoolean((String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "RT_CARD"))) {
			 
			 
			 l.add("twowaycarsharing");
			 
		 }
		
		return l;
	}

}
