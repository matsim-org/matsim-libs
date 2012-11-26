package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.collections.QuadTree.Rect;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;
import playground.wdoering.grips.evacuationanalysis.data.AttributeData;
import playground.wdoering.grips.evacuationanalysis.data.Cell;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;
import playground.wdoering.grips.evacuationanalysis.data.EventData;

public class EvacuationTimeVisualizer {
	
	private AttributeData<Color> coloration;
	private EventData data;
	private ColorationMode colorationMode;
	private float cellTransparency;
	
	public EvacuationTimeVisualizer(EventData eventData, ColorationMode colorationMode, float cellTransparency)
	{
		this.data = eventData;
		this.cellTransparency = cellTransparency;
		this.colorationMode = colorationMode;
		processVisualData();
	}
	
	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}

	public void processVisualData()
	{
		this.coloration = new AttributeData<Color>();
		
		LinkedList<Cell> cells = data.getCells();
		
		List<Tuple<Id,Double>> cellTimes = new LinkedList<Tuple<Id,Double>>();
		
		for (Cell cell : cells)
		{
			double relTravelTime = cell.getTimeSum() / data.getMaxCellTimeSum();
			
			//might be NAN or less than zero: make it a zero
			if ((Double.isNaN(relTravelTime)) || (relTravelTime < 0))
				relTravelTime = 0d;
			else
			{
				cellTimes.add(new Tuple<Id,Double>(cell.getId(), cell.getTimeSum()));
			}
			
			coloration.setAttribute(cell.getId(), Coloration.getColor(relTravelTime, colorationMode, cellTransparency));
		}
		
		
//		//sort data
//		List<Tuple<Id,String>> colorClasses = new ArrayList<Tuple<Id,String>>();
//		Collections.sort(cellTimes, new Comparator<Tuple<Id,Double>>() {
//			@Override
//			public int compare(Tuple<Id, Double> o1, Tuple<Id, Double> o2)
//			{
//				if (o1.getSecond()>o2.getSecond())
//					return 1;
//				else if (o1.getSecond()<o2.getSecond())
//					return -1;
//				
//				return 0;
//			}
//		});
//		
//		for (Tuple<Id,Double> cellTime : cellTimes)
//			System.out.println(cellTime.getFirst() + ":" + cellTime.getSecond());
//		
//		this.data.setColorClasses(Mode.EVACUATION, colorClasses);
		
	}
	
	public AttributeData<Color> getColoration() {
		return coloration;
	}
	


}
