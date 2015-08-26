/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import org.xml.sax.helpers.DefaultHandler;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * TODO This class should dissappear in favor of something that is more
 * consistent with the <code>utilities.config</code> package.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class VisConfig extends DefaultHandler {

	// -------------------- MEMBERS --------------------

	private String logo = "";

	private int delay_ms = 1000;

	private int linkWidthFactor = 1;

	private boolean showNodeLabels = false;

	private boolean showLinkLabels = false;

	private boolean useAntiAliasing = false;

	private String colorDef = "BLUE -1 WHITE 0 GREEN 0.1 YELLOW 0.3 RED 1";

	private String linkDataFile = null;

	private boolean multiLane = false;

	// -------------------- CONSTRUCTION --------------------

	public VisConfig() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setLogo(final String logo) {
		this.logo = logo;
	}

	public String getLogo() {
		return this.logo;
	}

	public void setDelay_ms(final int delay_ms) {
		this.delay_ms = delay_ms;
	}

	public int getDelay_ms() {
		return this.delay_ms;
	}

	public void setLinkWidthFactor(final int linkWidthFactor) {
		this.linkWidthFactor = linkWidthFactor;
	}

	public int getLinkWidthFactor() {
		return this.linkWidthFactor;
	}

	public void setShowNodeLabels(final boolean showNodeLabels) {
		this.showNodeLabels = showNodeLabels;
	}

	public boolean getShowNodeLabels() {
		return this.showNodeLabels;
	}

	public void setShowLinkLabels(final boolean showLinkLabels) {
		this.showLinkLabels = showLinkLabels;
	}

	public boolean getShowLinkLabels() {
		return this.showLinkLabels;
	}

	public void setUseAntiAliasing(final boolean useAntiAliasing) {
		this.useAntiAliasing = useAntiAliasing;
	}

	public boolean getUseAntiAliasing() {
		return this.useAntiAliasing;
	}

	public void setColorDef(final String colorDef) {
		this.colorDef = colorDef;
	}

	public String getColorDef() {
		return this.colorDef;
	}

	public void setLinkDataFile(final String linkDataFile) {
		this.linkDataFile = linkDataFile;
	}

	public String getLinkDataFile() {
		return this.linkDataFile;
	}

	public void setMultiLane(final boolean multiLane) {
		this.multiLane = multiLane;
	}

	public boolean getMultiLane() {
		return this.multiLane;
	}
}
