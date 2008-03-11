package playground.david.vis.interfaces;

public interface OTFQueryHandler {
	public void addQuery(OTFQuery query);
	public void removeQueries();
	public void handleIdQuery(String id, String query) ;
}
