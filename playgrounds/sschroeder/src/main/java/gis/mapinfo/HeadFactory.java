/**
 * 
 */
package gis.mapinfo;

/**
 * @author stefan
 *
 */
public class HeadFactory {
	public String createDefaultHead(){
		String head = "Version 300" + getEndOfLine();
		head += "Charset \"WindowsLatin1\"" + getEndOfLine();
		head += "Delimiter \",\"" + getEndOfLine();
		head += "CoordSys \"LŠnge / Breite\", 1, 0" + getEndOfLine();
		return head;
	}

	private String getEndOfLine() {
		return "\n";
	}
}
