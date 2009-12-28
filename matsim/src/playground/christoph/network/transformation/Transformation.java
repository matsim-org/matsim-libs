package playground.christoph.network.transformation;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/*
 * Modifies a given Network. Typically this is used to
 * create a simplified version of the Network.
 */
public interface Transformation {

	/*
	 * Find all Structures that can be modifies with
	 * the Module. Typically this will be a List of
	 * Links and / or Nodes.
	 */
	public void findTransformableStructures();
	
	/*
	 * Select those Structures that are really gonna be transformed.
	 * If a Structure is transformed, maybe some other can't be
	 * transformed anymore. This Method selects those Structures
	 * that can be transformed in a Transformation Operation.
	 */
	public void selectTransformableStructures();
	
	/*
	 * Returns the transformable Structures. Some Elements
	 * may be remove from the Map because due to different
	 * reasons some of them should not be transformed.
	 * 
	 * A typical returned Map will look like Map<Id, Node>
	 * or Map<Id, Link>.
	 */
	public Map<Id, ?> getTransformableStructures();
	
	/*
	 * Transform all Elements that are contained in the Map.
	 */
	public void doTransformation();
}
