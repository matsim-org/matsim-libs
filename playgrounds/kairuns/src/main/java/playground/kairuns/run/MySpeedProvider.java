/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.kairuns.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nagel
 *
 */
final class MySpeedProvider implements BeforeMobsimListener,AfterMobsimListener {
	private final static Logger log = Logger.getLogger( MySpeedProvider.class );

	final class LinkInfo {
		double[] cnt = new double[36] ;
		void addCnt( double time ) {
			//			log.warn("adding cnt") ;
			int bin = time2bin(time) ;
			cnt[bin]++ ;
		}
		double getRawFlowPerSec( double time ) {
			int bin = time2bin(time) ;
			return cnt[bin]/3600. ;
		}
		void reset(){ 
			for ( int ii=0 ; ii<cnt.length ; ii++ ) {
				cnt[ii] = 0. ;
			}
		}
		int time2bin(double time) {
			int bin = (int) (time/3600.);
			if ( bin > cnt.length - 1 ) {
				bin = cnt.length - 1 ;
			}
			return bin ;
		}
	}

	final class MyObserver implements BasicEventHandler {
		Map<Id<Link>,LinkInfo> map = new HashMap<>() ;
		@Override public void reset(int iteration) {
		}
		public void ownReset() {
			for ( LinkInfo info : map.values() ) {
				info.reset();
			}
		}
		@Override public void handleEvent(Event event) {
			if ( event instanceof LinkEnterEvent ) {
				//				log.warn("link enter event") ;
				double now = event.getTime() ;
				Id<Link> linkId = ((LinkEnterEvent) event).getLinkId() ;
				LinkInfo info = map.get( linkId ) ;
				if ( info == null ) {
					info = new LinkInfo() ;
					map.put( linkId, info) ;
				}
				info.addCnt( now );
			}
		}
	}
	private final MyObserver observer = new MyObserver();
	private final Network network;
	private final Map<String, TravelTime> travelTimes;
	@Inject MySpeedProvider( EventsManager events, Network network, Map<String, TravelTime> travelTimes ) {
		this.travelTimes = travelTimes;
		events.addHandler( observer );
		this.network = network ;
	}
	private static double speedFactorBasedOnEWS(double cap_per_sec, double flow_per_sec2) {
		cap_per_sec *= KNBerlinControler.capFactorForEWS ;
		double mult = (1+flow_per_sec2/cap_per_sec) * (1-flow_per_sec2/cap_per_sec) ;
		if ( mult<0.1 ) mult=0.1 ;
		return mult;

		/*
		 * Die IVV-Funktionen (BVWP Hauptbericht 2003 s.150) lassen sich ganz gut approximieren mit:
		 * if ( flow < cap ) {
		 *    v = (1+flow/cap)*(1-flow/cap)*vmax ;
		 * } else {
		 *    v = 5km/h or 10km/h or 20km/h ; // replace by 0.1*vmax
		 * }
		 * Verweist auf EWS; dort stehen tatsächlich die Formeln; das ist hyper-aufwändig mit exp, coth, etc. etc. 
		 */
	}
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		{
			List<NetworkChangeEvent> changeEvents = new ArrayList<>() ;
			int cnt1 = 0 ;
			for ( Entry<Id<Link>, LinkInfo> entry : observer.map.entrySet() ) {
				Link link = network.getLinks().get(entry.getKey()) ;
				Gbl.assertNotNull(link);

				LinkInfo info = entry.getValue() ;
				for( int hour=0 ; hour<36; hour++ ) {

					double freespeed = link.getFreespeed() ;

//					double cap_per_sec = link.getFlowCapacityPerSec() * KNBerlinControler.sampleFactor ;
					double cap_per_sec = link.getFlowCapacityPerSec() * event.getServices().getConfig().qsim().getFlowCapFactor() ;

					double flow_per_sec = info.getRawFlowPerSec(hour*3600+1) ; // yyyyyy 

					final double speedFactor = speedFactorBasedOnEWS(cap_per_sec,  flow_per_sec);

					double speed = freespeed * speedFactor ;

					NetworkChangeEvent changeEvent = new NetworkChangeEvent( hour*3600. ) ;
					changeEvent.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  speed ));
					changeEvent.addLink(link);
					changeEvents.add(changeEvent) ;

					if ( speedFactor < 0.99 && cnt1<=10 ) {
						log.warn( "LinkId=" + link.getId() + "; hour=" + hour  ) ;
						log.warn( "cap_per_sec=" + cap_per_sec + "; flow_per_sec=" + flow_per_sec ) ;
						log.warn( "speedFactor=" + speedFactor ) ;
						log.warn("===");
						if ( cnt1==10 )  {
							log.warn( Gbl.FUTURE_SUPPRESSED );
						}
						cnt1++ ;
					}
				}
			}

			NetworkUtils.setNetworkChangeEvents(network, changeEvents );
			// (this also clears previously existing networkChangeEvents)

			observer.ownReset() ;
		}

		// ===
		{
			int cnt2= 0 ;
			for ( Link link : network.getLinks().values() ) {
				double fTravelTime = link.getLength()/link.getFreespeed() ;
				for ( double now = 6.*3600+300 ; now < 10.*3600 ; now+=900 ) {
					double cTravelTime = travelTimes.get( TransportMode.car ).getLinkTravelTime(link, now, null, null) ;
					if (cTravelTime > fTravelTime+3 && cnt2 <= 10) {
						log.warn("linkId=" + link.getId() + "; now=" + (now/3600) + "; freeTime=" + fTravelTime + "; cTime=" + cTravelTime ) ;
						if (cnt2==10) log.warn( Gbl.FUTURE_SUPPRESSED );
						cnt2++ ;
					}
				}
			}
		}
	}
	@Override public void notifyBeforeMobsim( BeforeMobsimEvent event ) {
		//		List<NetworkChangeEvent> changeEvents = new ArrayList<>() ;
		//		NetworkUtils.setNetworkChangeEvents(network, changeEvents );
	}


}
