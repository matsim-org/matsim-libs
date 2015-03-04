package org.matsim.contrib.analysis.vsp;

import org.matsim.api.core.v01.Id;

public class VectorJoin {

	private final String joinFieldName;
	private final String targetFieldName;
	private final Id<QGisLayer> joinLayerId;
	
	public VectorJoin(Id<QGisLayer> joinLayerId, String joinFieldName, String targetFieldName){
		
		this.joinFieldName = joinFieldName;
		this.targetFieldName = targetFieldName;
		this.joinLayerId = joinLayerId;
		
	}

	public String getJoinFieldName() {
		return this.joinFieldName;
	}

	public String getTargetFieldName() {
		return this.targetFieldName;
	}

	public Id<QGisLayer> getJoinLayerId() {
		return this.joinLayerId;
	}
	
}
