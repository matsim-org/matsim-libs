/**
 * 
 */
package playground.qiuhan.sa;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.yu.utils.io.SimpleReader;

/**
 * read zone information from a small zone file (4 columns ID\tx\ty\ttypeNb)
 * 
 * @author Q. SUN
 * 
 */
public class ZoneReader {
	private SimpleReader reader;
	private Map<String/* bezirk ID */, Integer/* type number */> zoneIdTypes;
	private Map<String/* bezirk ID */, Coord> zoneIdCoords;

	/**
	 * 
	 */
	public ZoneReader() {
		this.zoneIdCoords = new HashMap<String, Coord>();
		this.zoneIdTypes = new HashMap<String, Integer>();
	}

	public Map<String, Integer> getZoneIdTypes() {
		return zoneIdTypes;
	}

	public Map<String, Coord> getZoneIdCoords() {
		return zoneIdCoords;
	}

	public void readFile(String filename) {
		this.reader = new SimpleReader(filename);
		String line = this.reader.readLine();
		while (line != null) {
			line = this.reader.readLine();
			if (line != null) {
				String[] words = line.split("\t");
				String id = words[0];

				Coord coord = new CoordImpl(words[1], words[2]);
				zoneIdCoords.put(id, coord);

				int typeNb = Integer.parseInt(words[3]);
				zoneIdTypes.put(id, typeNb);
			}
		}
	}
}
