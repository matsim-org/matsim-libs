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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
abstract class AbstractRenderer {

	// -------------------- CONSTANTS --------------------

	private final VisConfig visConfig;

	private final VisNetwork net;

	// -------------------- MEMBERS --------------------

	private NetJComponent component;

	private AbstractRenderer prev;

	// -------------------- CONSTRUCTION --------------------

	AbstractRenderer(final VisConfig visConfig, final VisNetwork net) {
		this.visConfig = visConfig;
		this.net = net;
		this.component = null;
		this.prev = null;
	}

	// -------------------- IMPLEMENTATION --------------------

	void append(final AbstractRenderer rendererBelow) {
		if (this.prev == null) {
			this.prev = rendererBelow;
		} else {
			this.prev.append(rendererBelow);
		}
	}

	void render(final Graphics2D display, final AffineTransform boxTransform) {
		if (this.prev != null) {
			this.prev.render(display, boxTransform);
		}
		if (this.component != null) {
			myRendering(display, boxTransform);
		}
	}

	void setComponent(final NetJComponent component) {
		if (this.prev != null) {
			this.prev.setComponent(component);
		}
		this.component = component;
	}

	NetJComponent getComponent() {
		return this.component;
	}

	VisConfig getVisConfig() {
		return visConfig;
	}

	VisNetwork getNetwork() {
		return this.net;
	}

	// -------------------- INTERFACE DEFINITION --------------------

	abstract void myRendering(final Graphics2D display,
			final AffineTransform boxTransform);

}
