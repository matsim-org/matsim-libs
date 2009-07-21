package playground.mohit.pt;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.transitSchedule.api.TransitRouteStop;

import playground.mohit.pt.timeGraph.TransitLineRoute;

public class drawGraph extends Frame implements ItemListener{
	public Map<String , TransitLineRoute> lineRoutes = new TreeMap<String, TransitLineRoute>();
	private Choice listOfLineRoutes= new Choice() ;
	private String s;
	public drawGraph(Map<String , TransitLineRoute> lineRoutes){
		this.lineRoutes =  lineRoutes;
		setLayout(new BorderLayout());
		for (TransitLineRoute route : lineRoutes.values()){
			listOfLineRoutes.add(route.id.toString());
			
		}
		this.add(listOfLineRoutes, BorderLayout.NORTH);	
		listOfLineRoutes.addItemListener(this);
		
		
		setVisible(true);
		setSize(1000,1000);	
		
		
	}
	public void itemStateChanged(ItemEvent arg0) {
		s = listOfLineRoutes.getSelectedItem();
		  
		  repaint();
		// TODO Auto-generated method stub
		
	}
	public void paint(Graphics g){
		g.setColor(Color.red);
		g.drawLine(100,100,100,1200); // y-axis
		g.drawLine(100,100,1800,100);// x-axis
		g.setColor(Color.green);
		
		// Displaying the time on the Y-axis
		for (int i = 0; i < 50; i++) {
			
			g.drawString(Integer.toString(i*5),75,100+i*100);
		
		}
     g.setColor(Color.MAGENTA);
     g.drawString("STOPS",650,75);
     String str = "TIME in mins";
     // Displaying 'Stops' vertically
     int x=25; 
     int y=200;
     int v;
     v=g.getFontMetrics(getFont()).getHeight()+1;
     int j =0;
     int k= str.length();
     while(j < k+1) {
       if (j == k) 
          g.drawString(str.substring(j),x, y+(j*v));
       else
          g.drawString(str.substring(j,j+1),x, y+(j*v));
       j++;
       }
		
		
		
     for (TransitLineRoute route : lineRoutes.values()){
    	 j=0;
    	 
			if (s != null && s == route.id.toString()){
				int dep =0, arr= 0;
				for (TransitRouteStop stop : route.stops.values()){
					arr =  (int)(stop.getArrivalDelay()*20);
					
					if (j!=0){
							g.drawLine(100+(j-1)*60, 100+dep, 100+60*j, 100 + arr);
					}
					dep = (int)(stop.getDepartureDelay()*20);
					g.drawString(stop.getStopFacility().getId().toString(), 100+j*60, 90);
					g.drawLine(100+60*j,100+arr,100+60*j,100+dep);
					j++;
					
				}
				
			}
			
		}
		
	
			
	}

}
