package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.collections.QuadTree.Rect;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;
import playground.wdoering.grips.evacuationanalysis.control.AverageClusterizer;
import playground.wdoering.grips.evacuationanalysis.control.Clusterizer;
import playground.wdoering.grips.evacuationanalysis.data.AttributeData;
import playground.wdoering.grips.evacuationanalysis.data.Cell;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;
import playground.wdoering.grips.evacuationanalysis.data.EventData;

public class EvacuationTimeVisualizer {
	
	private AttributeData<Color> coloration;
	private EventData data;
	private Clusterizer clusterizer;
	private int k;
	private ColorationMode colorationMode;
	private float cellTransparency;
	
	public EvacuationTimeVisualizer(EventData eventData, Clusterizer clusterizer, int k, ColorationMode colorationMode, float cellTransparency)
	{
		this.data = eventData;
		this.cellTransparency = cellTransparency;
		this.clusterizer = clusterizer;
		this.k = k;
		this.colorationMode = colorationMode;
		processVisualData();
	}
	
	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}

	public void processVisualData()
	{
		LinkedList<Tuple<Id, Double>> cellIdsAndTimes = new LinkedList<Tuple<Id,Double>>();
		LinkedList<Double> cellTimes = new LinkedList<Double>();
		this.coloration = new AttributeData<Color>();
		
		LinkedList<Cell> cells = data.getCells();
		
		for (Cell cell : cells)
		{
			
			if (!cellTimes.contains(cell.getTimeSum()))
			{
				cellTimes.add(cell.getTimeSum());
				cellIdsAndTimes.add(new Tuple<Id,Double>(cell.getId(), cell.getTimeSum()));
			}
			
//			coloration.setAttribute(cell.getId(), Coloration.getColor(relTravelTime, colorationMode, cellTransparency));
		}
		
		//calculate data clusters
		LinkedList<Tuple<Id,Double>> clusters = this.clusterizer.getClusters(cellIdsAndTimes, k);
		this.data.updateClusters(Mode.EVACUATION, clusters);
		
		LinkedList<Double> clusterValues = new LinkedList<Double>();
		for (Tuple<Id,Double> cluster: clusters)
		{
			clusterValues.add(cluster.getSecond());
			System.out.println("V:" + cluster.getSecond());
		}
		
		
//		for (Tuple<Id, Double> cellTime : cellTimes)
		for (Cell cell : cells)
		{
			double cellTimeSum = cell.getTimeSum();
			System.out.println(cellTimeSum);
//			double relTravelTime = cell.getTimeSum() / data.getMaxCellTimeSum();
			
//			//might be NAN or less than zero: make it a zero
//			if ((Double.isNaN(relTravelTime)) || (relTravelTime < 0))
//				relTravelTime = 0d;
			
			
			if (cellTimeSum < clusterValues.get(0))
			{
				coloration.setAttribute(cell.getId(), Coloration.getColor(0, colorationMode, cellTransparency));
				continue;
			}
			for (int i = 1; i < k; i++)
			{
				if ((cellTimeSum >= clusterValues.get(i-1)) && cellTimeSum < clusterValues.get(i))
				{
					float ik = (float)i/(float)k;
					System.out.println(ik);
					coloration.setAttribute(cell.getId(), Coloration.getColor(ik, colorationMode, cellTransparency));
					break;
				}
			}
			
			if (cellTimeSum>=clusterValues.get(k-1))
				coloration.setAttribute(cell.getId(), Coloration.getColor(1, colorationMode, cellTransparency));
			
//			coloration.setAttribute(, data)
//			System.out.println(cellTime.getFirst() + ":" + cellTime.getSecond());
		}
		
		
	}
	
	public AttributeData<Color> getColoration() {
		return coloration;
	}
	


}
