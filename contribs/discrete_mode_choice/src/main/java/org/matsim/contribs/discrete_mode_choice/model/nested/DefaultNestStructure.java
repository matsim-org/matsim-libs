package org.matsim.contribs.discrete_mode_choice.model.nested;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class DefaultNestStructure implements NestStructure {
	private final Map<Nest, Nest> parents = new HashMap<>();
	private final Map<Nest, Collection<Nest>> children = new HashMap<>();
	private final Collection<Nest> nests = new LinkedList<>();

	private final Nest root = new DefaultNest("ROOT", 1.0);

	public DefaultNestStructure() {
		children.put(root, new HashSet<>());
		nests.add(root);
	}

	@Override
	public Nest getRoot() {
		return root;
	}

	@Override
	public Collection<Nest> getChildren(Nest nest) {
		return children.get(nest);
	}

	@Override
	public Nest getParent(Nest nest) {
		return parents.get(nest);
	}

	@Override
	public Collection<Nest> getNests() {
		return nests;
	}

	public void addNest(Nest parent, Nest child) {
		if (!children.containsKey(parent)) {
			throw new IllegalStateException(String.format("Nest '%s' does not exist yet", parent.getName()));
		}

		if (nests.contains(child)) {
			throw new IllegalStateException(String.format("Nest '%s' was already added", child.getName()));
		}

		children.get(parent).add(child);
		parents.put(child, parent);
		children.put(child, new HashSet<>());
		nests.add(child);
	}
}