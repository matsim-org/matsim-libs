package playground.sergioo.routingAnalysisCEPAS2013;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import others.sergioo.visUtils.JetColor;
import playground.sergioo.routingAnalysisCEPAS2013.MainRoutes.Journey;
import playground.sergioo.routingAnalysisCEPAS2013.MainRoutes.Trip;
import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;

public class RoutesPainter extends Painter {
	
	//Classes
	private class JourneyPainter extends Painter {

		private Set<TripPainter> tripPainters = new HashSet<TripPainter>();
		
		public JourneyPainter(Journey journey, Integer width, Color color) {
			for(Trip trip:journey.trips)
				tripPainters.add(new TripPainter(trip, width, color));
		}
		@Override
		public void paint(Graphics2D g2, LayersPanel layersPanel) {
			for(TripPainter tripPainter:tripPainters)
				tripPainter.paint(g2, layersPanel);
		}
		
	}
	
	private class TripPainter extends Painter {

		private static final float FACTOR = 2f;
		
		private Trip trip;
		private Integer width;
		private Color color;
		
		public TripPainter(Trip trip, Integer width, Color color) {
			this.trip = trip;
			this.width = width;
			this.color = color;
		}
		@Override
		public void paint(Graphics2D g2, LayersPanel layersPanel) {
			this.paintLine(g2, layersPanel, new double[]{trip.startLon, trip.startLat}, new double[]{trip.endLon, trip.endLat}, new BasicStroke(width*FACTOR), color);
		}
		
	}
	
	//Attributes
	private Set<JourneyPainter> journeyPainters = new HashSet<JourneyPainter>();
	
	//Methods
	public RoutesPainter(Map<Journey, Integer> journeys) {
		float i=0;
		for(Entry<Journey, Integer> journey:journeys.entrySet()) {
			journeyPainters.add(new JourneyPainter(journey.getKey(), journey.getValue(), JetColor.getJetColor(i/journeys.size())));
			i++;
		}
	}
	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		for(JourneyPainter journeyPainter:journeyPainters)
			journeyPainter.paint(g2, layersPanel);
	}

}
