//package playground.christoph.evacuation.analysis;
//
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.text.NumberFormat;
//import java.util.List;
//import java.util.Map;
//
//import net.opengis.kml._2.DocumentType;
//import net.opengis.kml._2.FolderType;
//import net.opengis.kml._2.IconStyleType;
//import net.opengis.kml._2.LinkType;
//import net.opengis.kml._2.ObjectFactory;
//import net.opengis.kml._2.PlacemarkType;
//import net.opengis.kml._2.PointType;
//import net.opengis.kml._2.StyleType;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacility;
//import org.matsim.core.gbl.MatsimResource;
//import org.matsim.core.utils.collections.Tuple;
//import org.matsim.core.utils.geometry.CoordImpl;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.vis.kml.KMZWriter;
//import org.matsim.vis.kml.NetworkFeatureFactory;
//
//public class TripComparisonWriter {
//	
//	private static final Logger log = Logger.getLogger(TripComparisonWriter.class);
//
//	/*
//	 * Strings
//	 */
//	private static final String LINK = "Link: ";
//	private static final String ID = "Id: ";
//	private static final String WINNERSCOUNT = "Winners count: ";
//	private static final String DRAWSCOUNT = "Draws count: ";
//	private static final String LOSERSCOUNT = "Losers count: ";
//	private static final String WINNERSSUM = "Winners sum: ";
//	private static final String LOSERSSUM = "Losers sum: ";
//	private static final String CHANGEPERPERSON = "Per Person: ";
//
//	/*
//	 * Icons
//	 */
//	private static final String CROSSICON = "../../matsim/icons/plus.png";
//	private static final String MINUSICON = "../../matsim/icons/minus.png";
//
//	private static final String DEFAULTNODEICON ="node.png";
//	private static final String DEFAULTNODEICONRESOURCE = "icon18.png";
//	private static final Double ICONSCALE = Double.valueOf(0.5);
//	
//	private static final byte[] MATSIMRED = new byte[]{(byte) 255, (byte) 15, (byte) 15, (byte) 190};
//	private static final byte[] MATSIMGREEN = new byte[]{(byte) 255, (byte) 15, (byte) 190, (byte) 15};
//	private static final byte[] MATSIMWHITE = new byte[]{(byte) 230, (byte) 230, (byte) 230, (byte) 230};
//	
//	
//	private KMZWriter kmzWriter;
//	private DocumentType document;
//
//	private ObjectFactory kmlObjectFactory = new ObjectFactory();
//	
//	private StyleType winnerStyle;
//	private StyleType drawStyle;
//	private StyleType loserStyle;
//
//	private CoordinateTransformation coordTransform;
//	
//	public TripComparisonWriter(final CoordinateTransformation coordTransform, KMZWriter kmzWriter, DocumentType document) throws IOException {
//		this.coordTransform = coordTransform;
//		this.kmzWriter = kmzWriter;
//		this.document = document;
//		
//		createDefaultStyles();
//	}
//
//	public FolderType getLinkFolder(String title, Network network,
//			final Map<Id, List<Tuple<Id, Double>>> winners,
//			final Map<Id, List<Tuple<Id, Double>>> draws,
//			final Map<Id, List<Tuple<Id, Double>>> losers) throws IOException {
//		
//		FolderType mainFolder = this.kmlObjectFactory.createFolderType();
//		
//		FolderType countFolder = this.kmlObjectFactory.createFolderType();
//		FolderType winnersCountFolder = this.kmlObjectFactory.createFolderType();
//		FolderType drawsCountFolder = this.kmlObjectFactory.createFolderType();
//		FolderType losersCountFolder = this.kmlObjectFactory.createFolderType();
//
//		FolderType sumFolder = this.kmlObjectFactory.createFolderType();
//		FolderType winnersSumFolder = this.kmlObjectFactory.createFolderType();
//		FolderType drawsSumFolder = this.kmlObjectFactory.createFolderType();
//		FolderType losersSumFolder = this.kmlObjectFactory.createFolderType();
//
//		
//		mainFolder.setName("Links");
//		
//		countFolder.setName("Links Count");
//		winnersCountFolder.setName("Winners");
//		drawsCountFolder.setName("Draws");
//		losersCountFolder.setName("Losers");
//		
//		sumFolder.setName("Links Sum");
//		winnersSumFolder.setName("Winners");
//		drawsSumFolder.setName("Draws");
//		losersSumFolder.setName("Losers");
//		
//		countFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(winnersCountFolder));
//		countFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(drawsCountFolder));
//		countFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(losersCountFolder));
//		
//		sumFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(winnersSumFolder));
//		sumFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(drawsSumFolder));
//		sumFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(losersSumFolder));
//		
//		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(countFolder));
//		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(sumFolder));
//		
//		// for each Link
//		for (Link link : network.getLinks().values()) {
//			Id id = link.getId();
////			Coord coord = link.getCoord();
//			Coord transformedCoord = this.coordTransform.transform(calculatePlacemarkPosition(link));
//			
//			int winnerCount = winners.get(id).size();
//			int drawCount = draws.get(id).size();
//			int loserCount = losers.get(id).size();
//			
//			// skip Link, if no agents are present
//			if (winnerCount + drawCount + loserCount == 0) continue;
//			
//			PlacemarkType countPlacemark = createPlacemark(id, title, transformedCoord, winnerCount, drawCount, loserCount);		
//			if (winnerCount > drawCount && winnerCount > loserCount) {
//				countPlacemark.setStyleUrl(winnerStyle.getId());			
//				winnersCountFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(countPlacemark));
//			}
//			else if (loserCount > drawCount && loserCount > winnerCount) {
//				countPlacemark.setStyleUrl(loserStyle.getId());			
//				losersCountFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(countPlacemark));
//			}
//			else {
//				countPlacemark.setStyleUrl(drawStyle.getId());			
//				drawsCountFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(countPlacemark));
//			}
//
//
//			double winnerSum = 0.0;
//			double drawSum = 0.0;
//			double loserSum = 0.0;
//			for (Tuple<Id, Double> tuple : winners.get(id)) winnerSum = winnerSum + tuple.getSecond();
//			for (Tuple<Id, Double> tuple : draws.get(id)) drawSum = drawSum + tuple.getSecond();
//			for (Tuple<Id, Double> tuple : losers.get(id)) loserSum = loserSum + tuple.getSecond();
//			
////			winnerSum = Math.abs(winnerSum);
////			drawSum = Math.abs(drawSum);
////			loserSum = Math.abs(loserSum);
//			
////			// normalize the won/lost time
////			if (winnerCount > 0) winnerSum = winnerSum / winnerCount;
////			if (drawCount > 0) drawSum = drawSum / drawCount;
////			if (loserCount > 0) loserSum = loserSum / loserCount;
//			double totalChange = winnerSum + drawSum + loserSum;
//			double changePerPerson = totalChange / (winnerCount + drawCount + loserCount);
//			
//			PlacemarkType sumPlacemark = createPlacemark(id, title, transformedCoord, winnerSum, loserSum, changePerPerson);		
////			if (winnerSum > drawSum && winnerSum > loserSum) {
//			if (changePerPerson < 0.0) {
//				sumPlacemark.setStyleUrl(winnerStyle.getId());			
//				winnersSumFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(sumPlacemark));
//			}
////			else if (loserSum > drawSum && loserSum > winnerSum) {
//			else if (changePerPerson > 0.0) {
//				sumPlacemark.setStyleUrl(loserStyle.getId());			
//				losersSumFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(sumPlacemark));
//			}
//			else {
//				sumPlacemark.setStyleUrl(drawStyle.getId());			
//				drawsSumFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(sumPlacemark));
//			}
//		}		
//		return mainFolder;
//	}
//	
//	public FolderType getFacilityCountFolder(String title, ActivityFacilities facilities,
//			final Map<Id, List<Tuple<Id, Double>>> winners,
//			final Map<Id, List<Tuple<Id, Double>>> draws,
//			final Map<Id, List<Tuple<Id, Double>>> losers) throws IOException {
//		
//		FolderType mainFolder = this.kmlObjectFactory.createFolderType();
//		FolderType winnersFolder = this.kmlObjectFactory.createFolderType();
//		FolderType drawsFolder = this.kmlObjectFactory.createFolderType();
//		FolderType losersFolder = this.kmlObjectFactory.createFolderType();
//		
//		mainFolder.setName("Facilities");
//		winnersFolder.setName("Winners");
//		drawsFolder.setName("Draws");
//		losersFolder.setName("Losers");
//		
//		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(winnersFolder));
//		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(drawsFolder));
//		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(losersFolder));
//		
//		// for each Link
//		for (ActivityFacility facility : facilities.getFacilities().values()) {
//			Id id = facility.getId();
//			Coord coord = facility.getCoord();
//			Coord transformedCoord = this.coordTransform.transform(coord);
//			
//			int winnerCount = winners.get(id).size();
//			int drawCount = draws.get(id).size();
//			int loserCount = losers.get(id).size();
//			
//			// skip Link, if no agents are present
//			if (winnerCount + drawCount + loserCount == 0) continue;
//
//			PlacemarkType placemarkType = createPlacemark(id, title, transformedCoord, winnerCount, drawCount, loserCount);
//			
//			if (winnerCount > drawCount && winnerCount > loserCount) {
//				placemarkType.setStyleUrl(winnerStyle.getId());			
//				winnersFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemarkType));
//			}
//			else if (loserCount > drawCount && loserCount > winnerCount){
//				placemarkType.setStyleUrl(loserStyle.getId());			
//				losersFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemarkType));
//			}
//			else {
//				placemarkType.setStyleUrl(drawStyle.getId());			
//				drawsFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemarkType));
//			}
//		}		
//		return mainFolder;
//	}
//	
//	private PlacemarkType createPlacemark(Id id, String title, Coord coord, int winnerCount, int drawCount, int loserCount) {
//		StringBuffer stringBuffer = new StringBuffer();
//		PlacemarkType placemark = kmlObjectFactory.createPlacemarkType();
//		stringBuffer.delete(0, stringBuffer.length());
//		stringBuffer.append(LINK);
//		stringBuffer.append(id.toString());
//		placemark.setDescription(createPlacemarkDescription(id.toString(), title, winnerCount, drawCount, loserCount));
//		
//		PointType point;
//		point = kmlObjectFactory.createPointType();
//		point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
//		placemark.setAbstractGeometryGroup(kmlObjectFactory.createPoint(point));
//		return placemark;
//	}
//
//	private PlacemarkType createPlacemark(Id id, String title, Coord coord, double winnerSum, double loserSum, double changePerPerson) {
//		StringBuffer stringBuffer = new StringBuffer();
//		PlacemarkType placemark = kmlObjectFactory.createPlacemarkType();
//		stringBuffer.delete(0, stringBuffer.length());
//		stringBuffer.append(LINK);
//		stringBuffer.append(id.toString());
//		placemark.setDescription(createPlacemarkDescription(id.toString(), title, winnerSum, loserSum, changePerPerson));
//		
//		PointType point;
//		point = kmlObjectFactory.createPointType();
//		point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
//		placemark.setAbstractGeometryGroup(kmlObjectFactory.createPoint(point));
//		return placemark;
//	}
//	
//	/**
//	 * Calculates the position of a placemark in a way that it is 40 % of the link
//	 * length away from the node where the link starts.
//	 *
//	 * @param l
//	 * @return the CoordI instance
//	 */
//	private Coord calculatePlacemarkPosition(final Link l) {
//		Coord coordFrom = l.getFromNode().getCoord();
//		Coord coordTo = l.getToNode().getCoord();
//		double xDiff = coordTo.getX() - coordFrom.getX();
//		double yDiff = coordTo.getY() - coordFrom.getY();
//		double length = Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
//		double scale = 0.4;
//		scale = l.getLength() * scale;
//		Coord vec = new CoordImpl(coordFrom.getX() + (xDiff * scale/length), coordFrom.getY() + (yDiff * scale/length));
//		return vec;
//	}
//	
//	private String createPlacemarkDescription(String id, String title,  int winnerCount, int drawCount, int loserCount) {
//		StringBuffer buffer = new StringBuffer(100);
//		buffer.append(NetworkFeatureFactory.STARTH2);
//		buffer.append(title);
//		buffer.append(NetworkFeatureFactory.ENDH2);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(ID); 
//		buffer.append(id);
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(WINNERSCOUNT);
//		buffer.append(winnerCount);
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(DRAWSCOUNT);
//		buffer.append(drawCount);
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(LOSERSCOUNT);
//		buffer.append(loserCount);
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		return buffer.toString();
//	}
//	
//	private String createPlacemarkDescription(String id, String title,  double winnerSum, double loserSum, double changePerPerson) {
//		
//		NumberFormat formatter = new DecimalFormat("0.0");
//		
//		StringBuffer buffer = new StringBuffer(100);
//		buffer.append(NetworkFeatureFactory.STARTH2);
//		buffer.append(title);
//		buffer.append(NetworkFeatureFactory.ENDH2);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(ID); 
//		buffer.append(id);
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(WINNERSSUM);
//		buffer.append(formatter.format(winnerSum));
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(LOSERSSUM);
//		buffer.append(formatter.format(loserSum));
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		buffer.append(NetworkFeatureFactory.STARTH3);
//		buffer.append(CHANGEPERPERSON);
//		buffer.append(formatter.format(changePerPerson));
//		buffer.append(NetworkFeatureFactory.ENDH3);
//		
//		return buffer.toString();
//	}
//	
//	private void createDefaultStyles() throws IOException {
//
//		this.winnerStyle = kmlObjectFactory.createStyleType();
//		this.drawStyle = kmlObjectFactory.createStyleType();
//		this.loserStyle = kmlObjectFactory.createStyleType();
//		
//		this.winnerStyle.setId("winnerStyle");
//		this.drawStyle.setId("drawStyle");
//		this.loserStyle.setId("loserStyle");
//		
//		LinkType iconLink = kmlObjectFactory.createLinkType();
//		iconLink.setHref(DEFAULTNODEICON);
//		this.kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTNODEICON);
//		
//		IconStyleType iStyleWinner = kmlObjectFactory.createIconStyleType();
//		iStyleWinner.setIcon(iconLink);
//		iStyleWinner.setColor(TripComparisonWriter.MATSIMGREEN);
//		iStyleWinner.setScale(TripComparisonWriter.ICONSCALE);
//		this.winnerStyle.setIconStyle(iStyleWinner);
//		this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.winnerStyle));
//		
//		IconStyleType iStyleDraw = kmlObjectFactory.createIconStyleType();
//		iStyleDraw.setIcon(iconLink);
//		iStyleDraw.setColor(TripComparisonWriter.MATSIMWHITE);
//		iStyleDraw.setScale(TripComparisonWriter.ICONSCALE);
//		this.drawStyle.setIconStyle(iStyleDraw);
//		this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.drawStyle));
//
//		IconStyleType iStyleLoser = kmlObjectFactory.createIconStyleType();
//		iStyleLoser.setIcon(iconLink);
//		iStyleLoser.setColor(TripComparisonWriter.MATSIMRED);
//		iStyleLoser.setScale(TripComparisonWriter.ICONSCALE);
//		this.loserStyle.setIconStyle(iStyleLoser);
//		this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.loserStyle));
//
//	}
//}
