/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.wobscenario.peoplemover;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ExtractPeopleMoverNetwork {

	private final static double northernLimit =  52.5585399;
//	private final static double southernLimit =  52.3110258;
	
	//as per may 3
	private final static double southernLimit =  52.2345;
	private final static double easternLimit =  10.9617273;
	private final static double westernLimit =  10.606989;
	private final static double minFreeSpeed =  8.0;
	private final static double minCapacity =  1000.0;
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
	
	private Coord topLeft;
	private Coord bottomRight;
	private Network network = NetworkUtils.createNetwork();
	private Set<Id<Link>> positiveList;
	private Set<Id<Link>> negativeList;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ExtractPeopleMoverNetwork().run();
	}

	/**
	 * 
	 */
	public void run() {
		String inputNetworkFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-av-mar17.xml";
		String outputNetworkFileAV = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-avonly-may17.xml";
		String outputNetworkFile  = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-av-may17.xml";
		topLeft = ct.transform(new Coord(westernLimit,northernLimit));
		bottomRight = ct.transform(new Coord(easternLimit,southernLimit));
		preparePositiveList();
		prepareNegativeList();
		new MatsimNetworkReader(network).readFile(inputNetworkFile);
		int i = 0;
		for (Link l : network.getLinks().values())
		{
			if (judgeLink(l))
			{ 
				Set<String> modes = new HashSet<>();
				modes.addAll(l.getAllowedModes());
				modes.add("av");
				l.setAllowedModes(modes);
				i++;
				}
		}
		
		System.out.println(i);
		NetworkFilterManager nfm = new NetworkFilterManager(network);
		nfm.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("av")) return true;
				else
				return false;
			}
		});
		
		Network avNetwork = nfm.applyFilters();		
		NetworkFilterManager nfm2 = new NetworkFilterManager(avNetwork);
		nfm2.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
							return true;
			}
		});
		Network uncleanedAvNetwork = nfm2.applyFilters();
		new NetworkCleaner().run(avNetwork);
		for (Link l : uncleanedAvNetwork.getLinks().values())
		{
			if (!avNetwork.getLinks().containsKey(l.getId()))
			{
				Link netLink = network.getLinks().get(l.getId());
				Set<String> modes = new HashSet<>();
				modes.addAll(l.getAllowedModes());
				modes.remove("av");
				netLink.setAllowedModes(modes);
			}
		}
		
		new NetworkWriter(network).write(outputNetworkFile);
		new NetworkWriter(avNetwork).write(outputNetworkFileAV);
	}
	

	/**
	 * 
	 */
	private void prepareNegativeList() {
		negativeList = new HashSet<>();
		negativeList.add(Id.createLinkId(61308));
		negativeList.add(Id.createLinkId(61309));
		negativeList.add(Id.createLinkId(4947));
		negativeList.add(Id.createLinkId(4948));
		negativeList.add(Id.createLinkId(37324));
		negativeList.add(Id.createLinkId(37323));
		negativeList.add(Id.createLinkId(37322));
		negativeList.add(Id.createLinkId(37325));
		negativeList.add(Id.createLinkId(37318));
		negativeList.add(Id.createLinkId(37320));
		negativeList.add(Id.createLinkId(26840));
		negativeList.add(Id.createLinkId(26841));
		negativeList.add(Id.createLinkId(44820));
		negativeList.add(Id.createLinkId(44819));
		negativeList.add(Id.createLinkId(40804));
		negativeList.add(Id.createLinkId(40805));
		negativeList.add(Id.createLinkId(1453));
		negativeList.add(Id.createLinkId(1454));
		negativeList.add(Id.createLinkId(56251));
		negativeList.add(Id.createLinkId(56250));
		negativeList.add(Id.createLinkId(43212));
		negativeList.add(Id.createLinkId(43213));
		negativeList.add(Id.createLinkId(43214));
		negativeList.add(Id.createLinkId(43215));
		negativeList.add(Id.createLinkId(43210));
		negativeList.add(Id.createLinkId(43211));
		negativeList.add(Id.createLinkId(43244));
		negativeList.add(Id.createLinkId(43245));
		negativeList.add(Id.createLinkId(43243));
		negativeList.add(Id.createLinkId(43242));
		negativeList.add(Id.createLinkId(56250));
		negativeList.add(Id.createLinkId(56251));
		negativeList.add(Id.createLinkId(41243));
		negativeList.add(Id.createLinkId(41242));


	}

	/**
	 * 
	 */
	private void preparePositiveList() {
		positiveList = new HashSet<>();
		positiveList.add(Id.createLinkId(42730));
		positiveList.add(Id.createLinkId(42729));
		positiveList.add(Id.createLinkId(42728));
		positiveList.add(Id.createLinkId(42727));
		positiveList.add(Id.createLinkId(855));
		positiveList.add(Id.createLinkId(854));
		positiveList.add(Id.createLinkId(42590));
		positiveList.add(Id.createLinkId(42591));
		positiveList.add(Id.createLinkId(3966));
		positiveList.add(Id.createLinkId(3967));
		positiveList.add(Id.createLinkId(15851));
		positiveList.add(Id.createLinkId(15850));
		positiveList.add(Id.createLinkId(26410));
		positiveList.add(Id.createLinkId(26409));
		positiveList.add(Id.createLinkId(60609));
		positiveList.add(Id.createLinkId(60608));
		positiveList.add(Id.createLinkId(22618));
		positiveList.add(Id.createLinkId(22619));
		positiveList.add(Id.createLinkId(63797));
		positiveList.add(Id.createLinkId(63796));
		positiveList.add(Id.createLinkId(63794));
		positiveList.add(Id.createLinkId(63793));
		positiveList.add(Id.createLinkId(63788));
		positiveList.add(Id.createLinkId(63795));
		positiveList.add(Id.createLinkId(63790));
		positiveList.add(Id.createLinkId(63791));
		positiveList.add(Id.createLinkId(63789));
		positiveList.add(Id.createLinkId(63798));
		positiveList.add(Id.createLinkId(61974));
		positiveList.add(Id.createLinkId(61975));
		positiveList.add(Id.createLinkId(47097));
		positiveList.add(Id.createLinkId(47098));
		positiveList.add(Id.createLinkId(47099));
		positiveList.add(Id.createLinkId(47100));
		positiveList.add(Id.createLinkId(47106));
		positiveList.add(Id.createLinkId(16077));
		positiveList.add(Id.createLinkId(16082));
		positiveList.add(Id.createLinkId(67435));
		positiveList.add(Id.createLinkId(67434));
		positiveList.add(Id.createLinkId(21834));
		positiveList.add(Id.createLinkId(21835));
		positiveList.add(Id.createLinkId(41402));
		positiveList.add(Id.createLinkId(41401));
		positiveList.add(Id.createLinkId(27685));
		positiveList.add(Id.createLinkId(3883));
		positiveList.add(Id.createLinkId(27232));
		positiveList.add(Id.createLinkId(10595));
		positiveList.add(Id.createLinkId(10596));
		positiveList.add(Id.createLinkId(3074));
		positiveList.add(Id.createLinkId(3075));
		positiveList.add(Id.createLinkId(3072));
		positiveList.add(Id.createLinkId(25289));
		
		positiveList.add(Id.createLinkId(46199));
		positiveList.add(Id.createLinkId(46200));
		positiveList.add(Id.createLinkId(46197));
		positiveList.add(Id.createLinkId(46198));
		positiveList.add(Id.createLinkId(46195));
		positiveList.add(Id.createLinkId(46196));
		positiveList.add(Id.createLinkId(37442));
		positiveList.add(Id.createLinkId(37443));
		positiveList.add(Id.createLinkId(37441));
		positiveList.add(Id.createLinkId(37438));
		positiveList.add(Id.createLinkId(37114));
		positiveList.add(Id.createLinkId(37115));
		positiveList.add(Id.createLinkId(7817));
		positiveList.add(Id.createLinkId(7818));
		positiveList.add(Id.createLinkId(7816));
		positiveList.add(Id.createLinkId(7815));
		positiveList.add(Id.createLinkId(60992));
		positiveList.add(Id.createLinkId(60991));
		positiveList.add(Id.createLinkId(60989));
		positiveList.add(Id.createLinkId(60990));
		positiveList.add(Id.createLinkId(17506));
		positiveList.add(Id.createLinkId(17505));
		positiveList.add(Id.createLinkId(17507));
		positiveList.add(Id.createLinkId(17508));
		positiveList.add(Id.createLinkId(17517));
		positiveList.add(Id.createLinkId(17518));
		positiveList.add(Id.createLinkId(17519));
		positiveList.add(Id.createLinkId(17520));
		positiveList.add(Id.createLinkId(11164));
		positiveList.add(Id.createLinkId(11165));
		positiveList.add(Id.createLinkId(168));
		positiveList.add(Id.createLinkId(169));
		positiveList.add(Id.createLinkId(170));
		positiveList.add(Id.createLinkId(171));
		positiveList.add(Id.createLinkId(172));
		positiveList.add(Id.createLinkId(173));
		positiveList.add(Id.createLinkId(23625));
		positiveList.add(Id.createLinkId(23624));
		positiveList.add(Id.createLinkId(70241));
		positiveList.add(Id.createLinkId(70240));
		positiveList.add(Id.createLinkId(70239));
		positiveList.add(Id.createLinkId(70238));
		positiveList.add(Id.createLinkId(70236));
		positiveList.add(Id.createLinkId(70237));
		positiveList.add(Id.createLinkId(70235));
		positiveList.add(Id.createLinkId(70234));
		positiveList.add(Id.createLinkId(33938));
		positiveList.add(Id.createLinkId(33939));
		positiveList.add(Id.createLinkId(33928));
		positiveList.add(Id.createLinkId(33927));
		positiveList.add(Id.createLinkId(33926));
		positiveList.add(Id.createLinkId(33925));

	}

	/**
	 * @param l
	 * @return
	 */
	private boolean judgeLink(Link l) {
		if (!l.getAllowedModes().contains("car")) 
				return false;
		if (negativeList.contains(l.getId()))
			return false;
		if (positiveList.contains(l.getId()))
			return true;
		if (somePartOfLinkIsInBox(l))
		{
			if ((l.getCapacity()>minCapacity))
			{
				return true;
			} else if (l.getFreespeed()>minFreeSpeed){
				return true;
			}
		}
				return false;
	}

	/**
	 * @param l
	 * @return
	 */
	private boolean somePartOfLinkIsInBox(Link l) {
		if (coordIsInBox(l.getFromNode().getCoord())||coordIsInBox(l.getToNode().getCoord())||coordIsInBox(l.getCoord())) return true;
		else 
			return false;
	}
	/**
	 * @param c
	 * @return
	 */
	private boolean coordIsInBox(Coord c) {
		if ((c.getX()<=bottomRight.getX()&&c.getX()>=topLeft.getX())&&(c.getY()>=bottomRight.getY()&&c.getY()<=topLeft.getY()) )
			return true;
		
		else return false;
	}
}
