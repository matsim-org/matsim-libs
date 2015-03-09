package org.matsim.contrib.analysis.vsp.qgis;


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
	private String fileHeader;
	
	public GraduatedSymbolRenderer(String header, QGisLayer layer) {
		
		super(QGisConstants.renderingType.graduatedSymbol, layer);
		this.useHeader = header != null ? true : false;
		this.fileHeader = header;
		
	}
	
	public abstract Range[] getRanges();

	public String getRenderingAttribute(){
		
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
	
	public void setRenderingAttribute(int columnIndex){
		
		if(!this.useHeader){
			
			this.renderingAttribute = "field_" + Integer.toString(columnIndex);
			
		} else{
			
			throw new RuntimeException("The input file for this renderer has a header. Use method \"setRenderingAttribute(String attr)\" instead!");
			
		}
		
	}

}
