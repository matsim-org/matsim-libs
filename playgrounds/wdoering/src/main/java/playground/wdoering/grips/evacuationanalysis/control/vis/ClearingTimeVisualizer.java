package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.util.LinkedList;

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

public class ClearingTimeVisualizer {
	
	private AttributeData<Color> coloration;
	private EventData data;
	private Clusterizer clusterizer;
	private int k;
	private ColorationMode colorationMode;
	private float cellTransparency;
	
	public ClearingTimeVisualizer(EventData eventData, Clusterizer clusterizer, int k, ColorationMode colorationMode, float cellTransparency)
	{
		this.data = eventData;
		this.colorationMode = colorationMode;
		this.clusterizer = clusterizer;		
		this.k = k;
		this.cellTransparency = cellTransparency;
		processVisualData();

	}
	
	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}
	
	public void processVisualData()
	{
		LinkedList<Tuple<Id, Double>> cellIdsAndTimes = new LinkedList<Tuple<Id,Double>>();
		LinkedList<Double> cellTimes = new LinkedList<Double>();

		//create new coloration (id <-> color relation)
		coloration = new AttributeData<Color>();
		
		//get cells
		LinkedList<Cell> cells = data.getCells();
		for (Cell cell : cells)
		{
//			System.out.println("cellid:" + cell.getId());
			if (!cellTimes.contains(cell.getClearingTime()))
			{
				cellTimes.add(cell.getClearingTime());
				cellIdsAndTimes.add(new Tuple<Id,Double>(cell.getId(), cell.getClearingTime()));
			}
			
//			double relClearingTime = cell.getClearingTime() / data.getMaxClearingTime();
//			cellIdsAndTimes.add(new Tuple<Id,Double>(cell.getId(), cell.getClearingTime()));
//			coloration.setAttribute(cell.getId(), Coloration.getColor(relClearingTime, colorationMode, cellTransparency));
		}
		
//		for (int i = 0; i < cellTimes.size(); i++)
//		{
//			System.out.println("cellTime:" + cellTimes.get(i) + " | celltimeandid: " + cellIdsAndTimes.get(i).getSecond() + "(" + cellIdsAndTimes.get(i).getFirst()+")");
//		}

		//calculate data clusters
//		System.out.println("ci:"+cellIdsAndTimes.size());
		LinkedList<Tuple<Id,Double>> clusters = this.clusterizer.getClusters(cellIdsAndTimes, k);
		this.data.updateClusters(Mode.CLEARING, clusters);
		
//		for (Tuple<Id,Double> cluster : clusters)
//			System.out.println(cluster.getFirst() + ":" + cluster.getSecond());
		
		for (Cell cell : cells)
		{
			double clearingTime = cell.getClearingTime();
			
			if (clearingTime < clusters.get(0).getSecond())
			{
				coloration.setAttribute(cell.getId(), Coloration.getColor(0, colorationMode, cellTransparency));
				continue;
			}
			for (int i = 1; i < k; i++)
			{
				if ((clearingTime >= clusters.get(i-1).getSecond()) && clearingTime < clusters.get(i).getSecond())
				{
					float ik = (float)i/(float)k;
					coloration.setAttribute(cell.getId(), Coloration.getColor(ik, colorationMode, cellTransparency));
					break;
				}
			}
			if (clearingTime>=clusters.get(k-1).getSecond())
				coloration.setAttribute(cell.getId(), Coloration.getColor(1, colorationMode, cellTransparency));
			
		}
		
		
	}
	
	public AttributeData<Color> getColoration() {
		return coloration;
	}
	

}
