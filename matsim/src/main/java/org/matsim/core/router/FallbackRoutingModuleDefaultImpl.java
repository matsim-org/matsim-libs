package org.matsim.core.router;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.Collections;
import java.util.List;

class FallbackRoutingModuleDefaultImpl implements  FallbackRoutingModule {

	@Deprecated // #deleteBeforeRelease : only used to retrofit plans created since the merge of fallback routing module (sep'-dec'19)
	public static final String _fallback = "_fallback";

	@Inject private RoutingConfigGroup pcrCfg;
	@Inject private Config config ;
	@Inject private Population population ;
	@Inject private Network network ;

	@Override public List<? extends PlanElement> calcRoute( RoutingRequest request ){
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();

		Leg leg = population.getFactory().createLeg( TransportMode.walk ) ;
		Coord fromCoord = FacilitiesUtils.decideOnCoord( fromFacility, network, config );
		Coord toCoord = FacilitiesUtils.decideOnCoord( toFacility, network, config ) ;
		Id<Link> dpLinkId = FacilitiesUtils.decideOnLink( fromFacility, network ).getId() ;
		Id<Link> arLinkId = FacilitiesUtils.decideOnLink( toFacility, network ).getId() ;
		/*
		 * Even TransportMode.walk needs an "UltimateFallbackRoutingModule", but for all other modes (pt, drt, ...) it
		 * would be better if we would try the walkRouter first and fall back to "UltimateFallbackRoutingModule" or a
		 * handwritten teleported walk like below only if the walkRouter returns null. - gl/kn-dec'19
		 */
		NetworkRoutingInclAccessEgressModule.routeBushwhackingLeg( person, leg, fromCoord, toCoord, departureTime, dpLinkId, arLinkId, population.getFactory(),
				pcrCfg.getModeRoutingParams().get(TransportMode.walk) ) ;
		return Collections.singletonList( leg ) ;
	}
}
