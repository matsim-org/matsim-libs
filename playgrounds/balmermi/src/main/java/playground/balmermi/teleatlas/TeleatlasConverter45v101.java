/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.balmermi.teleatlas;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.misc.Counter;

import playground.balmermi.modules.NetworkDoubleLinks;
import playground.balmermi.teleatlas.NwElement.FormOfWay;
import playground.balmermi.teleatlas.NwElement.NwFeatureType;
import playground.balmermi.teleatlas.NwElement.OneWay;
import playground.balmermi.teleatlas.NwElement.PrivateRoad;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Converts the data from a Tele Atlas network according to the specifications detailed in
 * <em>Tele Atlas MultiNet Shapefile 4.5 Format Specifications Version Final v1.0.1, June 2009</em>
 * into a MATSim compatible network.
 *
 * From Tele Atlas, only the Network (nw) and Junction (jc) data was used.
 * The following rules were applied to convert the attributes from the Tele Atlas network
 * to attributes in the MATSim network:
 *
 * <dl>
 * <dt>Ignored network elemens</dt>
 * <dd>Network elements meeting at least one of the following criteria are ignored:
 *     <ul>
 *       <li>NW.FEATTYP = 4165 (Address Area Boundary)</li>
 *       <li>NW.FOW = 6 (Parking Place)</li>
 *       <li>NW.FOW = 7 (Parking Garage)</li>
 *       <li>NW.FOW = 12 (Entrance/Exit to/from car park)</li>
 *       <li>NW.FOW = 20 (Authorities)</li>
 *       <li>NW.PRIVATERD = 2 (Private Road with Generic Restriction)</li>
 *     </ul>
 * </dd>
 * <dt>capacity</dt>
 * <dd>NW.NET2CLASS<br />
 *     Classes 0, 1, 2: capacity = 2000 veh/hour per lane<br />
 *     Class 3: capacity = 1000 veh/hour per lane<br />
 *     Classes 4-6: 600 veh/hour per lane
 * </dd>
 * <dt>freespeed</dt>
 * <dd>NW.SPEEDCAT<br />
 *     Category 1: 110 km/h<br />
 *     Category 2: 100 km/h<br />
 *     Category 3: 90 km/h<br />
 *     Category 4: 70 km/h<br />
 *     Category 5: 50 km/h<br />
 *     Category 6: 30 km/h<br />
 *     Category 7: 10 km/h<br />
 *     Category 8: 5 km/h<br />
 * </dd>
 * <dt>number of lanes</dt>
 * <dd>If NW.LANES > 0, use that value.
 *     If NW.LANES = 0, then assume number-of-lanes = 1, except for the following cases:
 *     <ul>
 *     <li>NW.FRC = 0 (Motoryway, Freeway, Other Major Road)</li>
 *     <li>NW.FRC = 1 (Major Road Less Important than Motorway)</li>
 *     <li>NW.FOW = 1 (Motorway)</li>
 *     <li>NW.FOW = 2 (Multi-Carriageway)</li>
 *     <li>NW.NET2CLASS = 0</li>
 *     </ul>
 *     In any of the above cases, number-of-lanes is assumed to be 2.
 * </dd>
 * <dt>modes</dt>
 * <dd>By default, {@link TransportMode#car}, {@link TransportMode#bike}, {@link TransportMode#walk}
 *     are set as the default allowed modes.<br />
 *     If the road is a Freeway or Motorway (NW.FREEWAY = 1 or NW.FRC = 0 or NW.FRC = 1), walk and bike are removed, leaving only car available.
 *     If the road is part of a pedestrian zone (NW.FOW = 14 or NW.FOW = 15), car is removed, leaving walk and bike only.
 *     If the road is a one-way road (NW.ONEWAY = FT or NW.ONEWAY = TF or NW.ONEWAY = N), car is removed, leaving walk and bike only.
 *     If the road is a slip-road (NW.FOW = 10) where car is not allowed (e.g. due to one-way restriction on such roads), walk and bike are removed as well, such that the link is not converted at all.
 * </dd>
 * <dt>Road direction</dt>
 * <dd>By default, each Network Element with Feature type 4110 and 4130 (NW.FEATTYP) is
 *     converted into two links in MATSim, one leading from F_JNCTID to T_JNCTID, and the
 *     other one leading in the opposite direction (from T_JNCTID to F_JNCTID). Depending
 *     on the value of NW.ONEWAY, only links for one direction may be created.
 * </dd>
 * <dt>length</dt>
 * <dd>By default, the value of NW.METERS is used. If that value is shorter than the
 *     direct distance between the from- and to-Junction, than the length of the direct
 *     distance is taken.
 * </dd>
 * </dl>
 *
 * @author mrieser
 * @author balmermi
 */
public class TeleatlasConverter45v101 {

	private final static Logger log = Logger.getLogger(TeleatlasConverter45v101.class);

	private final Scenario scenario;
	private final Network network;
	private final Counter nodeCounter = new Counter("Node #");
	private final Counter linkCounter = new Counter("Link #");
	private CoordinateTransformation coordTransform = new IdentityTransformation();

	private final TeleatlasData data;

	public TeleatlasConverter45v101(final Scenario scenario, final TeleatlasData data) {
		this.scenario = scenario;
		this.network = scenario.getNetwork();
		this.data = data;
	}

	/**
	 * Sets the Coordinate Transformation to be applied when converting data. By default,
	 * the data from the Tele Atlas network is in WGS84.
	 *
	 * @param transformation if <code>null</code>, an {@link IdentityTransformation} is used
	 */
	public void setCoordinateTransformation(final CoordinateTransformation transformation) {
		if (transformation == null) {
			this.coordTransform = new IdentityTransformation();
		} else {
			this.coordTransform = transformation;
		}
	}

	public void convertToNetwork() {
		for (NwElement nwElement : this.data.networkElements.values()) {
			if (NwFeatureType.ADDRESS_AREA_BOUNDARY.equals(nwElement.featType) ||
					FormOfWay.ETA_PARKING_PLACE.equals(nwElement.fow) ||
					FormOfWay.ETA_PARKING_GARAGE.equals(nwElement.fow) ||
					FormOfWay.ENTRANCE_EXIT_CARPARK.equals(nwElement.fow) ||
					FormOfWay.AUTHORITIES.equals(nwElement.fow) ||
					PrivateRoad.SPECIAL_RESTRICTION.equals(nwElement.privat)) {
				continue;
			}
			boolean createFTElement = true;
			boolean createTFElement = true;

			double length = nwElement.length.doubleValue();

			Set<TransportMode> modesFT = getModesFT(nwElement);
			Set<TransportMode> modesTF = getModesTF(nwElement);

			double freespeed = getFreespeed(nwElement);
			double nOfLanes = getNOfLanes(nwElement);
			double capacity = getFlowCapacity(nwElement);
			String type = getType(nwElement);

			// find out if we need to create the links
			if (modesFT.isEmpty()) {
				createFTElement = false;
			}
			if (modesTF.isEmpty()) {
				createTFElement = false;
			}

			// create the links
			if (createFTElement) {
				Link link = createLink(nwElement, "FT");
				setLinkLength(link, length);
				link.setCapacity(capacity);
				link.setFreespeed(freespeed);
				link.setNumberOfLanes(nOfLanes);
				link.setAllowedModes(modesFT);
				if (modesFT.size() == 3) { ((LinkImpl)link).setType(type); }
				else if (type.startsWith("-")) { ((LinkImpl)link).setType(type); }
				else { ((LinkImpl)link).setType("-"+type); }
			}
			if (createTFElement) {
				Link link = createLink(nwElement, "TF");
				setLinkLength(link, length);
				link.setCapacity(capacity);
				link.setFreespeed(freespeed);
				link.setNumberOfLanes(nOfLanes);
				link.setAllowedModes(modesTF);
				((LinkImpl)link).setType(type);
				if (modesTF.size() == 3) { ((LinkImpl)link).setType(type); }
				else if (type.startsWith("-")) { ((LinkImpl)link).setType(type); }
				else { ((LinkImpl)link).setType("-"+type); }
			}
		}
	}

	private Set<TransportMode> getModesFT(final NwElement nwElement) {
		Set<TransportMode> modes = EnumSet.of(TransportMode.car, TransportMode.walk, TransportMode.bike);

		if ((OneWay.CLOSED.equals(nwElement.oneway) || OneWay.OPEN_TF.equals(nwElement.oneway))) {
			modes.remove(TransportMode.car);
		}

		if (NwElement.Freeway.FREEWAY.equals(nwElement.freeway)) {
			modes.remove(TransportMode.walk);
			modes.remove(TransportMode.bike);
		}

		if (NwElement.FunctionalRoadClass.MOTORWAY.equals(nwElement.frc) || NwElement.FunctionalRoadClass.MAJOR_ROAD_HIGH.equals(nwElement.frc)) {
			modes.remove(TransportMode.walk);
			modes.remove(TransportMode.bike);
		}

		if (NwElement.FormOfWay.PEDESTRIAN_ZONE.equals(nwElement.fow) || NwElement.FormOfWay.WALKWAY.equals(nwElement.fow)) {
			modes.remove(TransportMode.car);
		}

		if (NwElement.FormOfWay.SLIP_ROAD.equals(nwElement.fow) && !modes.contains(TransportMode.car)) {
			modes.remove(TransportMode.walk);
			modes.remove(TransportMode.bike);
		}

		return modes;
	}

	private Set<TransportMode> getModesTF(final NwElement nwElement) {
		Set<TransportMode> modes = EnumSet.of(TransportMode.car, TransportMode.walk, TransportMode.bike);

		if ((OneWay.CLOSED.equals(nwElement.oneway) || OneWay.OPEN_FT.equals(nwElement.oneway))) {
			modes.remove(TransportMode.car);
		}

		if (NwElement.Freeway.FREEWAY.equals(nwElement.freeway)) {
			modes.remove(TransportMode.walk);
			modes.remove(TransportMode.bike);
		}

		if (NwElement.FunctionalRoadClass.MOTORWAY.equals(nwElement.frc) || NwElement.FunctionalRoadClass.MAJOR_ROAD_HIGH.equals(nwElement.frc)) {
			modes.remove(TransportMode.walk);
			modes.remove(TransportMode.bike);
		}

		if (NwElement.FormOfWay.PEDESTRIAN_ZONE.equals(nwElement.fow) || NwElement.FormOfWay.WALKWAY.equals(nwElement.fow)) {
			modes.remove(TransportMode.car);
		}

		if (NwElement.FormOfWay.SLIP_ROAD.equals(nwElement.fow) && !modes.contains(TransportMode.car)) {
			modes.remove(TransportMode.walk);
			modes.remove(TransportMode.bike);
		}

		return modes;
	}

	private double getFreespeed(final NwElement nwElement) {
		switch (nwElement.speedCat) {
			case LT11:
				return 5.0 / 3.6;
			case R11_30:
				return 10.0 / 3.6;
			case R31_50:
				return 30.0 / 3.6;
			case R51_70:
				return 50.0 / 3.6;
			case R71_90:
				return 70.0 / 3.6;
			case R91_100:
				return 90.0 / 3.6;
			case R101_130:
				return 100.0 / 3.6;
			case GT130:
				return 110.0 / 3.6;
		}
		throw new IllegalArgumentException("It seems not all possible cases were covered.");
	}

	private int getNOfLanes(final NwElement nwElement) {
		int nOfLanes = nwElement.nOfLanes.intValue();
//		nOfLanes = (int)Math.ceil(nOfLanes/2.0);

		if (nOfLanes == 0) {
			nOfLanes = 1;
			if (NwElement.FunctionalRoadClass.MOTORWAY.equals(nwElement.frc) ||
					NwElement.FunctionalRoadClass.MAJOR_ROAD_HIGH.equals(nwElement.frc) ||
					NwElement.FormOfWay.MOTORWAY.equals(nwElement.fow) ||
					NwElement.FormOfWay.MULTI_CARRIAGEWAY.equals(nwElement.fow) ||
					NwElement.NetTwoClass.MOTORWAY.equals(nwElement.net2Class)) {
				nOfLanes = 2;
			}
		}
		return nOfLanes;
	}

	private double getFlowCapacity(final NwElement nwElement) {
		int nOfLanes = getNOfLanes(nwElement);

		switch (nwElement.net2Class) {
		case MOTORWAY:
		case MAIN_AXIS:
		case CONNECTION_AXIS_HIGH:
			return 2000.0 * nOfLanes;
		case CONNECTION_AXIS_LOW:
			return 1000.0 * nOfLanes;
		case COUNTRYSIDE_LOCAL_ROAD_LOW:
		case PARKING_ACCESS_ROAD:
		case RESTRICTED_PEDESTRIAN:
			return 600.0 * nOfLanes;
		default:
			throw new IllegalArgumentException("nwId="+nwElement.id+": net2Class="+nwElement.net2Class+" not allowed.");
		}
	}

	private String getType(final NwElement nwElement) {
		if (NwElement.NwFeatureType.ROAD_ELEMENT.equals(nwElement.featType)) {
			switch (nwElement.frc) {
			case MOTORWAY:
				return "0";
			case MAJOR_ROAD_HIGH:
				return "1";
			case MAJOR_ROAD_LOW:
				return "2";
			case SECONDARY_ROAD:
				return "3";
			case LOCAL_CONNECTING_ROAD:
				return "4";
			case LOCAL_ROAD_HIGH:
				return "5";
			case LOCAL_ROAD_MEDIUM:
				return "6";
			case LOCAL_ROAD_LOW:
				return "7";
			case OTHER_ROAD:
				return "8";
			case UNDEFINED:
				return "-1";
			default:
				throw new IllegalArgumentException("nwId="+nwElement.id+": frc="+nwElement.frc+" not allowed.");
			}
		}
		return "-2";
	}

	private Link createLink(NwElement nwElement, String direction) {
		JcElement fromJunction;
		JcElement toJunction;
		if ("FT".equals(direction)) {
			fromJunction = nwElement.fromJunction;
			toJunction = nwElement.toJunction;
		} else if ("TF".equals(direction)) {
			fromJunction = nwElement.toJunction;
			toJunction = nwElement.fromJunction;
		} else {
			throw new IllegalArgumentException("direction must be FT or TF.");
		}
		Id fromId = this.scenario.createId(Long.toString(fromJunction.id));
		Id toId = this.scenario.createId(Long.toString(toJunction.id));
		Node fromNode = this.network.getNodes().get(fromId);
		if (fromNode == null) {
			fromNode = this.network.getFactory().createNode(fromId, convertCoordinate(fromJunction.c));
			this.nodeCounter.incCounter();
			this.network.addNode(fromNode);
		}
		Node toNode = this.network.getNodes().get(toId);
		if (toNode == null) {
			toNode = this.network.getFactory().createNode(toId, convertCoordinate(toJunction.c));
			this.nodeCounter.incCounter();
			this.network.addNode(toNode);
		}
		Id linkId = this.scenario.createId(Long.toString(nwElement.id) + direction);
		Link link = this.network.getFactory().createLink(linkId, fromId, toId);
		this.linkCounter.incCounter();
		this.network.addLink(link);
		return link;
	}

	private void setLinkLength(final Link link, final double length) {
		double minLength = CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
		if (minLength > length) {
			link.setLength(minLength);
		} else {
			link.setLength(length);
		}
	}

	private Coord convertCoordinate(Coordinate c) {
		return this.coordTransform.transform(this.scenario.createCoord(c.x, c.y));
	}

	public static void main(String[] args) throws IOException, RuntimeException {
		if (args.length != 2) { log.info("TeleatlasConverter45v101 indir outdir"); System.exit(1); }
		Gbl.printSystemInfo();

		String indir = args[0];
		String outdir = args[1];

		log.info("reading teleatlas data...");
		TeleatlasData data = new TeleatlasData();
		NetworkReaderTeleatlas45v101 reader = new NetworkReaderTeleatlas45v101(data);
		reader.parseJc(indir+"/jc.shp");
		reader.parseNw(indir+"/nw.shp");
		log.info("Got information about #" + data.junctionElements.size() + " junctions.");
		log.info("Got information about #" + data.networkElements.size() + " network elements.");
		Gbl.printMemoryUsage();

		log.info("start conversion...");
		Scenario scenario = new ScenarioImpl();
		TeleatlasConverter45v101 converter = new TeleatlasConverter45v101(scenario, data);
		converter.setCoordinateTransformation(new WGS84toCH1903LV03());
		converter.convertToNetwork();
		log.info("# nodes created: " + scenario.getNetwork().getNodes().size());
		log.info("# links created: " + scenario.getNetwork().getLinks().size());

		log.info("write network...");
		File outputDir = new File(outdir+"/base");
		outputDir.mkdir();
		data = null;
		reader = null;
		new NetworkWriter(scenario.getNetwork()).writeFile(outputDir.toString()+"/network.xml");
		new NetworkWriteAsTable(outputDir.toString()).run((NetworkLayer) scenario.getNetwork());

		log.info("clean network...");
		new NetworkCleaner().run(scenario.getNetwork());

		log.info("write cleaned network...");
		outputDir = new File(outdir+"/cleaned");
		outputDir.mkdir();
		new NetworkWriter(scenario.getNetwork()).writeFile(outputDir.toString()+"/network.xml");
		new NetworkWriteAsTable(outputDir.toString()).run((NetworkLayer) scenario.getNetwork());

		log.info("fix double links...");
		new NetworkDoubleLinks("d").run((NetworkLayer)scenario.getNetwork());

		log.info("write final version of network...");
		outputDir = new File(outdir+"/final");
		outputDir.mkdir();
		new NetworkWriter(scenario.getNetwork()).writeFile(outputDir.toString()+"/network.xml");
		new NetworkWriteAsTable(outputDir.toString()).run((NetworkLayer) scenario.getNetwork());

		log.info("done.");

//		scenario = null;
//		org.matsim.run.OTFVis.main(new String[]{"/data/tmp/che_network.cleaned.xml"});
	}
}
