package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;
import playground.wdoering.grips.evacuationanalysis.control.Clusterizer;
import playground.wdoering.grips.evacuationanalysis.data.AttributeData;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;
import playground.wdoering.grips.evacuationanalysis.data.EventData;

public class UtilizationVisualizer {

	private AttributeData<Tuple<Float,Color>> coloration;
	private List<Link> links;
	private EventData data;
	private Clusterizer clusterizer;
	private int k;
	private ColorationMode colorationMode;
	private float cellTransparency;

	public UtilizationVisualizer(List<Link> links, EventData eventData, Clusterizer clusterizer, int k, ColorationMode colorationMode, float cellTransparency)
	{
		this.links = links;
		this.data = eventData;
		this.clusterizer = clusterizer;
		this.k = k;
		this.colorationMode = colorationMode;
		this.cellTransparency = cellTransparency;
		processVisualData();

	}
	
	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}
	

	public void processVisualData()
	{
		LinkedList<Tuple<Id,Double>> linkTimes = new LinkedList<Tuple<Id,Double>>(); 
				
		coloration = new AttributeData<Tuple<Float,Color>>();

		HashMap<Id, List<Tuple<Id,Double>>> linkLeaveTimes = data.getLinkLeaveTimes();
		HashMap<Id, List<Tuple<Id,Double>>> linkEnterTimes = data.getLinkEnterTimes();
		
		
		for (Link link : links)
		{
			List<Tuple<Id,Double>> leaveTimes = linkLeaveTimes.get(link.getId());
			List<Tuple<Id,Double>> enterTimes = linkEnterTimes.get(link.getId());
			
			if ((enterTimes != null) && (enterTimes.size() > 0) && (leaveTimes!=null))
			{
				
				if (!linkTimes.contains(enterTimes.size()))
				{
					linkTimes.add(new Tuple(link.getId(),(double)enterTimes.size()));
				}
//				float relUtilization = (((float)enterTimes.size()/(float)data.getMaxUtilization())); 
//				Tuple<Float, Color> currentColoration = new Tuple(relUtilization, Coloration.getColor(relUtilization, colorationMode, cellTransparency));
//				coloration.setAttribute((IdImpl)link.getId(), currentColoration);
				
			}
		}
		
		LinkedList<Tuple<Id,Double>> clusters = this.clusterizer.getClusters(linkTimes, k);
		
		//calculate data clusters
		this.data.updateClusters(Mode.UTILIZATION, clusters);
		
		
//		for (Tuple<Id,Double> cluster : clusters)
//			System.out.println("UTIL CL: " + cluster.getFirst() + ":" + cluster.getSecond());
		
		//assign clusterized colors to all link ids 
		for (Link link : links)
		{
			List<Tuple<Id,Double>> enterTimes = linkEnterTimes.get(link.getId());
			
			if ((enterTimes != null) && (enterTimes.size() > 0))
			{
				double enterTime = enterTimes.size();
				
				if (enterTime < clusters.get(0).getSecond())
				{
					coloration.setAttribute(link.getId(), new Tuple<Float,Color>(0f,Coloration.getColor(0, colorationMode, cellTransparency)));
					continue;
				}
				for (int i = 1; i < k; i++)
				{
					if ((enterTime >= clusters.get(i-1).getSecond()) && enterTime < clusters.get(i).getSecond())
					{
						float ik = (float)i/(float)k;
						coloration.setAttribute(link.getId(), new Tuple<Float,Color>(ik,Coloration.getColor(ik, colorationMode, cellTransparency)));
						break;
					}
				}
				if (enterTime>=clusters.get(k-1).getSecond())
					coloration.setAttribute(link.getId(), new Tuple<Float,Color>(1f,Coloration.getColor(1f, colorationMode, cellTransparency)));
			}
		}
		
	}

	public AttributeData<Tuple<Float,Color>> getColoration() {
		return this.coloration;
	}
	
	

}
