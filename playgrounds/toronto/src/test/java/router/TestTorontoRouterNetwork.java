package router;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.exceptions.NetworkFormattingException;
import playground.toronto.router.routernetwork.TorontoTransitRouterNetwork;

import com.vividsolutions.jts.util.Assert;

public class TestTorontoRouterNetwork {

	/**
	 * Test Network:
	 * 
	 * 			(A)				  (C)
	 *   4---		1				2
	 *   			  \\			|
	 *   				\\			|
	 *   2.5-			  3 -.-.-.- 4
	 *   				 / \\		|
	 *   				/	 \\		|
	 *   2---		5--6------++----7------8  (B)
	 *   					  ||    |
	 *   					  ||    |
	 *   					  ||    |
	 *   0---				  9    10
	 *   
	 *   			|  |  |   |     |      |
	 *   			0  1 1.2  2    2.5     3 
	 *   			
	 *   Line A: 1->3->9
	 *   Line B: 5->6->7->8
	 *   Line c: 2->4->7->10
	 *   
	 *   Line B is connected to A via a transfer link from 6-3
	 *   Line C is connected to A via a walk link from 4-3
	 *   Line B and C are connected at stop/node 7 
	 *   			
	 */
	@Test
	public void testTrnCreation() throws UnsupportedEncodingException, NetworkFormattingException {
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario);		
		reader.parse(new ByteArrayInputStream(writeTestNetworkToXml().getBytes("UTF-8")));
		Network network = scenario.getNetwork();
		Assert.isTrue(network != null, "Network is null!"); 
		Assert.isTrue(network.getNodes().size() == 10, "Network does not contain 10 links!");
		Assert.isTrue(network.getLinks().size() == 20, "Network does not contain 20 links!");
		
		new TransitScheduleReaderV1(scenario).parse(new ByteArrayInputStream(writeTestScheduleToXml().getBytes("UTF-8")));
		TransitSchedule schedule = scenario.getTransitSchedule();
		Assert.isTrue(schedule != null, "Schedule is null!");
		Assert.isTrue(schedule.getTransitLines().size() == 3, "Schedule does not have 3 lines!");
		Assert.isTrue(schedule.getFacilities().size() == 11, "Schedule does not have 11 stops!");
		
		TransitRouterNetwork TRN = TorontoTransitRouterNetwork.createTorontoTransitRouterNetwork(network, schedule, 1.0);
		Assert.isTrue(TRN.getLinks().size() == 14);
		
		System.out.println("Done.");
	}
	
	private String writeTestScheduleToXml(){
		String s = "";
		
		s += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		s += "<!DOCTYPE transitSchedule SYSTEM \"http://www.matsim.org/files/dtd/transitSchedule_v1.dtd\">";
		s += "<transitSchedule>";
		s += "<transitStops>";
		s += "<stopFacility id=\"A1\" x=\"0\" y=\"400\" linkRefId=\"3-1\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"A3\" x=\"120\" y=\"250\" linkRefId=\"1-3\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"A9\" x=\"200\" y=\"0\" linkRefId=\"3-9\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"B5\" x=\"0\" y=\"200\" linkRefId=\"6-5\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"B6\" x=\"100\" y=\"200\" linkRefId=\"5-6\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"B7\" x=\"250\" y=\"200\" linkRefId=\"6-7\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"B8\" x=\"300\" y=\"200\" linkRefId=\"7-8\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"C2\" x=\"250\" y=\"400\" linkRefId=\"4-2\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"C4\" x=\"250\" y=\"250\" linkRefId=\"2-4\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"C7\" x=\"250\" y=\"200\" linkRefId=\"4-7\" isBlocking=\"true\" />";
		s += "<stopFacility id=\"C10\" x=\"250\" y=\"0\" linkRefId=\"7-10\" isBlocking=\"true\" />";
		s += "</transitStops>";
		
		s += "<transitLine id=\"A\">";
			s += "<transitRoute id=\"A1\">";
				s += "<transportMode>Train</transportMode>";
				s += "<routeProfile>";
					s += "<stop refId=\"A1\" departureOffset=\"00:00:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"A3\" departureOffset=\"00:01:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"A9\" departureOffset=\"00:02:00\" awaitDeparture=\"false\" />";
				s += "</routeProfile>";
				s += "<route>";
					s += "<link refId=\"3-1\" />";
					s += "<link refId=\"1-3\" />";
					s += "<link refId=\"3-9\" />";
				s += "</route>";
				s += "<departures>";
				s += "</departures>";
			s += "</transitRoute>";
		s += "</transitLine>";

		s += "<transitLine id=\"B\">";
			s += "<transitRoute id=\"B1\">";
				s += "<transportMode>Bus</transportMode>";
				s += "<routeProfile>";
					s += "<stop refId=\"B5\" departureOffset=\"00:00:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"B6\" departureOffset=\"00:01:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"B7\" departureOffset=\"00:02:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"B8\" departureOffset=\"00:03:00\" awaitDeparture=\"false\" />";
				s += "</routeProfile>";
				s += "<route>";
					s += "<link refId=\"6-5\" />";
					s += "<link refId=\"5-6\" />";
					s += "<link refId=\"6-7\" />";
					s += "<link refId=\"7-8\" />";
				s += "</route>";
				s += "<departures>";
				s += "</departures>";
			s += "</transitRoute>";
		s += "</transitLine>";
		
		s += "<transitLine id=\"C\">";
			s += "<transitRoute id=\"C1\">";
				s += "<transportMode>Bus</transportMode>";
				s += "<routeProfile>";
					s += "<stop refId=\"C2\" departureOffset=\"00:00:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"C4\" departureOffset=\"00:01:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"C7\" departureOffset=\"00:02:00\" awaitDeparture=\"false\" />";
					s += "<stop refId=\"C10\" departureOffset=\"00:03:00\" awaitDeparture=\"false\" />";
				s += "</routeProfile>";
				s += "<route>";
					s += "<link refId=\"4-2\" />";
					s += "<link refId=\"2-4\" />";
					s += "<link refId=\"4-7\" />";
					s += "<link refId=\"7-10\" />";
				s += "</route>";
				s += "<departures>";
				s += "</departures>";
			s += "</transitRoute>";
		s += "</transitLine>";
		
		s += "</transitSchedule>";
		return s;
	}
	private String writeTestNetworkToXml(){
		
		String s = "<!DOCTYPE network SYSTEM \"http://www.matsim.org/files/dtd/network_v1.dtd\">" +
				"<network>";
		s += "<nodes>";
		s += "<node id=\"1\" x=\"0\" y=\"400\" />";
		s += "<node id=\"2\" x=\"250\" y=\"400\" />";
		s += "<node id=\"3\" x=\"120\" y=\"250\" />";
		s += "<node id=\"4\" x=\"250\" y=\"250\" />";
		s += "<node id=\"5\" x=\"0\" y=\"200\" />";
		s += "<node id=\"6\" x=\"100\" y=\"200\" />";
		s += "<node id=\"7\" x=\"250\" y=\"200\" />";
		s += "<node id=\"8\" x=\"300\" y=\"200\" />";
		s += "<node id=\"9\" x=\"200\" y=\"0\" />";
		s += "<node id=\"10\" x=\"250\" y=\"0\" />";
		s += "</nodes>";
		
		s += "<links capperiod=\"01:00:00\">";
		s += "<link id=\"1-3\" from=\"1\" to=\"3\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Train\" />";
		s += "<link id=\"3-9\" from=\"3\" to=\"9\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Train\" />";
		s += "<link id=\"2-4\" from=\"2\" to=\"4\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"4-7\" from=\"4\" to=\"7\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"7-10\" from=\"7\" to=\"10\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"5-6\" from=\"5\" to=\"6\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"6-7\" from=\"6\" to=\"7\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"7-8\" from=\"7\" to=\"8\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"6-3\" from=\"6\" to=\"3\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Transfer\" />";
		s += "<link id=\"4-3\" from=\"4\" to=\"3\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Walk\" />";
		s += "<link id=\"3-1\" from=\"3\" to=\"1\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Train\" />";
		s += "<link id=\"9-3\" from=\"9\" to=\"3\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Train\" />";
		s += "<link id=\"4-2\" from=\"4\" to=\"2\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"7-4\" from=\"7\" to=\"4\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"10-7\" from=\"10\" to=\"7\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"6-5\" from=\"6\" to=\"5\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"7-6\" from=\"7\" to=\"6\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"8-7\" from=\"8\" to=\"7\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Bus\" />";
		s += "<link id=\"3-6\" from=\"3\" to=\"6\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Transfer\" />";
		s += "<link id=\"3-4\" from=\"3\" to=\"4\" freespeed=\"50\" length=\"0\" capacity=\"999\" permlanes=\"1.0\" oneway=\"1\" modes=\"Walk\" />";
		s += "</links>"; 
		
		s += "</network>";
		
		return s;
	}
	
	public void testTorontoSchedule() throws NetworkFormattingException{
		String networkFileName = "D:/MATSim/Schedule Data/Files from TAC Paper 2013/Second Full Draft - June 25_2013/2012NetworkWtihTransitFixedAndTypes.xml";
		String scheduleFileName = "D:/MATSim/Schedule Data/Files from TAC Paper 2013/Second Full Draft - June 25_2013/scheduletrialWithLowDeparturesMergedJune24.xml";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFileName);
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(scheduleFileName);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Network baseNetwork = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitRouterNetwork TRN = TorontoTransitRouterNetwork.createTorontoTransitRouterNetwork(baseNetwork, schedule, 10.0);
		
		Assert.isTrue(TRN != null, "Transit Router Network is null!");
		
		System.out.println("TRN had " + TRN.getLinks().size() + " links.");
	}
}
