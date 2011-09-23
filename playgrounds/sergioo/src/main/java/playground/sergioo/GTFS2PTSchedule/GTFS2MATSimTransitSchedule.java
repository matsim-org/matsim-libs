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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
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
import playground.sergioo.GTFS2PTSchedule.PathEditor.kernel.RoutesPathsGenerator;
import playground.sergioo.GTFS2PTSchedule.GTFSDefinitions.RouteTypes;


public class GTFS2MATSimTransitSchedule {
	
	//Constants
	/**
	 * Maximum distance allowed between an stop and the end of the corresponding link
	 */
	private static final double MAX_DISTANCE_STOP_LINK = 50*180/(6371000*Math.PI);
	private static final double DEFAULT_FREE_SPEED = 6;
	private static final double DEFAULT_CAPACITY = 500;
	
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
	 * The calendar services
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
	/**
	 * Desired coordinates system
	 */
	private CoordinateTransformation coordinateTransformation;
	
	//Methods
	/**
	 * @param root
	 * @param network
	 */
	public GTFS2MATSimTransitSchedule(File[] roots, Network network, String[] serviceIds, String outCoordinateSystem) {
		super();
		this.roots = roots;
		this.network = network;
		this.serviceIds = serviceIds;
		this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outCoordinateSystem);
		updateNetwork();
	}
	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return network;
	}
	private void updateNetwork() {
		try {
			if(!RoutesPathsGenerator.NEW_NETWORK_NODES_FILE.exists())
				if(!RoutesPathsGenerator.NEW_NETWORK_NODES_FILE.createNewFile())
					throw new IOException();
			BufferedReader reader = new BufferedReader(new FileReader(RoutesPathsGenerator.NEW_NETWORK_NODES_FILE));
			String line = reader.readLine();
			while(line!=null) {
				Id id = new IdImpl(line);
				network.addNode(network.getFactory().createNode(id, new CoordImpl(Double.parseDouble(reader.readLine()), Double.parseDouble(reader.readLine()))));
				line = reader.readLine();
			}
			reader.close();
			if(!RoutesPathsGenerator.NEW_NETWORK_LINKS_FILE.exists())
				if(!RoutesPathsGenerator.NEW_NETWORK_LINKS_FILE.createNewFile())
					throw new IOException();
			reader = new BufferedReader(new FileReader(RoutesPathsGenerator.NEW_NETWORK_LINKS_FILE));
			line = reader.readLine();
			LinkFactory linkFactory = new LinkFactoryImpl();
			while(line!=null) {
				Node fromNode = network.getNodes().get(new IdImpl(reader.readLine()));
				Node toNode = network.getNodes().get(new IdImpl(reader.readLine()));
				double length = CoordUtils.calcDistance(coordinateTransformation.transform(fromNode.getCoord()),coordinateTransformation.transform(toNode.getCoord()));
				Link link = linkFactory.createLink(new IdImpl(line), fromNode, toNode, network, length, DEFAULT_FREE_SPEED, DEFAULT_CAPACITY, 1);
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
						Method m = GTFS2MATSimTransitSchedule.class.getMethod(gtfs.getFunction(), new Class[] {String[].class,int[].class,int.class});
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
	}

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
	private void calculateUnknownInformation() throws IOException {
		//Setting Route types to the stops
		for(int publicSystemNumber=0; publicSystemNumber<roots.length; publicSystemNumber++)
			for(Route route:routes[publicSystemNumber].values())
				for(Trip trip:route.getTrips().values())
					for(Entry<Integer,StopTime> stopTime:trip.getStopTimes().entrySet())
						if(stops[publicSystemNumber].get(stopTime.getValue().getStopId()).getRouteType()==null)
							stops[publicSystemNumber].get(stopTime.getValue().getStopId()).setRouteType(route.getRouteType());
		//Interactive tool for calculating routes path and stops link
		for(byte publicSystemNumber=0; publicSystemNumber<roots.length; publicSystemNumber++) {
			RoutesPathsGenerator routesPathsGenerator = new RoutesPathsGenerator(network, roots[publicSystemNumber], routes[publicSystemNumber], stops[publicSystemNumber]);
			routesPathsGenerator.run();
			//Splitting of stop-links
			splitStopLinks(publicSystemNumber);
		}
	}
	/**
	 * Modifies the network avoiding big distances between stops and the end of the related links
	 * @param maxDistanceStopLink
	 */
	private void splitStopLinks(byte publicTransportSystem) {
		Map<String,LinkStops> linksStops = new HashMap<String,LinkStops>();
		for(Stop stop:stops[publicTransportSystem].values()) {
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
					changeTrips(linkStops.getLink(),linkStops.split(i,network, coordinateTransformation), false, publicTransportSystem);
				} catch (Exception e) {
					e.printStackTrace();
				}
			if(linkStops.getLastDistance()>MAX_DISTANCE_STOP_LINK)
				try {
					changeTrips(linkStops.getLink(),linkStops.split(linkStops.getNumStops()-1,network, coordinateTransformation), true, publicTransportSystem);
				} catch (Exception e) {
					e.printStackTrace();
				}	
		}	
	}
	private void changeTrips(Link link, Link split, boolean last, byte publicTransporSystem) {
		for(Route route:routes[publicTransporSystem].values())
			for(Trip trip:route.getTrips().values())
				for(int i=0; i<trip.getLinks().size(); i++)
					if(trip.getLinks().get(i).equals(link)) {
						String firstStopId = trip.getStopTimes().get(trip.getStopTimes().firstKey()).getStopId();
						if(i>0 || (i==0 && stops[publicTransporSystem].get(firstStopId).getLinkId().equals(split.getId().toString()))) {
							trip.getLinks().add(i, split);
							i++;
							if(last && i==trip.getLinks().size()-1)
								trip.getLinks().remove(i);
						}
					}
	}
	/**
	 * @param outCoordinateSystem the desired output coordinate system of the network nodes and the transit schedule stops
	 * @return the MATSim transit schedule
	 */
	public TransitSchedule getTransitSchedule() {
			loadGTFSFiles();
			try {
				calculateUnknownInformation();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Coordinates system of the network
			for(Node node:network.getNodes().values())
				((NodeImpl)node).setCoord(coordinateTransformation.transform(node.getCoord()));
			//Public Transport Schedule
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
							NetworkRoute networkRoute = new LinkNetworkRouteImpl(trip.getValue().getLinks().get(0).getId(), trip.getValue().getLinks().get(trip.getValue().getLinks().size()-1).getId());
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
			return transitSchedule;
	}
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
		(new MatsimNetworkReader(scenario)).readFile(args[1]);
		Network network = scenario.getNetwork();
		//Convert lengths of the link from km to m and speeds from km/h to m/s
		for(Link link:network.getLinks().values()) {
			link.setLength(link.getLength()*1000);
			link.setFreespeed(link.getFreespeed()/3.6);
		}
		//Construct conversion object
		GTFS2MATSimTransitSchedule g2m = new GTFS2MATSimTransitSchedule(new File[]{new File("./data/gtfs/buses"),new File("./data/gtfs/trains")}, network, new String[]{"weekday","weeksatday","daily"}, TransformationFactory.WGS84_SVY21);
		//Convert
		(new TransitScheduleWriter(g2m.getTransitSchedule())).writeFile(args[0]);
		//Write modified network
		((NetworkImpl)network).setName(args[3]);
		NetworkWriter networkWriter =  new NetworkWriter(network);
		networkWriter.write(args[2]);
	}

}
