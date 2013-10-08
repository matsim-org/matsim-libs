package playground.sergioo.scheduling2013.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.geometry.Vector3D;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.PlaceSharer.KnownPlace;
import processing.core.PApplet;

public class WorldVisualizer implements Visualizer {

	private static final double MAX_DISTANCE = 2;

	private static final double R = 50;
	
	private final Network network;
	private final Map<Id, Map<String, Set<double[]>>> activityTypes = new HashMap<Id, Map<String,Set<double[]>>>();
	private final Map<Id, Coord> coords = new HashMap<Id, Coord>();
	private VisualizersSet visualizersSet;
	private Id nearestCity;
	private double smallestTime;

	
	public WorldVisualizer(VisualizersSet visualizersSet, PlaceSharer placeSharer, Map<Id, ? extends ActivityFacility> facilities, Network network, double smallestTime) {
		this.visualizersSet = visualizersSet;
		this.network = network;
		for(KnownPlace knownPlace:placeSharer.getKnownPlaces()) {
			Id id = knownPlace.getFacilityId();
			coords.put(id, facilities.get(id).getCoord());
			Map<String, Set<double[]>> map = new HashMap<String, Set<double[]>>();
			activityTypes.put(id, map);
			for(String activityType:knownPlace.getActivityTypes())
				map.put(activityType, knownPlace.getTimes(activityType));
		}
		this.smallestTime = smallestTime;
	}
	public void setPoint(Vector3D vector) {
		Coord point = new CoordImpl(vector.getX(), vector.getZ());
		Id nearest = coords.keySet().iterator().hasNext()?coords.keySet().iterator().next():null;
		for(Entry<Id, Coord> coord:coords.entrySet())
			if(CoordUtils.calcDistance(point, coords.get(nearest))>CoordUtils.calcDistance(point, coord.getValue()))
				nearest = coord.getKey();
		if(nearest!=null) {
			if(CoordUtils.calcDistance(point, coords.get(nearest))<MAX_DISTANCE && nearestCity!=nearest) {
				visualizersSet.setChange();
				nearestCity = nearest;
			}
			else if(CoordUtils.calcDistance(point, coords.get(nearest))>=MAX_DISTANCE){
				visualizersSet.setChange();
				nearestCity = null;
			}
		}
	}
	public Id getNearestFacility() {
		return nearestCity;
	}
	public void paintOnce(PApplet applet) {
		applet.stroke(155, 155, 155);
		applet.noFill();
		for(Link link:network.getLinks().values()) {
			applet.strokeWeight((float) (10f*link.getCapacity()/1500));
			applet.line((float)link.getFromNode().getCoord().getX(), (float)-link.getFromNode().getCoord().getY(), 0, (float)link.getToNode().getCoord().getX(), (float)-link.getToNode().getCoord().getY(), 0);
		}
		for(Entry<Id, Map<String, Set<double[]>>> place:activityTypes.entrySet()) {
			Coord coord = coords.get(place.getKey());
			Vector3D diff = new Vector3D(coord.getX(), 0, coord.getY()).subtract(visualizersSet.getEye());
			Vector3D diff2 = visualizersSet.getCenter().subtract(visualizersSet.getEye());
			if(diff.getNorm()>50 && Vector3D.angle(diff, diff2)<Math.PI/3)
				paintFacility(applet, coord, place.getValue(), activityTypes.size());
		}
		if(nearestCity!=null) {
			applet.strokeWeight(20f);
			applet.stroke(300, 0, 0);
			applet.point((float)coords.get(nearestCity).getX(), (float)-coords.get(nearestCity).getY(), 0.001f);
		}
	}
	public void paint(PApplet applet, double time) {
		
	}
	private void paintFacility(PApplet applet, Coord coordinate, Map<String, Set<double[]>> placeActivityTypes, int numFacilities) {
		if(nearestCity==null || !coords.get(nearestCity).equals(coordinate)) {
			Color color = null;
			double angle = 0, deltaAngle = 2*Math.PI/placeActivityTypes.size();
			double min=Double.MAX_VALUE, max=0;
			for(Entry<String, Set<double[]>> intervals:placeActivityTypes.entrySet()) {
				color = VisualizersSet.COLORS.get(intervals.getKey());
				Coord coord;
				if(placeActivityTypes.size()<=1)
					coord = coordinate;
				else
					coord = new CoordImpl(coordinate.getX()+R*Math.cos(angle), coordinate.getY()+R*Math.sin(angle));
				applet.strokeWeight(200f);
				applet.stroke(color.getRed(), color.getGreen(), color.getBlue());
				applet.point((float)coord.getX(), -(float)coord.getY(), 0.001f);
				applet.strokeWeight(30f);
				for(double[] interval:intervals.getValue()) {
					if(interval[0]<min)
						min = interval[0];
					if(interval[1]>max)
						max = interval[1];
					/*applet.line((float)coord.getX(), -(float)coord.getY(), (float)(interval[0]-smallestTime)*SchedulingNetworkVisualizer.FACTOR,
							(float)coord.getX(), -(float)coord.getY(), (float)(interval[1]-smallestTime)*SchedulingNetworkVisualizer.FACTOR);*/
				}
				angle += deltaAngle;
			}
			applet.strokeWeight(30f);
			if(min<max) {
				applet.stroke(55, 55, 0);
				applet.line((float)coordinate.getX(), -(float)coordinate.getY(), (float)(min-smallestTime)*SchedulingNetworkVisualizer.FACTOR,
						(float)coordinate.getX(), -(float)coordinate.getY(), (float)(max-smallestTime)*SchedulingNetworkVisualizer.FACTOR);
			}
		}
	}

}
