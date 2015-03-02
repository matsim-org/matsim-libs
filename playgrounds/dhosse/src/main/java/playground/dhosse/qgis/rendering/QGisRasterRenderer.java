package playground.dhosse.qgis.rendering;

import playground.dhosse.qgis.QGisConstants;

/**
 * Renderer for QGis raster layers.
 * So far, no project or use case specific settings exist, so this is the only class that renders raster data.
 * 
 * @author dhosse
 *
 */
public class QGisRasterRenderer extends QGisRenderer {
	
	private double opacity;
	
	private int alphaBand;
	private int blueBand;
	private int greenBand;
	private int redBand;
	
	private QGisConstants.rasterRendererType type;
	
	public QGisRasterRenderer(double opacity, int alpha, int blue, int green, QGisConstants.rasterRendererType type, int red){
		
		super(QGisConstants.renderingType.singleSymbol);
		
		this.opacity = opacity;
		this.alphaBand = alpha;
		this.blueBand = blue;
		this.greenBand = green;
		this.type = type;
		this.redBand = red;
		
	}
	
	public double getOpacity() {
		return opacity;
	}
	public void setOpacity(double opacity) {
		this.opacity = opacity;
	}
	public int getAlphaBand() {
		return alphaBand;
	}
	public void setAlphaBand(int alphaBand) {
		this.alphaBand = alphaBand;
	}
	public int getBlueBand() {
		return blueBand;
	}
	public void setBlueBand(int blueBand) {
		this.blueBand = blueBand;
	}
	public int getGreenBand() {
		return greenBand;
	}
	public void setGreenBand(int greenBand) {
		this.greenBand = greenBand;
	}
	public int getRedBand() {
		return redBand;
	}
	public void setRedBand(int redBand) {
		this.redBand = redBand;
	}
	public QGisConstants.rasterRendererType getType() {
		return type;
	}
	public void setType(QGisConstants.rasterRendererType type) {
		this.type = type;
	}

}
