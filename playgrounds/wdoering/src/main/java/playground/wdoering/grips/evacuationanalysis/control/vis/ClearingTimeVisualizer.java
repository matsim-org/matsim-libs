package playground.wdoering.grips.evacuationanalysis.control.vis;

import java.awt.Color;
import java.util.LinkedList;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;

import playground.wdoering.grips.evacuationanalysis.data.AttributeData;
import playground.wdoering.grips.evacuationanalysis.data.Cell;
import playground.wdoering.grips.evacuationanalysis.data.ColorationMode;
import playground.wdoering.grips.evacuationanalysis.data.EventData;

public class ClearingTimeVisualizer {
	
	public static AttributeData<Color> getVisualData(EventData data, ColorationMode colorationMode)
	{
		AttributeData<Color> coloration = new AttributeData<Color>();
		
		LinkedList<Cell> cells = getCells(data);
		
		for (Cell cell : cells)
		{
			double relClearingTime = cell.getClearingTime() / data.getMaxClearingTime();
			coloration.setAttribute(cell.getId(), Coloration.getColor(relClearingTime, colorationMode));
		}
		
		return coloration;
	}
	
	static LinkedList<Cell> getCells(EventData data)
	{
		LinkedList<Cell> cells = new LinkedList<Cell>();
		data.getCellTree().get(new Rect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), cells);
		return cells;
	}


	

}
