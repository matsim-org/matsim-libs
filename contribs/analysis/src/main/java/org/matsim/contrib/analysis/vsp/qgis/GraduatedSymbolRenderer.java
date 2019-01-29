package org.matsim.contrib.analysis.vsp.qgis;


import org.matsim.contrib.analysis.vsp.qgis.utils.ColorRangeUtils;

import java.awt.*;

/**
 * Renderer that draws graduated symbols.
 * </p>
 * This type of renderer needs a rendering attribute (to classify the symbols),
 * ranges (from which to which value a symbol is drawn in a specific way) and at least
 * two symbol layers.
 * 
 * @author dhosse, gthunig
 *
 */
public class GraduatedSymbolRenderer extends QGisRenderer {

	private String renderingAttribute;
	private boolean useHeader;
	private String fileHeader;

    private Range[] ranges;
    private double lowerBound;
    private double upperBound;
    private int range;
    private int symbolSize;
    private ColorRangeUtils.ColorRange colorRange;

    public GraduatedSymbolRenderer(VectorLayer layer, Double lowerBound, Double upperBound,
                                 Integer range, int symbolSize) {
        this(layer, lowerBound, upperBound, range, symbolSize, ColorRangeUtils.ColorRange.RED_TO_GREEN);
    }

    public GraduatedSymbolRenderer(VectorLayer layer, Double lowerBound, Double upperBound,
                                   Integer range, int symbolSize, ColorRangeUtils.ColorRange colorRange) {
        super(QGisConstants.renderingType.graduatedSymbol, layer);
        this.fileHeader = layer.getHeader();
        this.useHeader = fileHeader != null;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.range = range;
        this.symbolSize = symbolSize;
        this.colorRange = colorRange;
        init();
    }
	
	public Range[] getRanges() {
	    return ranges;
    }

	String getRenderingAttribute(){
		return this.renderingAttribute;
	}

	public void setRenderingAttribute(String attr){
		if(this.useHeader){
			if(this.fileHeader.contains(attr)){
				this.renderingAttribute = attr;
			} else{
				throw new RuntimeException("Rendering attribute " + attr + " does not exist in header!");
			}
		} else{
			throw new RuntimeException("The input file for this renderer has no header. Use method \"setRenderingAttribute(int columnIndex)\" instead!");
		}
	}
	
	void setRenderingAttribute(int columnIndex){
		if(!this.useHeader){
			this.renderingAttribute = "field_" + Integer.toString(columnIndex);
		} else{
			throw new RuntimeException("The input file for this renderer has a header. Use method \"setRenderingAttribute(String attr)\" instead!");
		}
	}

    @Override
    public void init() {
        double spread = this.upperBound - this.lowerBound;
        double stepSize = spread / (this.range - 2);

        this.ranges = new Range[this.range];
        this.ranges[0] = createFirstRange();

        for (int i = 0; i < range - 2; i++) {
            Double lowerBoundary = this.lowerBound + i * stepSize;
            Double upperBoundary = this.lowerBound + (i+1) * stepSize;
            this.ranges[i+1] = new Range(lowerBoundary, upperBoundary, lowerBoundary.toString() + " - " + upperBoundary.toString());
        }

        if (range > 1)
            this.ranges[this.range - 1] = createLastRange();

        createQGisPointSymbolLayers();
    }

    private Range createFirstRange() {
	    Range firstRange = new Range("Smaller than " + String.valueOf(lowerBound));
	    firstRange.setLowerBound(-100);
	    firstRange.setUpperBound(lowerBound);
	    return firstRange;
    }

    private Range createLastRange() {
        Range lastRange = new Range("Greater than " + String.valueOf(lowerBound));
        lastRange.setLowerBound(upperBound);
        lastRange.setUpperBound(100);
        return lastRange;
    }

    private void createQGisPointSymbolLayers() {

        double colorRangeStep = 1.0 / (double)(range-1);
        double sizeMapUnitScale[] = {0,0};

        for (int i = 0; i < range; i++) {
            QGisPointSymbolLayer currentLevel = new QGisPointSymbolLayer();
            currentLevel.setId(i);
            currentLevel.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*i));
            currentLevel.setColorBorder(new Color(0,0,0,255));
            currentLevel.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
            currentLevel.setSize(symbolSize);
            currentLevel.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
            currentLevel.setSizeMapUnitScale(sizeMapUnitScale);
            currentLevel.setPenStyle(QGisConstants.penstyle.no);
            currentLevel.setLayerTransparency(1);
            this.addSymbolLayer(currentLevel);
        }
    }

}
