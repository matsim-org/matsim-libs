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
package floetteroed.utilities.networks.construction;

import java.util.Set;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class AbstractLink<N extends AbstractNode<N, L>, L extends AbstractLink<N, L>>
		extends AttributeContainer {

	// -------------------- MEMBER VARIABLES --------------------

	private N fromNode = null;

	private N toNode = null;

	// -------------------- CONSTRUCTION --------------------

	public AbstractLink(final String id) {
		super(id);
	}

	// BasicLink(final BasicLink parent) {
	// super(parent);
	// }

	// -------------------- CONTENT WRITING --------------------

	public void setFromNode(final N node) {
		this.fromNode = node;
	}

	public void setToNode(final N node) {
		this.toNode = node;
	}

	// -------------------- CONTENT READING --------------------

	public N getFromNode() {
		return fromNode;
	}

	public N getToNode() {
		return toNode;
	}

	public Set<L> getInLinks() {
		if (this.fromNode == null) {
			return null;
		} else {
			return this.fromNode.getInLinks();
		}
	}

	public Set<L> getOutLinks() {
		if (this.toNode == null) {
			return null;
		} else {
			return this.toNode.getOutLinks();
		}
	}

	// -------------------- ADDITIONAL FUNCTIONALITY --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer(this.getClass()
				.getSimpleName());
		result.append("(id = ");
		result.append(this.getId());
		result.append(", fromNode = ");
		result.append(this.getFromNode() == null ? "null" : this.getFromNode()
				.getId());
		result.append(", toNode = ");
		result.append(this.getToNode() == null ? "null" : this.getToNode()
				.getId());
		result.append(", super = ");
		result.append(super.toString());
		result.append(")");
		return result.toString();
	}
}
