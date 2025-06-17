package org.matsim.core.router;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

public final class MultimodalLinkChooserDefaultImpl implements MultimodalLinkChooser {
    // the class is public so it can be used as a delegate ...

    private static final Logger log = LogManager.getLogger( FacilitiesUtils.class ) ;

    @Inject MultimodalLinkChooserDefaultImpl(){}
    // ... but the constructor is non-public so one is forced to use it via injection.  kai, may'25

    @Override
    public Link decideAccessLink(RoutingRequest request, String networkMode, Network network) {
    	return decideOnLink(request.getFromFacility(), network);
    }

    @Override
    public Link decideEgressLink(RoutingRequest request, String networkMode, Network network) {
    	return decideOnLink(request.getToFacility(), network);
    }

    private Link decideOnLink(Facility facility, Network network) {
        Link accessActLink = null ;

        Id<Link> accessActLinkId = null ;
        try {
            accessActLinkId = facility.getLinkId() ;
        } catch ( Exception ee ) {
            // there are implementations that throw an exception here although "null" is, in fact, an interpretable value. kai, oct'18
        }

        if ( accessActLinkId!=null ) {
            accessActLink = network.getLinks().get( facility.getLinkId() );
            // i.e. if street address is in mode-specific subnetwork, I just use that, and do not search for another (possibly closer)
            // other link.

        }

        if ( accessActLink==null ) {
            // this is the case where the postal address link is NOT in the subnetwork, i.e. does NOT serve the desired mode,
            // OR the facility does not have a street address link in the first place.

            if( facility.getCoord()==null ) {
                throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
            }

            accessActLink = NetworkUtils.getNearestLink(network, facility.getCoord()) ;
            if ( accessActLink == null ) {
                log.warn("Facility without link for which no nearest link on the respective network could be found. " +
                        "About to abort. Writing out the first 10 links to understand which subnetwork was used to help debugging.");
                int ii = 0 ;
                for ( Link link : network.getLinks().values() ) {
                    if ( ii==10 ) {
                        break ;
                    }
                    ii++ ;
                    log.warn( link );
                }
            }
            Gbl.assertNotNull(accessActLink);
        }
        return accessActLink;

        // I just found out that there are facilities that insist on links that may not be postal addresses since they cannot be reached by car.
        // TransitStopFacility is an example.  kai, jun'19

    }
}
