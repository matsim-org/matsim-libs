package playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.publicTransport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.sergioo.gtfs2PTSchedule2011.GTFSDefinitions.RouteTypes;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.networkVisualizer.networkPainters.NetworkPainter;
import playground.sergioo.visualizer2D2012.networkVisualizer.publicTransportCapacity.LinkDrawInformation;

public class PublicTransportNetworkPainter extends NetworkPainter {
	
	//Constants
	private static final String NUM_TIMES_LINK_ROUTE_FILE = "./data/NetworkCharacteristics/linkDrawInformation.txt";
	private static final String SEPARATOR = "~~~";
	private static final float MRT_FACTOR = 1920/132;
	private static final float MRT_FACTOR_2 = 930/132;
	private static final String OTHER_COLOR = "other";
	
	//Attributes
	private Color networkColor = Color.DARK_GRAY;
	private Stroke networkStroke = new BasicStroke(0.1f);
	private Map<RouteTypes, Map<String, Color>> colors;
	private Map<Id<Link>, LinkDrawInformation> linksDrawInformation;
	private float weight = 0.001f;
	
	//Methods
	public PublicTransportNetworkPainter(Network network, TransitSchedule transitSchedule) {
		super(network);
		initialize(transitSchedule);
	}
	public void initialize(TransitSchedule transitSchedule) {
		colors = new HashMap<RouteTypes, Map<String, Color>>();
		Map<String, Color> busColorsMap = new HashMap<String, Color>();
		busColorsMap.put("SBS", new Color(100,50,0));
		busColorsMap.put("SMRT", Color.BLACK);
		colors.put(RouteTypes.BUS, busColorsMap);
		Map<String, Color> mrtColorsMap = new TreeMap<String, Color>();
		mrtColorsMap.put("EW",  new Color(5,157,77));
		mrtColorsMap.put("NS", new Color(217,29,7));
		mrtColorsMap.put("NE", new Color(145,17,162));
		mrtColorsMap.put("CC", new Color(250,155,16));
		mrtColorsMap.put(OTHER_COLOR, Color.LIGHT_GRAY);
		colors.put(RouteTypes.SUBWAY, mrtColorsMap);
		Map<String, Color> tramColorsMap = new HashMap<String, Color>();
		tramColorsMap.put(OTHER_COLOR, Color.YELLOW);
		colors.put(RouteTypes.TRAM, tramColorsMap);
		Map<String, Color> railColorsMap = new HashMap<String, Color>();
		railColorsMap.put("other", Color.CYAN);
		colors.put(RouteTypes.RAIL, railColorsMap);
		linksDrawInformation = new HashMap<Id<Link>, LinkDrawInformation>();
		File numTimesLinkRouteFile = new File(NUM_TIMES_LINK_ROUTE_FILE);
		if(numTimesLinkRouteFile.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(numTimesLinkRouteFile));
				String line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split(SEPARATOR);
					linksDrawInformation.put(Id.createLinkId(parts[0]), new LinkDrawInformation(Float.parseFloat(parts[1]), Color.decode(parts[2])));
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
			for(Link linkT:networkPainterManager.getNetwork().getLinks().values()) {
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
										for(Id<Link> linkId:transitRoute.getRoute().getLinkIds())
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
								Id<Link> idOpposite = Id.createLinkId(parts[1]+"_"+parts[0]);
								LinkDrawInformation linkDrawInformationO = linksDrawInformation.get(idOpposite);
								if(linkDrawInformationO!=null)
									linkDrawInformationO.increaseThickness(thickness);
							}
							linksDrawInformation.put(link.getId(), new LinkDrawInformation(thickness*(routeType.equals(RouteTypes.BUS)?1:color.equals(colors.get(RouteTypes.SUBWAY).get("CC"))?MRT_FACTOR_2:MRT_FACTOR),color));
						}
					}
				if(i%10000==0)
					System.out.println(i+" of "+networkPainterManager.getNetwork().getLinks().size());
				i++;
			}
			try {
				PrintWriter writer = new PrintWriter(numTimesLinkRouteFile);
				for(Entry<Id<Link>, LinkDrawInformation> linkDrawInformationE:linksDrawInformation.entrySet())
					writer.println(linkDrawInformationE.getKey()+SEPARATOR+linkDrawInformationE.getValue().getThickness()+SEPARATOR+linkDrawInformationE.getValue().getColor().getRGB());
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		try {
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera())) {
				LinkDrawInformation linkDrawInformation = linksDrawInformation.get(link.getId());
				if(linkDrawInformation==null)
					paintLink(g2, layersPanel, link, networkStroke, 0.5, networkColor);
			}
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera())) {
				LinkDrawInformation linkDrawInformation = linksDrawInformation.get(link.getId());
				if(linkDrawInformation!=null && linkDrawInformation.getColor().equals(new Color(100,50,0)))
					paintLink(g2, layersPanel, link, new BasicStroke(linkDrawInformation.getThickness()*weight), 2, linkDrawInformation.getColor());
			}
			for(Link link:networkPainterManager.getNetworkLinks(layersPanel.getCamera())) {
				LinkDrawInformation linkDrawInformation = linksDrawInformation.get(link.getId());
				if(linkDrawInformation!=null && !linkDrawInformation.getColor().equals(new Color(100,50,0)))
					paintLink(g2, layersPanel, link, new BasicStroke(linkDrawInformation.getThickness()*weight), 2, linkDrawInformation.getColor());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public float getWeight() {
		return weight;
	}
	public void setWeight(float weight) {
		this.weight = weight;
	}
	protected void paintLink(Graphics2D g2, LayersPanel layersPanel, Link link, Stroke stroke, double pointSize, Color color) {
		double[] from = new double[]{link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()};
		double[] to = new double[]{link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()};
		paintLine(g2, layersPanel, from, to, stroke, color);
		//paintCircle(g2,link.getToNode().getCoord(), pointSize, color);
	}
	
}
