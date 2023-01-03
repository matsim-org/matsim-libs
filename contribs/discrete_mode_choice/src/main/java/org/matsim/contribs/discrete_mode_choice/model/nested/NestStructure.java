package org.matsim.contribs.discrete_mode_choice.model.nested;

import java.util.Collection;

public interface NestStructure {
	Nest getRoot();

	Collection<Nest> getChildren(Nest nest);

	Nest getParent(Nest nest);

	Collection<Nest> getNests();
}