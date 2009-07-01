package playground.mohit.pt;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.misc.Time;

import playground.marcel.pt.analysis.TransitRouteAccessEgressAnalysis;
import playground.marcel.pt.demo.AccessEgressDemo;
import playground.marcel.pt.transitSchedule.api.Departure;
import playground.marcel.pt.transitSchedule.api.TransitRouteStop;
public class agentGraph extends Frame implements ActionListener{
	AccessEgressDemo a;
	TransitRouteAccessEgressAnalysis b;
	Map<Id,Integer> graph = new LinkedHashMap<Id,Integer>();
	private Panel p1;	
	private Button[] buttons ;
	private String s = "Choose Departure";
	public agentGraph(AccessEgressDemo a,TransitRouteAccessEgressAnalysis b)
	{
		this.a=a;
		this.b=b;
		buttons = new Button[b.headings.size()];
		setLayout(new BorderLayout());
		p1 = new Panel(); 
		p1.setLayout(new GridLayout());
		int u=0;
		for (Departure departure : b.headings.values()){
			
			buttons[u]=new Button(Time.writeTime(departure.getDepartureTime()));
			p1.add(buttons[u]);
			buttons[u].addActionListener(this);
		u++;
		}
		
		add(p1,BorderLayout.SOUTH);
		
		
		setVisible(true);
		setSize(1000,1000);			
	}
	  public void actionPerformed(ActionEvent ae)
		 {
		  s = ae.getActionCommand();
		  repaint();
		 }
	public void paint(Graphics g)
	{
		g.setColor(Color.red);
		g.drawLine(100,70,100,500); // y-axis
		g.drawLine(100,500,a.ids.length*50+100,500);// x-axis
		g.setColor(Color.green);
		//  Displaying stopIds on the X-axis
		for (int i = 0; i < a.ids.length; i++) {
			
			g.drawString(a.ids[i].toString(),100+50*i,530);
		
		}
		// Displaying the No. of Agents on the Y-axis
		for (int i = 0; i < 11; i++) {
			
			g.drawString(Integer.toString(i*10),60,505-i*40);
		
		}
     g.setColor(Color.MAGENTA);
     g.drawString("Stop IDs",(100+a.ids.length*50)/2-50,570);
     String str = "No Of Agents";
     // Displaying 'No of Agents' vertically
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
     
     graph();
     g.drawString(s,50,50);
     
     //  Drawing the Graph
	int m=0;
    int n=0;
	for (int q: graph.values()){    	 
		g.setColor(Color.blue);
		g.drawLine(100+m*50,500-q*4,100+(m+1)*50,500-q*4);
		g.drawLine(100+m*50, 500-n*4,100+m*50, 500-q*4);
		m++;
		n=q;
     }
     
     
    }
	public void graph(){
		List<Id> stopFacilityIds = new ArrayList<Id>(b.transitRoute.getStops().size());
		for (TransitRouteStop stop : b.transitRoute.getStops()) {
			stopFacilityIds.add(stop.getStopFacility().getId());
		}

		for (Departure departure : b.headings.values()){
			if (Time.writeTime(departure.getDepartureTime()).equals(s)){
					Map<Id, Integer> accessCounter = b.getAccessCounter(departure);
					Map<Id, Integer> egressCounter = b.getEgressCounter(departure);
					Integer value1=0,value2=0;					
					for (Id id : stopFacilityIds) {
							int f,h;
							if (accessCounter.get(id)==null)
								f=0;
							else f=accessCounter.get(id);
							if (egressCounter.get(id)==null)
								h=0;
							else h=egressCounter.get(id);
							value1 = value1 + f;
							value2 = value2 + h;
							graph.put(id, value1-value2);
					
					}
		
		
		
  
			}
	
	
		}
	}
}
	

