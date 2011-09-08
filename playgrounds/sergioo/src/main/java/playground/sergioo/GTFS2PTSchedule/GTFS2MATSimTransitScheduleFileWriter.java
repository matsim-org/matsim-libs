package playground.sergioo.GTFS2PTSchedule;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.GTFS2PTSchedule.auxiliar.LinkStops;
import playground.sergioo.GTFS2PTSchedule.PathEditor.gui.Window;
import playground.sergioo.GTFS2PTSchedule.PathEditor.kernel.RoutesPathsGenerator;
import playground.sergioo.GTFS2PTSchedule.GTFSDefinitions.RouteTypes;
import util.geometry.Line2D;
import util.geometry.Point2D;

public class GTFS2MATSimTransitScheduleFileWriter {
	
	//Constants
	/**
	 * Maximum distance allowed between an stop and the end of the corresponding link
	 */
	private static final double MAX_DISTANCE_STOP_LINK = 50*180/(6371000*Math.PI);
	private static final double DEFAULT_FREE_SPEED = 20;
	private static final double DEFAULT_CAPCITY = 500;
	
	//Attributes
	/**
	 * The folder root of the GTFS files
	 */
	private File[] roots;
	/**
	 * The network where the public transport will be performed
	 */
	private Network network;
	/**
	 * The types of dates that will be represented by the new file
	 */
	private String[] serviceIds;
	/**
	 * The stops
	 */
	private Map<String, Stop>[] stops;
	/**
	 * The calendar sevices
	 */
	private Map<String, Service>[] services;
	/**
	 * The shapes
	 */
	private Map<String, Shape>[] shapes;
	/**
	 * The routes
	 */
	private SortedMap<String, Route>[] routes;
	/**
	 * The time format 
	 */
	private SimpleDateFormat timeFormat;
	
	//Methods
	/**
	 * @param root
	 * @param network
	 */
	public GTFS2MATSimTransitScheduleFileWriter(File[] roots, Network network, String[] serviceIds) {
		super();
		this.roots = roots;
		this.network = network;
		updateNetwork();
		this.serviceIds = serviceIds;
	}
	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return network;
	}
	private void updateNetwork() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(RoutesPathsGenerator.NEW_NETWORK_NODES_FILE));
			String line = reader.readLine();
			while(line!=null) {
				Id id = new IdImpl(line);
				network.addNode(network.getFactory().createNode(id, new CoordImpl(Double.parseDouble(reader.readLine()), Double.parseDouble(reader.readLine()))));
				line = reader.readLine();
			}
			reader.close();
			reader = new BufferedReader(new FileReader(RoutesPathsGenerator.NEW_NETWORK_LINKS_FILE));
			line = reader.readLine();
			while(line!=null) {
				String fromNode = reader.readLine(), toNode = reader.readLine();
				double distance = CoordUtils.calcDistance(network.getNodes().get(new IdImpl(fromNode)).getCoord(),network.getNodes().get(new IdImpl(toNode)).getCoord());
				Link link = new LinkFactoryImpl().createLink(new IdImpl(line), network.getNodes().get(new IdImpl(fromNode)), network.getNodes().get(new IdImpl(toNode)), network, distance, DEFAULT_FREE_SPEED, DEFAULT_CAPCITY, 1);
				Set<String> modes = new HashSet<String>();
				modes.add("car");
				modes.add(RouteTypes.BUS.name);
				link.setAllowedModes(modes);
				network.addLink(link);
				line = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Loads all the GTFS information form the roots field
	 */
	private void loadGTFSFiles() {
		try {
			timeFormat = new SimpleDateFormat("HH:mm:ss");
			BufferedReader reader = null;
			String line = null;
			//Files load
			int size = roots.length;
			stops = new Map[size];
			services = new Map[size];
			shapes = new Map[size]; 
			routes = new SortedMap[size];
			int publicSystemNumber=0;
			for(File root:roots) {
				stops[publicSystemNumber]=new HashMap<String, Stop>();
				services[publicSystemNumber]=new HashMap<String, Service>();
				shapes[publicSystemNumber]=new HashMap<String, Shape>();
				routes[publicSystemNumber]=new TreeMap<String, Route>();
				for(GTFSDefinitions gtfs:GTFSDefinitions.values()) {
					File file = new File(root.getPath()+"/"+gtfs.fileName);
					reader = new BufferedReader(new FileReader(file));
					int[] indices = gtfs.getIndices(reader.readLine());
					line = reader.readLine();
					while(line!=null) {
						String[] parts = line.split(",");
						Method m = GTFS2MATSimTransitScheduleFileWriter.class.getMethod(gtfs.getFunction(), new Class[] {String[].class,int[].class,int.class});
						m.invoke(this, new Object[]{parts,indices,publicSystemNumber});
						line = reader.readLine();
					}
					reader.close();
				}
				publicSystemNumber++;
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		/*try {
			calculateShapePoints();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
	}
	/*private void calculateShapePoints() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter("./data/shapesDistance");
		for(Route route:routes[0].values())
			for(Entry<String,Trip> tripE:route.getTrips().entrySet()) {
				Shape shape = tripE.getValue().getShape();
				if(shape!=null) {
					double avg = 0;
					SortedMap<Integer, Coord> ps = shape.getPoints();
					for(int i=1; i<ps.size(); i++)
						avg+=CoordUtils.calcDistance(ps.get(i), ps.get(i+1))*(6371000*Math.PI)/180;
					avg/=ps.size();
					pw.println(tripE.getKey()+" "+avg);
				}
			}
		pw.close();
	}*/

	/**
	 * Methods for processing one line of each file
	 * @param parts
	 * @param indices
	 */
	public void processStop(String[] parts, int[] indices, int publicSystemNumber) {
		stops[publicSystemNumber].put(parts[indices[0]],new Stop(new CoordImpl(Double.parseDouble(parts[indices[1]]),Double.parseDouble(parts[indices[2]])),parts[indices[3]],true));
	}
	public void processCalendar(String[] parts, int[] indices, int publicSystemNumber) {
		boolean[] days = new boolean[7];
		for(int d=0; d<days.length; d++)
			days[d]=parts[d+indices[1]].equals("1");
		services[publicSystemNumber].put(parts[indices[0]], new Service(days, parts[indices[2]], parts[indices[3]]));
	}
	public void processCalendarDate(String[] parts, int[] indices, int publicSystemNumber) {
		Service actual = services[publicSystemNumber].get(parts[indices[0]]);
		if(parts[indices[2]].equals("2"))
			actual.addException(parts[indices[1]]);
		else
			actual.addAddition(parts[indices[1]]);
	}
	public void processShape(String[] parts, int[] indices, int publicSystemNumber) {
		Shape actual = shapes[publicSystemNumber].get(parts[indices[0]]);
		if(actual==null) {
			actual = new Shape(parts[indices[0]]);
			shapes[publicSystemNumber].put(parts[indices[0]], actual);
		}
		actual.addPoint(new CoordImpl(Double.parseDouble(parts[indices[1]]), Double.parseDouble(parts[indices[2]])),Integer.parseInt(parts[indices[3]]));
	}
	public void processRoute(String[] parts, int[] indices, int publicSystemNumber) {
		routes[publicSystemNumber].put(parts[indices[0]], new Route(parts[indices[1]], RouteTypes.values()[Integer.parseInt(parts[indices[2]])]));
	}
	public void processTrip(String[] parts, int[] indices, int publicSystemNumber) {
		Route route = routes[publicSystemNumber].get(parts[indices[0]]);
		if(parts.length==5)
			route.putTrip(parts[indices[1]], new Trip(services[publicSystemNumber].get(parts[indices[2]]), shapes[publicSystemNumber].get(parts[indices[3]]),parts[indices[1]]));
		else
			route.putTrip(parts[indices[1]], new Trip(services[publicSystemNumber].get(parts[indices[2]]), null, parts[indices[1]]));
	}
	public void processStopTime(String[] parts, int[] indices, int publicSystemNumber) {
		for(Route actualRoute:routes[publicSystemNumber].values()) {
			Trip trip = actualRoute.getTrips().get(parts[indices[0]]);
			if(trip!=null) {
				try {
					trip.putStopTime(Integer.parseInt(parts[indices[1]]), new StopTime(timeFormat.parse(parts[indices[2]]),timeFormat.parse(parts[indices[3]]),parts[indices[4]]));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public void processFrequency(String[] parts, int[] indices, int publicSystemNumber) {
		for(Route actualRoute:routes[publicSystemNumber].values()) {
			Trip trip = actualRoute.getTrips().get(parts[indices[0]]);
			if(trip!=null) {
				try {
					trip.addFrequency(new Frequency(timeFormat.parse(parts[indices[1]]), timeFormat.parse(parts[indices[2]]), Integer.parseInt(parts[indices[3]])));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * From the loaded information calculates all the necessary information for MATSim
	 * @throws IOException 
	 */
	private void calculateUnknownInformation(CoordinateTransformation coordinateTransformation) throws IOException {
		//New Stops according to the modes
		for(int publicSystemNumber=0; publicSystemNumber<roots.length; publicSystemNumber++)
			for(Route route:routes[publicSystemNumber].values())
				for(Trip trip:route.getTrips().values())
					for(Entry<Integer,StopTime> stopTime:trip.getStopTimes().entrySet())
						if(stops[publicSystemNumber].get(stopTime.getValue().getStopId()).getRouteType()==null)
							stops[publicSystemNumber].get(stopTime.getValue().getStopId()).setRouteType(route.getRouteType());
		//Path
		boolean shape = false;
		if(shape)
			generateRepeatedMRTStops();
		for(int publicSystemNumber=0; publicSystemNumber<roots.length; publicSystemNumber++) {
			if(publicSystemNumber==0) {
				RoutesPathsGenerator routesPathsGenerator = new RoutesPathsGenerator(network, routes[publicSystemNumber], stops[publicSystemNumber]);
				routesPathsGenerator.run();
			}
			for(Entry<String,Route> route:routes[publicSystemNumber].entrySet())
				if(!route.getValue().getRouteType().wayType.equals(GTFSDefinitions.WayTypes.ROAD))
					for(Entry<String,Trip> trip:route.getValue().getTrips().entrySet())
						if(shape && trip.getValue().getShape()!= null)
							addNewLinksSequenceShape(trip, route.getValue().getRouteType(), route.getKey(), publicSystemNumber);
						else
							addNewLinksSequence(trip.getValue(), route.getValue().getRouteType(), route.getKey(), publicSystemNumber);
		}
		//Splitting of stop-links
		splitBusStopLinks(MAX_DISTANCE_STOP_LINK);
		/*for(Route route:routes[0].values())
			for(Entry<String,Trip> trip:route.getTrips().entrySet()) {
				Window window = new Window(trip.getKey(),network,trip.getValue(),stops[0]);
				window.setVisible(true);
				while(window.isVisible())
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}*/
		//Coordinates system of the network
		for(Node node:network.getNodes().values())
			((NodeImpl)node).setCoord(coordinateTransformation.transform(node.getCoord()));
		for(Link link:network.getLinks().values()) {
			if(((LinkImpl)link).getOrigId()!=null)
				((LinkImpl)link).setLength(((LinkImpl)link).getLength()*1000);
			else
				((LinkImpl)link).setLength(CoordUtils.calcDistance(link.getFromNode().getCoord(),link.getToNode().getCoord()));
			link.setFreespeed(link.getFreespeed()/3.6);
		}
	}
	private void generateRepeatedMRTStops() {
		for(Entry<String,Route> routeE:routes[1].entrySet()) {
			for(Trip trip:routeE.getValue().getTrips().values()) {
				String pStopTime = "";
				for(StopTime stopTime:trip.getStopTimes().values()) {
					Stop stop = stops[1].get(stopTime.getStopId());
					String[] parts = stopTime.getStopId().split("/");
					int iPart=-1;
					for(iPart++;iPart<parts.length && !parts[iPart].contains(routeE.getKey());iPart++);
					if(pStopTime.equals("") && iPart<parts.length) {
						if(parts[iPart].equals("EW23"))
							pStopTime = "EW24";
						else if(parts[iPart].equals("EW4"))
							pStopTime = "EW5";
						else if(parts[iPart].equals("EW24"))
							pStopTime = "EW23";
						else if(parts[iPart].equals("NS7"))
							pStopTime = "NS5";
						else if(parts[iPart].equals("NS13"))
							pStopTime = "NS11";
						else if(parts[iPart].equals("NS21"))
							pStopTime = "NS22";
						else if(parts[iPart].equals("NS16"))
							pStopTime = "NS17";
						else if(parts[iPart].equals("CC12"))
							pStopTime = "CC11";
						
					}
					String newId = pStopTime+"_to_"+(iPart<parts.length?parts[iPart]:stopTime.getStopId());
					stops[1].put(newId,stop);
					stopTime.setStopId(newId);
					pStopTime = (iPart<parts.length?parts[iPart]:stopTime.getStopId());
				}
			}
		}
		Object[] stopIds = stops[1].keySet().toArray();
		for(Object stopId:stopIds)
			if(!((String)stopId).contains("_to_"))
				stops[1].remove(stopId);
		System.out.println();
	}
	/**
	 * Methods for write a new or calculated sequence of links for a trip
	 * @param trip
	 * @param withShape
	 */
	private void addNewLinksSequence(Trip trip, RouteTypes routeType, String routeKey, int publicSystemNumber) {
		double length;
		double freeSpeed=50;
		double capacity=1000;
		double nOfLanes=1;
		StopTime stopTime = trip.getStopTimes().get(1);
		String id = stopTime.getStopId();
		Stop stop = stops[publicSystemNumber].get(id);
		NodeImpl node = (NodeImpl) network.getNodes().get(new IdImpl(id));
		if(node==null) {
			node = new NodeImpl(new IdImpl(id));
			node.setCoord(stop.getPoint());
			network.addNode(node);
		}
		String id2 = trip.getStopTimes().get(2).getStopId();
		Stop stop2 = stops[publicSystemNumber].get(id2);
		NodeImpl node2 = (NodeImpl) network.getNodes().get(new IdImpl(id2));
		if(node2==null) {
			node2 = new NodeImpl(new IdImpl(id2));
			node2.setCoord(stop2.getPoint());
			network.addNode(node2);
		}
		id2 = trip.getStopTimes().get(2).getStopId()+"_"+trip.getStopTimes().get(1).getStopId();
		Link link = network.getLinks().get(new IdImpl(id2));
		if(link==null) {
			length = CoordUtils.calcDistance(node2.getCoord(), node.getCoord());
			link = new LinkFactoryImpl().createLink(new IdImpl(id2), node2, node, network, length, freeSpeed, capacity, nOfLanes);
			Set<String> modes = new HashSet<String>();
			modes.add(routeType.name);
			link.setAllowedModes(modes);
			network.addLink(link);
		}
		trip.addLink(link);
		String[] parts = id.split("/");
		if(parts.length>1) {
			for(String part:parts)
				if(part.startsWith(routeKey)) {
					Stop nStop = stops[publicSystemNumber].get(part);
					Stop nStopR = stops[publicSystemNumber].get(part+"_r");
					if(nStop==null && nStopR==null) {
						nStop = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
						nStop.setLinkId(id2);
						nStop.setFixedLinkId();
						stops[publicSystemNumber].put(part,nStop);
						stopTime.setStopId(part);
					}
					else if(nStopR==null && !nStop.getLinkId().equals(id2)) {
							nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
							nStopR.setLinkId(id2);
							nStopR.setFixedLinkId();
							stops[publicSystemNumber].put(part+"_r",nStopR);
							stopTime.setStopId(part+"_r");
					}
					else
						if(id2.equals(nStop.getLinkId()))
							stopTime.setStopId(part);
						else
							stopTime.setStopId(part+"_r");
				}
		}
		else
			if(!stop.setLinkId(id2) && !stop.getLinkId().equals(id2)) {
				Stop nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
				nStopR.setLinkId(id2);
				nStopR.setFixedLinkId();
				stops[publicSystemNumber].put(id+"_r",nStopR);
				stopTime.setStopId(id+"_r");
			}
			else
				stop.setFixedLinkId();
		Node previous = node;
		String prevId = id; 
		for(int p=2; p<=trip.getStopTimes().lastKey(); p++)
			if(trip.getStopTimes().get(p)!=null) {
				stopTime = trip.getStopTimes().get(p);
				id = stopTime.getStopId();
				stop = stops[publicSystemNumber].get(id);
				node = (NodeImpl) network.getNodes().get(new IdImpl(id));
				if(node==null) {
					node = new NodeImpl(new IdImpl(id));
					node.setCoord(stop.getPoint());
					network.addNode(node);
				}
				id2 = prevId+"_"+id;
				link = network.getLinks().get(new IdImpl(id2));
				if(link==null) {
					length = CoordUtils.calcDistance(previous.getCoord(), node.getCoord());
					link = new LinkFactoryImpl().createLink(new IdImpl(id2), previous, node, network, length, freeSpeed, capacity, nOfLanes);
					Set<String> modes = new HashSet<String>();
					modes.add(routeType.name);
					link.setAllowedModes(modes);
					network.addLink(link);
				}
				trip.addLink(link);
				parts = id.split("/");
				if(parts.length>1) {
					for(String part:parts)
						if(part.startsWith(routeKey)) {
							Stop nStop = stops[publicSystemNumber].get(part);
							Stop nStopR = stops[publicSystemNumber].get(part+"_r");
							if(nStop==null && nStopR==null) {
								nStop = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
								nStop.setLinkId(id2);
								nStop.setFixedLinkId();
								stops[publicSystemNumber].put(part,nStop);
								stopTime.setStopId(part);
							}
							else if(nStopR==null && !nStop.getLinkId().equals(id2)) {
								nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
								nStopR.setLinkId(id2);
								nStopR.setFixedLinkId();
								stops[publicSystemNumber].put(part+"_r",nStopR);
								stopTime.setStopId(part+"_r");
							}
							else
								if(id2.equals(nStop.getLinkId()))
									stopTime.setStopId(part);
								else
									stopTime.setStopId(part+"_r");
						}
				}
				else
					if(!stop.setLinkId(id2) && !stop.getLinkId().equals(id2)) {
						Stop nStopR = new Stop(stop.getPoint(), stop.getName(), stop.isBlocks());
						nStopR.setLinkId(id2);
						nStopR.setFixedLinkId();
						stops[publicSystemNumber].put(id+"_r",nStopR);
						stopTime.setStopId(id+"_r");
					}
					else
						stop.setFixedLinkId();
				previous = node;
				prevId = id;
			}
		/*Window window = new Window("ssss",network,trip,stops[r]);
		window.setVisible(true);
		while(window.isVisible())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
	}
	private void addNewLinksSequenceShape(Entry<String,Trip> tripE, RouteTypes routeType, String routeKey, int publicSystemNumber) {
		Shape shape = tripE.getValue().getShape();
		Iterator<StopTime> iStopTime=tripE.getValue().getStopTimes().values().iterator();
		String stopId = iStopTime.next().getStopId();
		double nearest = CoordUtils.calcDistance(shape.getPoints().get(1),stops[publicSystemNumber].get(stopId).getPoint());
		int nearI = 1;
		for(int n=2; n<shape.getPoints().size(); n++) {
			double distance = CoordUtils.calcDistance(shape.getPoints().get(n),stops[publicSystemNumber].get(stopId).getPoint());
			if(distance<nearest) {
				nearest = distance;
				nearI = n;
			}
		}
		Point2D pPoint = nearI==1?null:new Point2D(shape.getPoints().get(nearI-1).getX(), shape.getPoints().get(nearI-1).getY());
		Point2D point = new Point2D(shape.getPoints().get(nearI).getX(), shape.getPoints().get(nearI).getY());
		Point2D nPoint = new Point2D(shape.getPoints().get(nearI+1).getX(), shape.getPoints().get(nearI+1).getY());
		Point2D sPoint = new Point2D(stops[publicSystemNumber].get(stopId).getPoint().getX(), stops[publicSystemNumber].get(stopId).getPoint().getY());
		Line2D line = pPoint==null?null:new Line2D(pPoint, point);
		Line2D lineN = new Line2D(point, nPoint);
		Node previous = null, node = null;
		Link link = null;
		if(line!=null && line.isNearestInside(sPoint))
			nearI--;
		else if(!lineN.isNearestInside(sPoint)) {
			Iterator<StopTime> iStopTime2=tripE.getValue().getStopTimes().values().iterator();
			iStopTime2.next();
			String stop2Id = iStopTime2.next().getStopId();
			node = addNode(stop2Id, stops[1].get(stop2Id).getPoint());
			for(previous = node; !(line!=null && line.isNearestInside(sPoint)) && !lineN.isNearestInside(sPoint);previous = node) {
				node = addNode(stopId,stops[1].get(stopId).getPoint());
				String idS = stopId;
				link = addLink(idS, previous, node, routeType);
				tripE.getValue().addLink(link);
				stops[publicSystemNumber].get(stopId).setLinkId(idS);
				if(iStopTime.hasNext())
					stopId = iStopTime.next().getStopId();
				nearest = CoordUtils.calcDistance(shape.getPoints().get(1),stops[publicSystemNumber].get(stopId).getPoint());
				nearI = 1;
				for(int n=2; n<shape.getPoints().size(); n++) {
					double distance = CoordUtils.calcDistance(shape.getPoints().get(n),stops[publicSystemNumber].get(stopId).getPoint());
					if(distance<nearest) {
						nearest = distance;
						nearI = n;
					}
				}
				pPoint = nearI==1?null:new Point2D(shape.getPoints().get(nearI-1).getX(), shape.getPoints().get(nearI-1).getY());
				point = new Point2D(shape.getPoints().get(nearI).getX(), shape.getPoints().get(nearI).getY());
				nPoint = new Point2D(shape.getPoints().get(nearI+1).getX(), shape.getPoints().get(nearI+1).getY());
				sPoint = new Point2D(stops[publicSystemNumber].get(stopId).getPoint().getX(), stops[publicSystemNumber].get(stopId).getPoint().getY());
				line = pPoint==null?null:new Line2D(pPoint, point);
				lineN = new Line2D(point, nPoint);
			}
			if(line!=null && line.isNearestInside(sPoint))
				nearI--;
		}
		node = addNode(shape.getId()+"_"+nearI, shape.getPoints().get(nearI));
		if(previous != null) {
			link = addLink("con"+tripE.getKey(), previous, node, routeType);
			tripE.getValue().addLink(link);
		}
		previous = node;
		boolean end=false;
		for(int p=nearI+1; !end; p++) {
			if(p<=shape.getPoints().size()) {
				if(!shape.getPoints().get(p).equals(node.getCoord())) {
					node = addNode(shape.getId()+"_"+p, shape.getPoints().get(p));
					String idS = nearI+"_"+shape.getId()+"_"+p;
					link =	addLink(idS,previous,node,routeType);
					nearI = p;
					tripE.getValue().addLink(link);
					pPoint = new Point2D(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
					point = new Point2D(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
					sPoint = new Point2D(stops[publicSystemNumber].get(stopId).getPoint().getX(), stops[publicSystemNumber].get(stopId).getPoint().getY());
					line = new Line2D(pPoint, point);
					if(line.isNearestInside(sPoint)) {
						stops[publicSystemNumber].get(stopId).setLinkId(idS);
						if(iStopTime.hasNext())
							stopId = iStopTime.next().getStopId();
						else
							end = true;
					}
				}
			}
			else {
				node = addNode(stopId,stops[1].get(stopId).getPoint());
				String idS = stopId;
				link = addLink(idS, previous, node, routeType);
				tripE.getValue().addLink(link);
				stops[publicSystemNumber].get(stopId).setLinkId(idS);
				if(iStopTime.hasNext()) {
					stopId = iStopTime.next().getStopId();
				}
				else
					end = true;
			}
			previous = node;
		}
		Window window = new Window(tripE.getKey(),network,tripE.getValue(),stops[publicSystemNumber]);
		window.setVisible(true);
		while(window.isVisible())
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	private Link addLink(String idS, Node previous, Node node, RouteTypes routeType) {
		double freeSpeed=100;
		double capacity=100;
		double nOfLanes=1;
		Id id = new IdImpl(idS);
		Link link = network.getLinks().get(id);
		if(link==null) {
			link = new LinkFactoryImpl().createLink(id, previous, node, network, 0, freeSpeed, capacity, nOfLanes);
			Set<String> modes = new HashSet<String>();
			modes.add(routeType.name);
			link.setAllowedModes(modes);
			network.addLink(link);
		}
		return link;
	}

	private Node addNode(String idS, Coord coord) {
		Id id = new IdImpl(idS);
		NodeImpl node = (NodeImpl) network.getNodes().get(id);
		if(node==null) {
			node = new NodeImpl(id);
			node.setCoord(coord);
			network.addNode(node);
		}
		return node;
	}
	/**
	 * Modifies the network avoiding big distances between stops and the end 
	 * @param maxDistanceStopLink
	 */
	private void splitBusStopLinks(double maxDistanceStopLink) {
		Map<String,LinkStops> linksStops = new HashMap<String,LinkStops>();
		for(Stop stop:stops[0].values()) {
			if(stop.getLinkId()!=null) {
				LinkStops linkStops = linksStops.get(stop.getLinkId());
				if(linkStops == null) {
					linkStops = new LinkStops(network.getLinks().get(new IdImpl(stop.getLinkId())));
					linksStops.put(stop.getLinkId(), linkStops);
				}
				linkStops.addStop(stop);
			}
		}
		for(LinkStops linkStops:linksStops.values()) {
			for(int i=0; i<linkStops.getNumStops()-1; i++)
				try {
					changeBusTrips(linkStops.getLink(),linkStops.split(i,network), false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			if(linkStops.getLastDistance()>MAX_DISTANCE_STOP_LINK)
				try {
					changeBusTrips(linkStops.getLink(),linkStops.split(linkStops.getNumStops()-1,network), true);
				} catch (Exception e) {
					e.printStackTrace();
				}	
		}	
	}

	private void changeBusTrips(Link link, Link split, boolean last) {
		for(Route route:routes[0].values())
			for(Trip trip:route.getTrips().values())
				for(int i=0; i<trip.getLinks().size(); i++)
					if(trip.getLinks().get(i).equals(link)) {
						String firstStopId = trip.getStopTimes().get(trip.getStopTimes().firstKey()).getStopId();
						if(i>0 || (i==0 && stops[0].get(firstStopId).getLinkId().equals(split.getId().toString()))) {
							trip.getLinks().add(i, split);
							i++;
							if(last && i==trip.getLinks().size()-1)
								trip.getLinks().remove(i);
						}
					}
	}

	
	/**
	 * Writes the MATSim public transport file
	 * @param filename
	 */
	public void write(String filename, CoordinateTransformation coordinateTransformation) {
			loadGTFSFiles();
			try {
				calculateUnknownInformation(coordinateTransformation);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Public Transport Schedule
			//writeXML(filename+"t");
			TransitScheduleFactory transitScheduleFactory = new TransitScheduleFactoryImpl();
			TransitSchedule transitSchedule = transitScheduleFactory.createTransitSchedule();
			for(int publicSystemNumber=0; publicSystemNumber<roots.length; publicSystemNumber++)
				for(Entry<String, Stop> stop: stops[publicSystemNumber].entrySet())
					if(stop.getValue().getLinkId()!=null) {
						Coord result = coordinateTransformation.transform(stop.getValue().getPoint());
						TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(new IdImpl(stop.getKey()), result, stop.getValue().isBlocks());
						transitStopFacility.setLinkId(new IdImpl(stop.getValue().getLinkId()));
						transitStopFacility.setName(stop.getValue().getName());
						transitSchedule.addStopFacility(transitStopFacility);
					}
			for(int publicSystemNumber=0; publicSystemNumber<roots.length; publicSystemNumber++)
				for(Entry<String,Route> route:routes[publicSystemNumber].entrySet()) {
					TransitLine transitLine = transitScheduleFactory.createTransitLine(new IdImpl(route.getKey()));
					transitSchedule.addTransitLine(transitLine);
					for(Entry<String,Trip> trip:route.getValue().getTrips().entrySet()) {
						boolean isService=false;
						for(String serviceId:serviceIds)
							if(trip.getValue().getService().equals(services[publicSystemNumber].get(serviceId)))
								isService = true;
						if(isService) {
							NetworkRoute networkRoute = (NetworkRoute) ((NetworkFactoryImpl)network.getFactory()).createRoute(/*TODO*/TransportMode.car, trip.getValue().getLinks().get(0).getId(), trip.getValue().getLinks().get(trip.getValue().getLinks().size()-1).getId());
							List<Id> intermediate = new ArrayList<Id>();
							for(Link link:trip.getValue().getLinks())
								intermediate.add(link.getId());
							intermediate.remove(0);
							intermediate.remove(intermediate.size()-1);
							networkRoute.setLinkIds(networkRoute.getStartLinkId(), intermediate, networkRoute.getEndLinkId());
							List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
							Date startTime = trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getArrivalTime();
							for(Integer stopTimeKey:trip.getValue().getStopTimes().keySet()) {
								StopTime stopTime = trip.getValue().getStopTimes().get(stopTimeKey);
								double arrival = Time.UNDEFINED_TIME, departure = Time.UNDEFINED_TIME;
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().firstKey())) {
									long difference = stopTime.getArrivalTime().getTime()-startTime.getTime();
									try {
										arrival = Time.parseTime(timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference)));
									} catch (ParseException e) {
										e.printStackTrace();
									}
								}
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().lastKey())) {
									long difference = stopTime.getDepartureTime().getTime()-startTime.getTime();
									try {
										departure = Time.parseTime(timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference)));
									} catch (ParseException e) {
										e.printStackTrace();
									}
								}
								transitRouteStops.add(transitScheduleFactory.createTransitRouteStop(transitSchedule.getFacilities().get(new IdImpl(stopTime.getStopId())),arrival,departure));
							}
							TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(new IdImpl(trip.getKey()), networkRoute, transitRouteStops, route.getValue().getRouteType().name);
							transitLine.addRoute(transitRoute);
							int id = 1;
							for(Frequency frequency:trip.getValue().getFrequencies())
								for(Date actualTime = (Date) frequency.getStartTime().clone(); actualTime.before(frequency.getEndTime()); actualTime.setTime(actualTime.getTime()+frequency.getSecondsPerDeparture()*1000)) {
									transitRoute.addDeparture(transitScheduleFactory.createDeparture(new IdImpl(id), Time.parseTime(timeFormat.format(actualTime))));
									id++;
								}
							if(id==1)
								transitRoute.addDeparture(transitScheduleFactory.createDeparture(new IdImpl(id), Time.parseTime(timeFormat.format(trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getDepartureTime()))));
						}
					}
				}
			(new TransitScheduleWriter(transitSchedule)).writeFile(filename);
	}
	/**
	 * Writes the Transit Schedule file
	 */
	/*private void writeXML(String filename) {
		try {
			//Stops
			this.openFile(filename);
			this.writeXmlHead();
			this.writeDoctype("transitSchedule", "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd");
			this.writeStartTag("transitSchedule", new ArrayList<Tuple<String,String>>());
			this.writeStartTag("transitStops", new ArrayList<Tuple<String,String>>());
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
			for(int r=0; r<roots.length; r++)
				for(Entry<String, Stop> stop: stops[r].entrySet())
					if(stop.getValue().getLinkId()!=null) {
						Coord result = ct.transform(stop.getValue().getPoint());
						List<Tuple<String, String>> params = new ArrayList<Tuple<String,String>>();
						params.add(new Tuple<String, String>("id", stop.getKey()));
						params.add(new Tuple<String, String>("x", Double.toString(result.getX())));
						params.add(new Tuple<String, String>("y", Double.toString(result.getY())));
						params.add(new Tuple<String, String>("linkRefId", stop.getValue().getLinkId()));
						params.add(new Tuple<String, String>("name", stop.getValue().getName()));
						params.add(new Tuple<String, String>("isBlocking", Boolean.toString(stop.getValue().isBlocks())));
						this.writeStartTag("stopFacility", params,true);
					}
			writeEndTag("transitStops");
			//Lines
			for(int r=0; r<roots.length; r++)
				for(Entry<String,Route> route:routes[r].entrySet()) {
					List<Tuple<String,String>> routeAtts = new ArrayList<Tuple<String,String>>();
					routeAtts.add(new Tuple<String, String>("id", route.getKey()));
					this.writeStartTag("transitLine", routeAtts);
					for(Entry<String,Trip> trip:route.getValue().getTrips().entrySet()) {
						boolean isService=false;
						for(String serviceId:serviceIds)
							if(trip.getValue().getService().equals(services[r].get(serviceId)))
								isService = true;
						if(isService) {
							List<Tuple<String,String>> tripAtts = new ArrayList<Tuple<String,String>>();
							tripAtts.add(new Tuple<String, String>("id", trip.getKey()));
							this.writeStartTag("transitRoute", tripAtts);
							//Mode
							this.writeStartTag("transportMode", new ArrayList<Tuple<String,String>>());
							this.writeContent(route.getValue().getRouteType().name, true);
							this.writeEndTag("transportMode");
							//Route profile
							this.writeStartTag("routeProfile", new ArrayList<Tuple<String,String>>());
							Date startTime = trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getArrivalTime();
							for(Integer stopTimeKey:trip.getValue().getStopTimes().keySet()) {
								StopTime stopTime = trip.getValue().getStopTimes().get(stopTimeKey);
								List<Tuple<String,String>> stopTimeAtts = new ArrayList<Tuple<String,String>>();
								stopTimeAtts.add(new Tuple<String, String>("refId", stopTime.getStopId()));
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().firstKey())) {
									long difference = stopTime.getArrivalTime().getTime()-startTime.getTime();
									stopTimeAtts.add(new Tuple<String, String>("arrivalOffset", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
								}
								if(!stopTimeKey.equals(trip.getValue().getStopTimes().lastKey())) {
									long difference = stopTime.getDepartureTime().getTime()-startTime.getTime();
									stopTimeAtts.add(new Tuple<String, String>("departureOffset", timeFormat.format(new Date(timeFormat.parse("00:00:00").getTime()+difference))));
								}
								this.writeStartTag("stop", stopTimeAtts, true);
							}
							this.writeEndTag("routeProfile");
							//Route
							this.writeStartTag("route", new ArrayList<Tuple<String,String>>());
							for(Link link:trip.getValue().getLinks()) {
								List<Tuple<String,String>> linkAtts = new ArrayList<Tuple<String,String>>();
								linkAtts.add(new Tuple<String, String>("refId", link.getId().toString()));
								this.writeStartTag("link", linkAtts, true);
							}
							this.writeEndTag("route");
							//Departures
							int id = 1;
							this.writeStartTag("departures", new ArrayList<Tuple<String,String>>());
							for(Frequency frequency:trip.getValue().getFrequencies())
								for(Date actualTime = (Date) frequency.getStartTime().clone(); actualTime.before(frequency.getEndTime()); actualTime.setTime(actualTime.getTime()+frequency.getSecondsPerDeparture()*1000)) {
									List<Tuple<String,String>> departureAtts = new ArrayList<Tuple<String,String>>();
									departureAtts.add(new Tuple<String, String>("id", Integer.toString(id)));
									departureAtts.add(new Tuple<String, String>("departureTime", timeFormat.format(actualTime)));
									this.writeStartTag("departure", departureAtts, true);
									id++;
								}
							if(id==1) {
								List<Tuple<String,String>> departureAtts = new ArrayList<Tuple<String,String>>();
								departureAtts.add(new Tuple<String, String>("id", Integer.toString(id)));
								departureAtts.add(new Tuple<String, String>("departureTime", timeFormat.format(trip.getValue().getStopTimes().get(trip.getValue().getStopTimes().firstKey()).getDepartureTime())));
								this.writeStartTag("departure", departureAtts, true);
							}
							this.writeEndTag("departures");
							this.writeEndTag("transitRoute");
						}
					}
					this.writeEndTag("transitLine");
				}
			this.writeEndTag("transitSchedule");
			this.close();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}*/
	//Main method
	/**
	 * @param args:
	 * 0 - Transit schedule file
	 * 1 - Input network file
	 * 2 - Output network file
	 * 3 - Name of the network 
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario);
		matsimNetworkReader.readFile(args[1]);
		Network network = scenario.getNetwork();
		GTFS2MATSimTransitScheduleFileWriter g2m = new GTFS2MATSimTransitScheduleFileWriter(new File[]{new File("./data/gtfs/buses"),new File("./data/gtfs/trains")}, network, new String[]{"weekday","weeksatday","daily"});
		//Transformation for Singapore
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_SVY21);
		g2m.write(args[0],coordinateTransformation);
		//Write modified network
		((NetworkImpl)network).setName(args[3]);
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write(args[2]);
	}

}
