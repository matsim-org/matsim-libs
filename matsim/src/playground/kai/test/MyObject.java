package playground.kai.test;

import java.util.TreeMap;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.geometry.Coord;
import org.matsim.world.Layer;
import org.matsim.world.Location;

public class MyObject implements BasicLink,BasicAct {
	
	BasicLinkImpl basicLink = new BasicLinkImpl(null, null, null, null) ;
	
	BasicActImpl basicAct = new BasicActImpl();

	public final void addDownMapping(Location other) {
		basicLink.addDownMapping(other);
	}

	public final void addUpMapping(Location other) {
		basicLink.addUpMapping(other);
	}

	public double calcDistance(Coord coord) {
		return basicLink.calcDistance(coord);
	}

	public final Location downLocation(Id id) {
		return basicLink.downLocation(id);
	}

	public boolean equals(Object obj) {
		return basicLink.equals(obj);
	}

	public double getCapacity(double time) {
		return basicLink.getCapacity(time);
	}

	public final Coord getCenter() {
		return basicLink.getCenter();
	}

	public final TreeMap<Id, Location> getDownMapping() {
		return basicLink.getDownMapping();
	}

	public double getFreespeed(double time) {
		return basicLink.getFreespeed(time);
	}

	public BasicNode getFromNode() {
		return basicLink.getFromNode();
	}

	public final Id getId() {
		return basicLink.getId();
	}

	public double getLanes(double time) {
		return basicLink.getLanes(time);
	}

	public int getLanesAsInt(double time) {
		return basicLink.getLanesAsInt(time);
	}

	public final Layer getLayer() {
		return basicLink.getLayer();
	}

	public double getLength() {
		return basicLink.getLength();
	}

	public BasicNode getToNode() {
		return basicLink.getToNode();
	}

	public final Location getUpLocation(Id id) {
		return basicLink.getUpLocation(id);
	}

	public final TreeMap<Id, Location> getUpMapping() {
		return basicLink.getUpMapping();
	}

	public int hashCode() {
		return basicLink.hashCode();
	}

	public final boolean removeAllDownMappings() {
		return basicLink.removeAllDownMappings();
	}

	public final boolean removeAllUpMappings() {
		return basicLink.removeAllUpMappings();
	}

	public void setCapacity(double capacity) {
		basicLink.setCapacity(capacity);
	}

	public void setFreespeed(double freespeed) {
		basicLink.setFreespeed(freespeed);
	}

	public boolean setFromNode(BasicNode node) {
		return basicLink.setFromNode(node);
	}

	public final void setId(Id id) {
		basicLink.setId(id);
	}

	public void setLanes(double lanes) {
		basicLink.setLanes(lanes);
	}

	public void setLength(double length) {
		basicLink.setLength(length);
	}

	public boolean setToNode(BasicNode node) {
		return basicLink.setToNode(node);
	}

	public String toString() {
		return basicLink.toString();
	}

	public final Coord getCoord() {
		return basicAct.getCoord();
	}

	public final double getEndTime() {
		return basicAct.getEndTime();
	}

	public Facility getFacility() {
		return basicAct.getFacility();
	}

	public BasicLink getLink() {
		return basicAct.getLink();
	}

	public final double getStartTime() {
		return basicAct.getStartTime();
	}

	public final String getType() {
		return basicAct.getType();
	}

	public void setCoord(Coord coord) {
		basicAct.setCoord(coord);
	}

	public final void setEndTime(double endTime) {
		basicAct.setEndTime(endTime);
	}

	public final void setFacility(Facility facility) {
		basicAct.setFacility(facility);
	}

	public final void setLink(BasicLink link) {
		basicAct.setLink(link);
	}

	public final void setStartTime(double startTime) {
		basicAct.setStartTime(startTime);
	}

	public final void setType(String type) {
		basicAct.setType(type);
	}



}
