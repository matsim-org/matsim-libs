package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.collections.QuadTree.Rect;

import playground.wdoering.grips.evacuationanalysis.data.AttributeData;
import playground.wdoering.grips.evacuationanalysis.data.Cell;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;
import playground.wdoering.grips.evacuationanalysis.data.EventData;

public class UtilizationVisualizer {

	private AttributeData<Tuple<Float,Color>> coloration;
	private List<Link> links;
	private EventData data;
	private ColorationMode colorationMode;
	private float cellTransparency;

	public UtilizationVisualizer(List<Link> links, EventData eventData, ColorationMode colorationMode, float cellTransparency)
	{
		this.links = links;
		this.data = eventData;
		this.colorationMode = colorationMode;
		this.cellTransparency = cellTransparency;
		processVisualData();

	}
	
	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}
	

	public void processVisualData()
	{
		coloration = new AttributeData<Tuple<Float,Color>>();

		HashMap<Id, List<Tuple<Id,Double>>> linkLeaveTimes = data.getLinkLeaveTimes();
		HashMap<Id, List<Tuple<Id,Double>>> linkEnterTimes = data.getLinkEnterTimes();
		for (Link link : links)
		{
			List<Tuple<Id,Double>> leaveTimes = linkLeaveTimes.get(link.getId());
			List<Tuple<Id,Double>> enterTimes = linkEnterTimes.get(link.getId());
			
			if ((enterTimes != null) && (enterTimes.size() > 0) && (leaveTimes!=null))
			{
				
				float relUtilization = (((float)enterTimes.size()/(float)data.getMaxUtilization())); 
				
				Tuple<Float, Color> currentColoration = new Tuple(relUtilization, Coloration.getColor(relUtilization, colorationMode, cellTransparency));
				coloration.setAttribute((IdImpl)link.getId(), currentColoration);
				
			}
		}
		
	}

	public AttributeData<Tuple<Float,Color>> getColoration() {
		return this.coloration;
	}
	
	

}
