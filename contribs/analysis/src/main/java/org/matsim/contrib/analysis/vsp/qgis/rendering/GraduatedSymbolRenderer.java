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

	public GraduatedSymbolRenderer() {
		
		super(QGisConstants.renderingType.graduatedSymbol);
		
	}

	public abstract Range[] getRanges();

	public abstract String getRenderingAttribute();

	public abstract void setRenderingAttribute(String attr);

}
