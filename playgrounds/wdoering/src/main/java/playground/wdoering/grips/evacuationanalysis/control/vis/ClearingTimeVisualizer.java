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
	
	private AttributeData<Color> coloration;
	private EventData data;
	private ColorationMode colorationMode;
	private float cellTransparency;
	
	public ClearingTimeVisualizer(EventData eventData, ColorationMode colorationMode, float cellTransparency)
	{
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
		coloration = new AttributeData<Color>();
		
		LinkedList<Cell> cells = data.getCells();
		
		for (Cell cell : cells)
		{
			double relClearingTime = cell.getClearingTime() / data.getMaxClearingTime();
			coloration.setAttribute(cell.getId(), Coloration.getColor(relClearingTime, colorationMode, cellTransparency));
		}
		
	}
	
	public AttributeData<Color> getColoration() {
		return coloration;
	}
	

}
