/*
 * Created on Mar 29, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package bibtex.expansions;

import bibtex.dom.BibtexFile;

/**
 * An expander is a transformer that makes a bibtex model more elaborate.
 * 
 * @author henkel
 */
public interface Expander {

	public void expand(BibtexFile file) throws ExpansionException;
	
	public void expand(BibtexFile file1, BibtexFile file2) throws ExpansionException;

	/**
	 * @return this method returns all exceptions that have been accumulated in
	 *         the last call to expand. Whether or not expand accumulates
	 *         exceptions depends on the configuration of the expander, which
	 *         is usually specified in the constructor call by setting the flag
	 *         throwAllExpansionExceptions. Oviously, implementers of this
	 *         interface should provide this flag as a parameter to the
	 *         constructor. Hint: Extend AbstractExpander.
	 */
	public ExpansionException[] getExceptions();
}
