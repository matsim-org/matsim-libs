package playground.dhosse.qgis.rendering;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.Range;

/**
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
