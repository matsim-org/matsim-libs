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
		LinkedList<Tuple<Id, Double>> cellTimes = new LinkedList<Tuple<Id,Double>>();
		
		coloration = new AttributeData<Color>();
		
		LinkedList<Cell> cells = data.getCells();
		
		for (Cell cell : cells)
		{
			double relClearingTime = cell.getClearingTime() / data.getMaxClearingTime();
			cellTimes.add(new Tuple<Id,Double>(cell.getId(), cell.getClearingTime()));
			
			coloration.setAttribute(cell.getId(), Coloration.getColor(relClearingTime, colorationMode, cellTransparency));
		}
		
		//calculate data clusters
		this.data.updateClusters(Mode.CLEARING, clusterizer.getClusters(cellTimes, k));
		
		
	}
	
	public AttributeData<Color> getColoration() {
		return coloration;
	}
	

}
