/*
 * A BibtexMacroReference references a BibtexMacroDefinition
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;

/**
 * A BibtexMacroReference references a BibtexMacroDefinition.
 * 
 * @author henkel
 */
public final class BibtexMacroReference extends BibtexAbstractValue {

	BibtexMacroReference(BibtexFile file,String key){
		super(file);
		this.key = key.toLowerCase();
	}

	private String key;

	/**
	 * @return String
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the key.
	 * @param key The key to set
	 */
	public void setKey(String key) {
	    
	    assert key!=null: "key paramter may not be null.";
	    
		this.key = key;
	}

	/* (non-Javadoc)
	 * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
	 */
	public void printBibtex(PrintWriter writer) {
	    
	    assert writer!=null:"writer parameter may not be null.";
	    
		writer.print(this.key);
	}

}
