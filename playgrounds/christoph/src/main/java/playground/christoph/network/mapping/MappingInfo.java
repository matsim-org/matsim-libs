package playground.christoph.network.mapping;

/*
 * Should be implemented by MappingNodes and MappingLinks.
 * The Mapping contains information about the Elements
 * Parents and Children.
 */
public interface MappingInfo {

	public void setDownMapping(Mapping mapping);
	
	public Mapping getDownMapping();
	
	public void setUpMapping(Mapping mapping);
	
	public Mapping getUpMapping();
}
