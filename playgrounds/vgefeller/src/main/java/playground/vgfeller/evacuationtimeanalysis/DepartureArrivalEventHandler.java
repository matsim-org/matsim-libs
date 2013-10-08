package playground.vgfeller.evacuationtimeanalysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;




public class DepartureArrivalEventHandler implements
		PersonDepartureEventHandler, PersonArrivalEventHandler {

	private final Map<Id,PersonDepartureEvent> events = new HashMap<Id, PersonDepartureEvent>();
	
	private final double CELL_SIZE = 250;	
	
	private double timeSum = 0;
	private int arrivals = 0;



	private final Network network;



	private QuadTree<Cell> tree;
	
	public DepartureArrivalEventHandler(Network network) {
		this.network = network;
		init();
	}

	private void init() {
		
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		for (Node n : this.network.getNodes().values()) {
			double x = n.getCoord().getX();
			double y = n.getCoord().getY();
			
			if (x < minX) {
				minX = x;
			}
			
			if (x > maxX) {
				maxX = x;
			}
			
			if (y < minY) {
				minY = y;
			}
			
			if (y > maxY) {
				maxY = y;
			}
		}
		
		
		
		
		
		this.tree = new QuadTree<Cell>(minX,minY,maxX,maxY);
		
		for (double x = minX; x <= maxX; x += CELL_SIZE) {
			for (double y = minY; y <= minY; y += CELL_SIZE) {
				Cell cell = new Cell();
				this.tree.put(x, y, cell);
			}
			
		}
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		PersonDepartureEvent departure = this.events.get(event.getPersonId());
	
		Link link = this.network.getLinks().get(departure.getLinkId());
		
		Coord c = link.getCoord();
		
		Cell cell = this.tree.get(c.getX(), c.getY());
		
		double time = event.getTime() - departure.getTime();

		cell.timeSum += time;
		cell.count++;
		
		this.timeSum += time;
		arrivals++;
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.events.put(event.getPersonId(), event);
	}
	

	public double getAverageTravelTime() {
		return this.timeSum/this.arrivals;
	}
	
	public double getAverageCellTravelTime(double x, double y) {
		Cell cell = this.tree.get(x, y);
		return cell.timeSum/cell.count;
	}
	
	
	private static class Cell {
		double timeSum;
		int count;
		
	}
}
