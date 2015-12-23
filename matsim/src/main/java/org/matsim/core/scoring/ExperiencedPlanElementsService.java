package org.matsim.core.scoring;

import com.google.common.annotations.Beta;

@Beta
public interface ExperiencedPlanElementsService {

	void register(Object subscriber);
	void unregister(Object subscriber);

}
