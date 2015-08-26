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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class AbstractNode<N extends AbstractNode<N, L>, L extends AbstractLink<N, L>>
		extends AttributeContainer {

	// -------------------- MEMBER VARIABLES --------------------

	private final Set<L> inLinks = new LinkedHashSet<L>();

	private final Set<L> outLinks = new LinkedHashSet<L>();

	// -------------------- CONSTRUCTION --------------------

	public AbstractNode(final String id) {
		super(id);
	}

	// BasicNode(final BasicNode parent) {
	// super(parent);
	// }

	// -------------------- CONTENT WRITING --------------------

	public boolean addInLink(final L link) {
		return inLinks.add(link);
	}

	public boolean addOutLink(final L link) {
		return outLinks.add(link);
	}

	// -------------------- CONTENT READING --------------------

	public Set<L> getInLinks() {
		return inLinks;
	}

	public Set<L> getOutLinks() {
		return outLinks;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer(this.getClass()
				.getSimpleName());
		result.append("(id = ");
		result.append(this.getId());
		result.append(", #inLinks = ");
		result.append(this.getInLinks().size());
		result.append(", #outLinks = ");
		result.append(this.getOutLinks().size());
		result.append(", super = ");
		result.append(super.toString());
		result.append(")");
		return result.toString();
	}
}
