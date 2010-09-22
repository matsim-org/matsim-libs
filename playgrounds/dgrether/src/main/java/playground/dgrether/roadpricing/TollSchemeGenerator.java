/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.roadpricing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.BoundaryType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinearRingType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.PolygonType;
import net.opengis.kml._2.ScreenOverlayType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

import playground.dgrether.analysis.gis.ShapeFileNetworkWriter;
import playground.dgrether.analysis.gis.ShapeFilePolygonWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * @author dgrether
 *
 */
public class TollSchemeGenerator {

	private static final Logger log = Logger.getLogger(TollSchemeGenerator.class);

	private static final String EQUILCONFIG = "../testData/examples/equil/config.xml";

	private static final String EQUILOUTFILE = "../testData/output/equil/moutArea.kmz";

	private static final Coord[] equilPolyCoords = { new CoordImpl(-10000, -10000),
			new CoordImpl(-10000, 10000), new CoordImpl(10000, 10000),
			new CoordImpl(10000, -10000) };

//	private static final String IVTCHCONF = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/baseCase/config.xml";

	private static final String IVTCHCONF = "../testData/schweiz-ivtch/ivtch-config.xml";

	private static final String SCHWEIZCHCONF = "../testData/schweiz-ch/ch-config.xml";

//	private static final String IVTOUTFILE = "../testData/output/ivtch/zurichCityAreaWithHighwaysPricingScheme.kmz";

//	private static final String IVTOUTSCHEME = "../testData/output/ivtch/zurichCityAreaWithHighwaysPricingScheme.xml";

	private static final String IVTOUTFILE = "../testData/output/ivtch/zurichCityAreaWithoutHighwaysPricingScheme.kmz";

	private static final String IVTOUTSCHEME = "../testData/output/ivtch/zurichCityAreaWithoutHighwaysPricingScheme.xml";

	private static final String IVTCHGISOUTBASE = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/baseCase/roadpricing/zurichCityArea/zurichCityAreaWithoudHighways";

	private static final String SCHWEIZCHOUTBASE = "/Volumes/data/work/cvsRep/vsp-cvs/studies/schweiz/roadpricing/zurichCityArea/zurichCityAreaWithoutHighwaysNew";

	private static final String SCHWEIZCHGISOUTBASE = "/Volumes/data/work/cvsRep/vsp-cvs/studies/schweiz/roadpricing/zurichCityArea/zurichCityAreaWithoutHighways";

	private static final String googleEarthPolyCoords = "8.425786799175953,47.40840613705993,0 8.421710028400241,47.39994280817881,0 8.417963147980927,47.39278943114768,0 8.415207918748933,47.38987365287824,0 8.415791890517795,47.38423209531803,0 8.41880861685496,47.37720026451881,0 8.421729416484297,47.37044196846617,0 8.425622484935019,47.36553079510605,0 8.423323740404612,47.35797751066716,0 8.428112748653305,47.34962290934973,0 8.45070089899518,47.34302244301254,0 8.458258697461332,47.34432245722293,0 8.4656718873172,47.35020668370471,0 8.471279932572031,47.35120072779423,0 8.51758408022306,47.34546749813342,0 8.522905646570109,47.35174736676126,0 8.521961169059978,47.36510115571412,0 8.522302525522861,47.36779482494401,0 8.534379467214848,47.36070541878087,0 8.565794165754525,47.34404660499774,0 8.583987479049085,47.34901770420281,0 8.591569601726832,47.40653036566425,0 8.572016057524166,47.41038517087896,0 8.570925906094473,47.42308052314462,0 8.534867061262872,47.43239384530932,0 8.473752933324278,47.42081611667773,0 8.450598247606663,47.41851870605534,0 8.425786799175953,47.40840613705993,0";

	private static final String googleEarthPolyCoordsZurichStadt = "8.44864283511221,47.37986637690101,0 8.451416777701553,47.37833007336098,0 8.459581508090139,47.37723627445201,0 " +
	"8.467433810761946,47.37338669769034,0 8.469762671688585,47.37357431212267,0 8.470953697207889,47.37270258811743,0 8.465353519866838,47.36880188166491,0 " +
	"8.464917809320042,47.36709036573689,0 8.469508778706881,47.36456328102342,0 8.470990177309787,47.36155771805489,0 8.476458153675964,47.3604108044819,0 8.479010577766175,47.36080479922836,0 " +
	"8.482353676617109,47.3593798994772,0 8.487673558834473,47.35337364668366,0 8.498810788990056,47.34477556667025,0 8.49767002585763,47.34246950802471,0 8.502525500368748,47.33738250658584,0 " +
	"8.502377264982343,47.33444855265198,0 8.501187727721771,47.33158503531836,0 8.501967712551462,47.32757415088069,0 8.502887170504579,47.32015441040062,0 8.508098951390465,47.32227272688915,0 " +
	"8.512519549792472,47.32287368433094,0 8.516952606171444,47.32326390223137,0 8.523850173549391,47.3246876366586,0 8.524542601130221,47.32631658767797,0 8.528866555174915,47.32711863692558,0 " +
	"8.531642268903695,47.32933138793216,0 8.539911576785542,47.33383325502518,0 8.56491191836173,47.34661823330764,0 8.570767565821313,47.34721671422761,0 8.586084695569738,47.35286011048823,0 " +
	"8.590951671648384,47.35290844343171,0 8.602442029920695,47.35123877990164,0 8.603681243050474,47.35338672299446,0 8.610347007753333,47.35363168235629,0 8.61243206562564,47.3543488357591,0 " +
	"8.616232722780893,47.35450505361912,0 8.62230262453221,47.35356438402733,0 8.624787912328614,47.35469489080281,0 8.621449272871928,47.35700402468927,0 8.622095385593028,47.35944905257028,0 " +
	"8.620122243233268,47.36065551850162,0 8.620034142814191,47.36151191590656,0 8.617900873725233,47.36154792201287,0 8.6162607649797,47.36114661051329,0 8.614875891385054,47.3613801748138,0 " +
	"8.615036462892562,47.36265895027167,0 8.614720432862725,47.36384431297679,0 8.616078154522413,47.36575341858956,0 8.616798434939156,47.36713753561843,0 8.613038001222804,47.36644249170886,0 " +
	"8.610421969054926,47.36680441531784,0 8.602248268711321,47.37110392493603,0 8.593898298412242,47.37970817160232,0 8.590003620421044,47.38397692219407,0 8.591652398857491,47.38492963774741,0 " +
	"8.591382835373381,47.38580031749137,0 8.587319931427686,47.38769032675616,0 8.584035070773723,47.3880787324257,0 8.583282316204034,47.39012193039267,0 8.587886748913832,47.39370766159691,0 " +
	"8.589352547071471,47.39455103533481,0 8.587974902038766,47.39566327796408,0 8.591555742152821,47.39702837649182,0 8.5941082137781,47.39635250037053,0 8.59667724649195,47.3967471937579,0 " +
	"8.596889342090577,47.39825882043707,0 8.594665565402096,47.39861654106586,0 8.595705180754635,47.40098145971395,0 8.593658857150487,47.40177136845836,0 8.596199269475477,47.40595466675183,0 " +
	"8.573525012874512,47.41105720420373,0 8.573633788296249,47.41938188009083,0 8.569765462489926,47.41835825478849,0 8.567448980365768,47.41590513735653,0 8.563741030685449,47.41508428093747,0 " +
	"8.560957135165538,47.41773462005145,0 8.554484619638771,47.4203115157041,0 8.557019032819682,47.42469881316054,0 8.555610221593483,47.43056385817484,0 8.543039938897536,47.43261424529186,0 " +
	"8.543642362249173,47.43143003910897,0 8.534371389552106,47.43114726187637,0 8.533451277049323,47.43232729087996,0 8.52579545010984,47.43266818965773,0 8.524780310071463,47.43097527811846,0 " +
	"8.518725279174831,47.43137519324898,0 8.516159015683698,47.43121350516078,0 8.514892442978976,47.43167206602394,0 8.512627266879477,47.43096395167997,0 8.502032529300919,47.43422473477193,0 " +
	"8.485945055257545,47.43055761912196,0 8.485873404192233,47.42851607869613,0 8.487437473021757,47.42685155459196,0 8.490763508041454,47.42605256575624,0 8.493276513468004,47.42438498700199,0 " +
	"8.490900771844565,47.42506803343011,0 8.490869875082694,47.42353581888205,0 8.490377991158923,47.42221365715753,0 8.488192153070813,47.42185339107501,0 8.485986350628323,47.42096607009258,0 " +
	"8.482332275332155,47.42245707249182,0 8.480741121611601,47.42081262307558,0 8.478076769637326,47.42169808868504,0 8.4777393672085,47.41895426545208,0 8.470508905272467,47.41704082143369,0 " +
	"8.469243529479201,47.41331064078145,0 8.471179547424271,47.41051201942035,0 8.470779078122973,47.41233796628759,0 8.473663427871756,47.41184572893032,0 8.47418904474813,47.41037491442653,0 " +
	"8.476016103467444,47.41084805979789,0 8.479476495620105,47.41006224171615,0 8.477596324756249,47.40797309004247,0 8.477831359762135,47.40637296178157,0 8.477529925213929,47.40382220868273,0 " +
	"8.468165419361014,47.40235183890383,0 8.467449566405591,47.39954117864379,0 8.472225671575913,47.39849628223908,0 8.469048051685377,47.3994093906715,0 8.46694908363736,47.39878889166638,0 " +
	"8.468355740492342,47.39746691123316,0 8.470883682949605,47.39656743520219,0 8.471627920840675,47.39394790751316,0 8.466070295798161,47.39433939496237,0 8.468607322537721,47.39135729003962,0 " +
	"8.467614108595754,47.39018181322238,0 8.464771613486562,47.39097562237137,0 8.44864283511221,47.37986637690101,0 ";

	private static final Coord centerCoord = new CoordImpl(0, 0);
	//6 o'clock
	private static final double START = 21600;
	//9 o'clock
	private static final double STOP = 21400;

	private static final double AMOUNT = 2;

	private static final String[] linkIdsToFilterArray = {"101222", "101221"};

	private Config config;

	private NetworkImpl network;

	private Coord[] usedCoords;

	// TODO change used data here:
	// private String usedConf = EQUILCONFIG;
//	private String usedConf = IVTCHCONF;
	private String usedConf = SCHWEIZCHCONF;

	// private String usedOut = EQUILOUTFILE;
//	private String usedOut = IVTOUTFILE;
	private String usedOut = SCHWEIZCHOUTBASE + ".kmz";

	private String usedSchemeOut = IVTOUTSCHEME;

	private String usedGoogleEarthCoords = googleEarthPolyCoordsZurichStadt;

//	private String usedGisOut = IVTCHGISOUTBASE;
	private String usedGisOut = SCHWEIZCHGISOUTBASE;

	private double usedStart = START;

	private double usedStop = STOP;

	private double usedAmount = AMOUNT;

	private String[] usedLinkIdsToFilterArray = linkIdsToFilterArray;


	public TollSchemeGenerator() {
		//read the config
		this.config = new Config();
		this.config.addCoreModules();
		Gbl.setConfig(this.config);
		MatsimConfigReader confReader = new MatsimConfigReader(this.config);
		confReader.readFile(this.usedConf);

		// TODO change used data here:
		// this.usedCoords = equilPolyCoords;
		this.usedCoords = parseGoogleEarthCoord(this.config);

		//prepare data
		List<Id> linkIdsToFilter = new ArrayList<Id>(this.usedLinkIdsToFilterArray.length);
		for (int i = 0; i < this.usedLinkIdsToFilterArray.length; i++) {
			linkIdsToFilter.add(new IdImpl(this.usedLinkIdsToFilterArray[i]));
		}


		//do something
		//filter the network with polygon
		NetworkImpl tollNetwork = this.createTollScheme(this.config);
		//filter the highway links explicitly
//		tollNetwork = this.applyLinkIdFilter(tollNetwork, linkIdsToFilter);
		//filter the highways by capacity
		this.applyCapacityFilter(tollNetwork, -1, 41000);


		//write kml
		writeKml(tollNetwork, this.usedOut);

		//create the road pricing scheme
		RoadPricingScheme pricingScheme = this.createRoadPricingScheme(tollNetwork);
		//write it to file
//		writeRoadPricingScheme(pricingScheme);

	  writeShapeFile(tollNetwork, this.usedCoords);

	}

	private void writeShapeFile(NetworkImpl network, Coord [] coords) {
		new ShapeFileNetworkWriter().writeNetwork(network, usedGisOut + "Network.shp");
		new ShapeFilePolygonWriter().writePolygon(coords, usedGisOut + "MoutArea.shp");
	}


	private Network applyCapacityFilter(Network network, double lowerBound, double upperBound) {
		Set<Link> linksToRemove = new HashSet<Link>();
		for (Link l : network.getLinks().values()) {
			if ((lowerBound > l.getCapacity()) || (upperBound < l.getCapacity())) {
				linksToRemove.add(l);
			}
		}
		for (Link l : linksToRemove) {
			network.removeLink(l.getId());
		}
		return network;
	}


	private Network applyLinkIdFilter(Network tollNetwork,
			List<Id> linkIdsToFilter) {
		for (Id i : linkIdsToFilter) {
			tollNetwork.removeLink(i);
		}
		return tollNetwork;
	}

	private NetworkImpl createTollScheme(Config config) {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadNetwork();
		this.network = loader.getScenario().getNetwork();

		NetworkImpl net = filterNetwork(this.network, false);
		log.info("Filtered the network, filtered network layer contains "
				+ net.getLinks().size() + " links.");
		return net;

	}

	private void writeRoadPricingScheme(RoadPricingScheme pricingScheme) {
		RoadPricingWriterXMLv1 pricingSchemeWriter = new RoadPricingWriterXMLv1(pricingScheme);
		try {
			pricingSchemeWriter.writeFile(this.usedSchemeOut);
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("RoadPricingScheme written to: "  + this.usedSchemeOut);
	}

	private RoadPricingScheme createRoadPricingScheme(NetworkImpl tollNetwork) {
		RoadPricingScheme scheme = new RoadPricingScheme();
		for (Link l : tollNetwork.getLinks().values()) {
			scheme.addLink(l.getId());
		}
		scheme.addCost(this.usedStart, this.usedStop, this.usedAmount);
		return scheme;
	}

	private Coord[] parseGoogleEarthCoord(Config config) {
		String[] coords3d = this.usedGoogleEarthCoords.split(" ");
		String[] singleCoords;
		double x, y;
		CoordinateTransformation transform = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84, config
						.global().getCoordinateSystem());
		Coord c, coord;
		Coord[] ret = new Coord[coords3d.length];
		int i = 0;
		for (String s : coords3d) {
			singleCoords = s.split(",");
			x = Double.parseDouble(singleCoords[0]);
			y = Double.parseDouble(singleCoords[1]);
			c = new CoordImpl(x, y);
//			log.debug("read coordinate with x: " + x + " y: " + y);
			coord = transform.transform(c);
//			log.debug("transformed coordinate with x: " + coord.getX() + " y: "
//					+ coord.getY());
			ret[i] = coord;
			i++;
		}

		return ret;
	}



	private void writeKml(NetworkImpl net, String filename) {

		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KmlType mainKml;
		DocumentType mainDoc;
		FolderType mainFolder;

		KMZWriter writer;

		mainKml = kmlObjectFactory.createKmlType();
		mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		// create a folder
		mainFolder = kmlObjectFactory.createFolderType();
		mainFolder.setName("Matsim Data");
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(mainFolder));
		// the writer
		writer = new KMZWriter(filename);
		try {
			// add the matsim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(logo));
			//create coordinate transformation for wgs84
			CoordinateTransformation transform = TransformationFactory
					.getCoordinateTransformation(this.config.global()
							.getCoordinateSystem(), TransformationFactory.WGS84);
			//write the network
			KmlNetworkWriter netWriter = new KmlNetworkWriter(net, transform, writer,
					mainDoc);
			FolderType networkFolder = netWriter.getNetworkFolder();
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(networkFolder));

			// write moute polygon
			FolderType polygonFolder = kmlObjectFactory.createFolderType();
			polygonFolder.setName("Mout area");

			PolygonType p = kmlObjectFactory.createPolygonType();
			p.setTessellate(true);

			LinearRingType ring = kmlObjectFactory.createLinearRingType();
			Coord transC;
			for (Coord c : this.usedCoords) {
				transC = transform.transform(c);
				PointType point = kmlObjectFactory.createPointType();
				point.getCoordinates().add(Double.toString(transC.getX()) + "," + Double.toString(transC.getY()) + ",0.0");
			}
			BoundaryType boundary = kmlObjectFactory.createBoundaryType();
			boundary.setLinearRing(ring);
			p.setOuterBoundaryIs(boundary);
			PlacemarkType polyPlace = kmlObjectFactory.createPlacemarkType();
			polyPlace.setName("Mout area polygon");
			polyPlace.setAbstractGeometryGroup(kmlObjectFactory.createPolygon(p));
			polygonFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(polyPlace));
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(polygonFolder));
			// polygonFolder.addFeature(feature)

		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo cause: " + e.getMessage());
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
		log.info("Network written to kmz!");
	}

	private NetworkImpl filterNetwork(final NetworkImpl net, boolean full) {
		NetworkImpl n = NetworkImpl.createNetwork();
		GeometryFactory geofac = new GeometryFactory();
		Coordinate[] geoToolCoords = new Coordinate[this.usedCoords.length];
		int i = 0;
		for (Coord c : this.usedCoords) {
			geoToolCoords[i] = new Coordinate(c.getX(), c.getY());
			i++;
		}
		CoordinateSequence coordsequence = new CoordinateArraySequence(
				geoToolCoords);
		LinearRing shell = new LinearRing(coordsequence, geofac);
		Polygon ppp = new Polygon(shell, new LinearRing[] {}, geofac);

		for (Link l : net.getLinks().values()) {
			Coordinate fromCord = MGC.coord2Coordinate(l.getFromNode().getCoord());
			Coordinate toCord = MGC.coord2Coordinate(l.getToNode().getCoord());

			if (ppp.contains(new Point(new CoordinateArraySequence(
					new Coordinate[] { fromCord }), geofac))
					&& ppp.contains(new Point(new CoordinateArraySequence(
							new Coordinate[] { toCord }), geofac))) {
				n.getLinks().put(l.getId(), l);
			}
		}
		return n;
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new TollSchemeGenerator();
	}

}
