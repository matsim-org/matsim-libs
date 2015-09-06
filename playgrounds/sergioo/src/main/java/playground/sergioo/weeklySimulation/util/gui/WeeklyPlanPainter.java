package playground.sergioo.weeklySimulation.util.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Time;

import playground.sergioo.visualizer2D2012.LayersPanel;
import playground.sergioo.visualizer2D2012.Painter;
import playground.sergioo.weeklySimulation.util.plans.TransitActsRemover;

public class WeeklyPlanPainter extends Painter {

	private enum WEEK_DAYS{
		MON,TUE,WED,THU,FRI,SAT,SUN;
	}
	private final Plan plan;
	private final boolean isHorizontal;
	private static final TransitActsRemover transitActsRemover = new TransitActsRemover();
	private final static Map<String, Color> TYPE_ACTIVITY_COLORS = new HashMap<String, Color>();
	{
		TYPE_ACTIVITY_COLORS.put("home", Color.LIGHT_GRAY);
		TYPE_ACTIVITY_COLORS.put("w", Color.DARK_GRAY);
		TYPE_ACTIVITY_COLORS.put("s", Color.GRAY);
		TYPE_ACTIVITY_COLORS.put("medi", Color.CYAN);
		
		TYPE_ACTIVITY_COLORS.put("sport", Color.GREEN);
		TYPE_ACTIVITY_COLORS.put("shop", Color.YELLOW);
		TYPE_ACTIVITY_COLORS.put("fun", Color.RED);
		TYPE_ACTIVITY_COLORS.put("biz", Color.BLUE);
		TYPE_ACTIVITY_COLORS.put("eat", Color.ORANGE);
		TYPE_ACTIVITY_COLORS.put("rec", Color.PINK);
		TYPE_ACTIVITY_COLORS.put("errand", Color.MAGENTA);
		TYPE_ACTIVITY_COLORS.put("social", Color.YELLOW.darker().darker());
	}
	private final static Map<String, Color> MODE_COLORS = new HashMap<String, Color>();
	{
		MODE_COLORS.put("car", Color.RED.darker().darker());
		MODE_COLORS.put("pt", Color.BLUE.darker().darker());
	}
	
	public WeeklyPlanPainter(Plan plan, boolean direction) {
		transitActsRemover.run(plan);
		this.plan = plan;
		this.isHorizontal = direction;
	}

	@Override
	public void paint(Graphics2D g2, LayersPanel layersPanel) {
		paintEdges(g2, layersPanel);
		double time = 0;
		for(PlanElement planElement:plan.getPlanElements()) {
			double time0 = time;
			if(planElement instanceof Activity)
				if(((Activity)planElement).getEndTime()!=Time.UNDEFINED_TIME)
					time = ((Activity)planElement).getEndTime();
				else if(((Activity)planElement).getMaximumDuration()!=Time.UNDEFINED_TIME)
					time += ((Activity)planElement).getMaximumDuration();
			if(planElement instanceof Leg)
				time += ((Leg)planElement).getTravelTime();
			paintPlanElement(planElement, time0, time, g2, layersPanel);
		}
	}

	private void paintPlanElement(PlanElement planElement, double startTime, double endTime, Graphics2D g2,
			LayersPanel layersPanel) {
		double x=0, y=0, width, height;
		if(isHorizontal) {
			x = (startTime%(24*3600))/3600;
			y = ((int)(startTime/(24*3600)))+0.5;
			height = 0.5;
			width = (endTime-startTime)/3600;
		}
		else {
			y = (endTime-startTime)/3600+(startTime%(24*3600))/3600;
			x = ((int)(startTime/(24*3600)));
			width = 0.5;
			height = (endTime-startTime)/3600;
		}
		Coord ldCorner = new Coord(x, y);
		if(planElement instanceof Activity) {
			Color color = TYPE_ACTIVITY_COLORS.get(((Activity)planElement).getType());
			if(color==null)
				color = TYPE_ACTIVITY_COLORS.get(((Activity)planElement).getType().substring(0, 1));
			if(((int)(endTime/(24*3600)))!=((int)(startTime/(24*3600))))
				if(isHorizontal) {
					double widthO = 24-ldCorner.getX();
					paintRectangle(g2, layersPanel, ldCorner, widthO, height, color);
					paintRectangle(g2, layersPanel, new Coord((double) 0, y + 1), width-widthO, height, color);
				}
				else {
					double heightO = ldCorner.getY()-24;
					paintRectangle(g2, layersPanel, new Coord(x, (double) 24), width, height-heightO, color);
					paintRectangle(g2, layersPanel, new Coord(x + 1, ldCorner.getY() - 24), width, heightO, color);
				}
			else
				paintRectangle(g2, layersPanel, ldCorner, width, height, color);
		}
		else if(!((Leg)planElement).getMode().equals("empty")) {
			Color color = MODE_COLORS.get(((Leg)planElement).getMode());
			if(((int)(endTime/(24*3600)))!=((int)(startTime/(24*3600))))
				if(isHorizontal) {
					double widthO = 24-ldCorner.getX();
					paintRectangleBorder(g2, layersPanel, ldCorner, widthO, height, color);
					paintRectangleBorder(g2, layersPanel, new Coord((double) 0, y + 1), width-widthO, height, color);
				}
				else {
					double heightO = 24-ldCorner.getY();
					paintRectangleBorder(g2, layersPanel, new Coord(x, (double) 24), width, heightO, color);
					paintRectangleBorder(g2, layersPanel, new Coord(x + 1, ldCorner.getY() - 24), width, height-heightO, color);
				}
			else
				paintRectangleBorder(g2, layersPanel, ldCorner, width, height, color);
		}
	}

	private void paintEdges(Graphics2D g2, LayersPanel layersPanel) {
		
	}

}
