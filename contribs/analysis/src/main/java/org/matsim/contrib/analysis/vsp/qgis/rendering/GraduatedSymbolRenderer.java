package org.matsim.contrib.analysis.vsp.qgis.rendering;

import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.Range;

/**
 * Template class for a renderer that draws graduated symbols.
 * </p>
 * This type of renderer needs a rendering attribute (to classify the symbols),
 * ranges (from which to which value a symbol is drawn in a specific way) and at least
 * two symbol layers.
 * 
 * @author dhosse
 *
 */
public abstract class GraduatedSymbolRenderer extends QGisRenderer {

	private String renderingAttribute;
	private boolean useHeader;
	
	public GraduatedSymbolRenderer(boolean useHeader) {
		
		super(QGisConstants.renderingType.graduatedSymbol);
		this.useHeader = useHeader;
		
	}

	public abstract Range[] getRanges();

	public String getRenderingAttribute(){
		
		return this.renderingAttribute;
		
	}

	public void setRenderingAttribute(String attr){
		
		if(this.useHeader){
			
			this.renderingAttribute = attr;
			
		} else{
			
			throw new RuntimeException("The input file for this renderer has no header. Use method \"setRenderingAttribute(int columnIndex)\" instead!");
			
		}
		
	}
	
	public void setRenderingAttribute(int columnIndex){
		
		if(!this.useHeader){
			
			this.renderingAttribute = "field_" + Integer.toString(columnIndex);
			
		} else{
			
			throw new RuntimeException("The input file for this renderer has a header. Use method \"setRenderingAttribute(String attr)\" instead!");
			
		}
		
	}

	public boolean isUseHeader() {
		return useHeader;
	}

}
