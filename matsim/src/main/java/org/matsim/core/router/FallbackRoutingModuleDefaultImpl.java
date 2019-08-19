package org.matsim.core.router;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.Collections;
import java.util.List;

class FallbackRoutingModuleDefaultImpl implements  FallbackRoutingModule {

	public static final String _fallback = "_fallback";


	@Inject private Population population ;
	@Inject private Network network ;

	@Override public List<? extends PlanElement> calcRoute( Facility fromFacility, Facility toFacility, double departureTime, Person person ){
		Leg leg = population.getFactory().createLeg( "dummy" ) ;
		Coord fromCoord = FacilitiesUtils.decideOnCoord( fromFacility, network );
		Coord toCoord = FacilitiesUtils.decideOnCoord( toFacility, network ) ;
		Id<Link> dpLinkId = FacilitiesUtils.decideOnLink( fromFacility, network ).getId() ;
		Id<Link> arLinkId = FacilitiesUtils.decideOnLink( toFacility, network ).getId() ;
		NetworkRoutingInclAccessEgressModule.routeBushwhackingLeg( person, leg, fromCoord, toCoord, departureTime, dpLinkId, arLinkId, population.getFactory() ) ;
		return Collections.singletonList( leg ) ;
	}

	@Override public StageActivityTypes getStageActivityTypes(){
		return EmptyStageActivityTypes.INSTANCE ;
	}
}
