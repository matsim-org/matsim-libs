/**
 * 
 */
package gis.mapinfo;

/**
 * @author stefan
 *
 */
public class ProjectionFactory {
	
	public Projection createDefaultLongitudeLatitude(){
		return new Projection("\"LŠnge / Breite\", 1, 0");
	}

}
