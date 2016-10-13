/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;

/**
 * @author amit
 */

public final class OuterCordonLinks{
	/*
	 * P -- Patna
	 * F -- Fatua
	 * PU -- Punpun
	 * M -- Muzafarpur
	 * D -- Danapur
	 * N -- Noera (patna)
	 */
	private final Map<String, String> countingStation2Link ;
	
	public OuterCordonLinks (final PatnaNetworkType pnt){
		countingStation2Link = new HashMap<>();
		switch (pnt) {
		case osmNetwork:
			// x=fatua
			countingStation2Link.put("OC1_P2X", "1097");
			countingStation2Link.put("OC1_X2P", "1096");
			// x=fatua
			countingStation2Link.put("OC2_P2X", "2890");
			countingStation2Link.put("OC2_X2P", "2891");
			// x=Punpun
			countingStation2Link.put("OC3_P2X", "1332");
			countingStation2Link.put("OC3_X2P", "1333");
			// x= Muzafarpur
			countingStation2Link.put("OC4_P2X", "gangaSetuLink_1");
			countingStation2Link.put("OC4_X2P", "gangaSetuLink_2");
			// x= danapur
			countingStation2Link.put("OC5_P2X", "1616-2068-9430");
			countingStation2Link.put("OC5_X2P", "9429-2067-1615");
			// x= fatua; fatua to noera (Patna)
			countingStation2Link.put("OC6_X2P", "2058");
			//x = fatua; noera to fatua
			countingStation2Link.put("OC6_P2X", "2057");
			// x= danapur
			countingStation2Link.put("OC7_P2X", "738");
			countingStation2Link.put("OC7_X2P", "739");
			break;
		case shpNetwork: //following links are used if network is created from transcad data Amit June 2016
			// x=fatua
			countingStation2Link.put("OC1_P2X", "13878-13876");
			countingStation2Link.put("OC1_X2P", "1387610000-1387810000");
			// x=fatua
			countingStation2Link.put("OC2_P2X", "18237-1823810000-1825010000-1825110000-16398-1851010000-1851310000-1851210000-18505");
			countingStation2Link.put("OC2_X2P", "1850510000-18512-18513-18510-1639810000-18251-18250-18238-1823710000");
			// x=Punpun
			countingStation2Link.put("OC3_P2X", "4908-490910000-4517-4776");
			countingStation2Link.put("OC3_X2P", "477610000-451710000-4909-490810000");
			// x= Muzafarpur
			countingStation2Link.put("OC4_P2X", "1683110000-1683210000-1678010000-1684210000-1678210000-1685010000-1684510000-1684710000-1684410000-16853-1668010000-1727110000-1731610000-1734510000-17268-1735410000-1735510000-1639510000");
			countingStation2Link.put("OC4_X2P", "16395-17355-17354-1726810000-17345-17316-17271-16680-1685310000-16844-16847-16845-16850-16782-16842-16780-16832-16831");
			// x= danapur
			countingStation2Link.put("OC5_P2X", "860310000-8604-852910000-857810000-8568-857410000-856910000-857310000-857110000-854910000-8560-8555-855810000-778310000-7946-794210000-794410000-794310000-7919-7929-789710000-7913-7914-791510000-790310000-791010000-7852-789210000-788510000-7888-7853-786210000-2910000-437-475-476-47310000-474-43910000-446-464-46610000-426");
			countingStation2Link.put("OC5_X2P", "42610000-466-46410000-44610000-439-47410000-473-47610000-47510000-43710000-29-7862-785310000-788810000-7885-7892-785210000-7910-7903-7915-791410000-791310000-7897-792910000-791910000-7943-7944-7942-794610000-7783-8558-855510000-856010000-8549-8571-8573-8569-8574-856810000-8578-8529-860410000-8603");
			// x= fatua; fatua to noera (Patna)
			countingStation2Link.put("OC6_X2P", "1457-1363-1536-155610000-135810000-169510000-169110000-169610000-170110000-170010000-1349-181410000-181110000-180410000-181710000-177810000-1848");
			//x = fatua; noera to fatua
			countingStation2Link.put("OC6_P2X", "184810000-1778-1817-1804-1811-1814-134910000-1700-1701-1696-1691-1695-1358-1556-153610000-136310000-145710000");
			// x= danapur
			countingStation2Link.put("OC7_P2X", "219410000-2128");
			countingStation2Link.put("OC7_X2P", "212810000-2194");
			break;
		default:
			throw new RuntimeException(pnt+" is not recognized. Aborting ..");
		}
	}

	public String getCountingStation(final String linkId){
		for (Entry<String, String> e : countingStation2Link.entrySet()) {
			if (e.getValue().equals(linkId)) return e.getKey();
		}
		return null;
	}
	
	public Id<Link> getLinkId(final String countingStation){
		return Id.createLinkId( countingStation2Link.get(countingStation) );
	}
	
	public Map<String, String> getCountingStationToLink(){
		return this.countingStation2Link;
	}
}
