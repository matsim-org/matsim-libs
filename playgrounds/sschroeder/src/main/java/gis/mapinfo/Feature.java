package gis.mapinfo;

public class Feature{
	
	private FeatureGeo featureGeo;
	
	private FeatureData featureData;
	
	private FeatureLayout featureLayout;

	public Feature(FeatureGeo featureGeo, FeatureData featureData,
			FeatureLayout featureLayout) {
		super();
		this.featureGeo = featureGeo;
		this.featureData = featureData;
		this.featureLayout = featureLayout;
	}

	public FeatureGeo getFeatureGeo() {
		return featureGeo;
	}

	public FeatureData getFeatureData() {
		return featureData;
	}

	public FeatureLayout getFeatureLayout() {
		return featureLayout;
	}
}