package playground.sergioo.NetworkVisualizer.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.sergioo.GTFS.GTFSDefinitions.RouteTypes;

public class PublicTransportNetworkPainter implements NetworkPainter {
	
	//Constants
	private static final String NUM_TIMES_LINK_ROUTE_FILE = "./data/NetworkCharacteristics/linkDrawInformation.txt";
	private static final String SEPARATOR = "~~~";
	private static final float MRT_FACTOR = 2000/80;
	private static final String OTHER_COLOR = "other";
	
	//Attributes
	private Color networkColor = Color.LIGHT_GRAY;
	private Stroke networkStroke = new BasicStroke(0.1f);
	private Map<RouteTypes, Map<String, Color>> colors;
	private NetworkByCamera networkByCamera;
	private TransitSchedule transitSchedule;
	private Map<Id, LinkDrawInformation> linksDrawInformation;
	private float weight = 0.001f;
	
	//Methods
	public PublicTransportNetworkPainter(NetworkByCamera networkByCamera, TransitSchedule transitSchedule) {
		this.networkByCamera =  networkByCamera;
		this.transitSchedule = transitSchedule;
		colors = new HashMap<RouteTypes, Map<String, Color>>();
		Map<String, Color> busColorsMap = new HashMap<String, Color>();
		busColorsMap.put("SBS", Color.BLUE);
		busColorsMap.put("SMRT", Color.BLACK);
		colors.put(RouteTypes.BUS, busColorsMap);
		Map<String, Color> mrtColorsMap = new TreeMap<String, Color>();
		mrtColorsMap.put("EW", Color.GREEN);
		mrtColorsMap.put("NS", Color.RED);
		mrtColorsMap.put("NE", Color.MAGENTA);
		mrtColorsMap.put("CC", Color.ORANGE);
		mrtColorsMap.put(OTHER_COLOR, Color.DARK_GRAY);
		colors.put(RouteTypes.SUBWAY, mrtColorsMap);
		Map<String, Color> tramColorsMap = new HashMap<String, Color>();
		tramColorsMap.put(OTHER_COLOR, Color.YELLOW);
		colors.put(RouteTypes.TRAM, tramColorsMap);
		Map<String, Color> railColorsMap = new HashMap<String, Color>();
		railColorsMap.put("other", Color.CYAN);
		colors.put(RouteTypes.RAIL, railColorsMap);
		linksDrawInformation = new HashMap<Id, LinkDrawInformation>();
		File numTimesLinkRouteFile = new File(NUM_TIMES_LINK_ROUTE_FILE);
		if(numTimesLinkRouteFile.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(numTimesLinkRouteFile));
				String line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(SEPARATOR);
					linksDrawInformation.put(new IdImpl(parts[0]), new LinkDrawInformation(Float.parseFloat(parts[1]), Color.decode(parts[2])));
					line = reader.readLine();
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			int i=0;
			for(Link linkT:networkByCamera.getNetwork().getLinks().values()) {
				Link link = linkT;
				for(RouteTypes routeType:RouteTypes.values())
					if(link.getAllowedModes().contains(routeType.name)) {
						float thickness = 0;
						Color color = null;
						for(TransitLine transitLine:transitSchedule.getTransitLines().values())
							for(TransitRoute transitRoute:transitLine.getRoutes().values())
								if(transitRoute.getTransportMode().trim().equals(routeType.name)) {
									if(routeType.equals(RouteTypes.BUS) && transitRoute.getRoute().getStartLinkId().equals(link.getId())) {
										thickness+=transitRoute.getDepartures().size();
										color = colors.get(routeType).get("SBS");//TODO
									}
									else if(transitRoute.getRoute().getEndLinkId().equals(link.getId())) {
										thickness+=transitRoute.getDepartures().size();
										if(routeType.equals(RouteTypes.BUS))
											color = colors.get(routeType).get("SBS");//TODO
										else {
											boolean firstColor = false;
											for(Entry<String, Color> lineE:colors.get(routeType).entrySet())
												if(transitRoute.getId().toString().contains(lineE.getKey())) {
													if(!firstColor)
														color = lineE.getValue();
												}
												else if(color!=null && color.equals(lineE.getValue()))
													firstColor = true;
											if(color==null)
												color = colors.get(routeType).get(OTHER_COLOR);
										}
									}
									else
										for(Id linkId:transitRoute.getRoute().getLinkIds())
											if(linkId.equals(link.getId())) {
												thickness+=transitRoute.getDepartures().size();
												if(routeType.equals(RouteTypes.BUS))
													color = colors.get(routeType).get("SBS");//TODO
												else {
													boolean firstColor = false;
													for(Entry<String, Color> lineE:colors.get(routeType).entrySet())
														if(transitRoute.getId().toString().contains(lineE.getKey())) {
															if(!firstColor)
																color = lineE.getValue();
														}
														else if(color!=null && color.equals(lineE.getValue()))
															firstColor = true;
													if(color==null)
														color = colors.get(routeType).get(OTHER_COLOR);
												}
											}
								}
						if(thickness>0) {
							if(routeType.equals(RouteTypes.SUBWAY)) {
								String[] parts = link.getId().toString().split("_");
								Id idOpposite = new IdImpl(parts[1]+"_"+parts[0]);
								LinkDrawInformation linkDrawInformationO = linksDrawInformation.get(idOpposite);
								if(linkDrawInformationO!=null)
									linkDrawInformationO.increaseThickness(thickness);
							}
							linksDrawInformation.put(link.getId(), new LinkDrawInformation(thickness*(routeType.equals(RouteTypes.BUS)?1:MRT_FACTOR),color));
						}
					}
				if(i%10000==0)
					System.out.println(i+" of "+networkByCamera.getNetwork().getLinks().size());
				i++;
			}
			try {
				PrintWriter writer = new PrintWriter(numTimesLinkRouteFile);
				for(Entry<Id,LinkDrawInformation> linkDrawInformationE:linksDrawInformation.entrySet())
					writer.println(linkDrawInformationE.getKey()+SEPARATOR+linkDrawInformationE.getValue().getThickness()+SEPARATOR+linkDrawInformationE.getValue().getColor().getRGB());
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public NetworkByCamera getNetworkByCamera() {
		return networkByCamera;
	}
	@Override
	public void paintNetwork(Graphics2D g2, Camera camera) throws Exception {
		networkByCamera.setCamera(camera);
		for(Link link:networkByCamera.getNetworkLinks()) {
			LinkDrawInformation linkDrawInformation = linksDrawInformation.get(link.getId());
			if(linkDrawInformation!=null)
				paintLink(g2, link, new BasicStroke(linkDrawInformation.getThickness()*weight), 2, linkDrawInformation.getColor());
			else
				paintLink(g2, link, networkStroke, 0.5, networkColor);
		}
	}
	public float getWeight() {
		return weight;
	}
	public void setWeight(float weight) {
		this.weight = weight;
	}
	private void paintLink(Graphics2D g2, Link link, Stroke stroke, double pointSize, Color color) throws Exception {
		paintLine(g2, new Tuple<Coord, Coord>(link.getFromNode().getCoord(), link.getToNode().getCoord()), stroke, color);
		//paintCircle(g2,link.getToNode().getCoord(), pointSize, color);
	}
	private void paintLine(Graphics2D g2, Tuple<Coord,Coord> coords, Stroke stroke, Color color) throws Exception {
		g2.setStroke(stroke);
		g2.setColor(color);
		g2.drawLine(networkByCamera.getIntX(coords.getFirst().getX()),
				networkByCamera.getIntY(coords.getFirst().getY()),
				networkByCamera.getIntX(coords.getSecond().getX()),
				networkByCamera.getIntY(coords.getSecond().getY()));
	}
	private void paintCircle(Graphics2D g2, Coord coord, double pointSize, Color color) throws Exception {
		Shape circle = new Ellipse2D.Double(networkByCamera.getIntX(coord.getX())-pointSize,networkByCamera.getIntY(coord.getY())-pointSize,pointSize*2,pointSize*2);
		g2.fill(circle);
	}
	
}
