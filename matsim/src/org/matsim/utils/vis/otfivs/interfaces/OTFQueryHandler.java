package org.matsim.utils.vis.otfivs.interfaces;

public interface OTFQueryHandler {
	public void addQuery(OTFQuery query);
	public void removeQueries();
	public void handleIdQuery(String id, String query) ;
	public OTFQuery handleQuery(OTFQuery query);

}
