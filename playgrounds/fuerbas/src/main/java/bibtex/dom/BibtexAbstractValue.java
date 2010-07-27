/*
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

/**
 * Abstract values are the values that can be used as field values in
 * entries or as bodies of macros.
 * 
 * For example, in 
 * <pre>
 * 	&#x0040;string(pldi = acm # " SIGPLAN Conference of Programming
 * 						Language Design and Implementation")
 * </pre>
 * the following is an abstract value.
 * <pre>
 * acm # " SIGPLAN Conference of Programming Language Design and Implementation"
 * </pre>
 * Other examples:
 * <ul>
 * <li>1971</li>
 * <li>"Johannes Henkel"</li>
 * <li>acm</li>
 * <li>dec</li>
 * </ul> 
 *  
 * @author henkel
 */
public abstract class BibtexAbstractValue extends BibtexNode {



	/**
	 * @param bibtexFile
	 */
	protected BibtexAbstractValue(BibtexFile bibtexFile) {
		super(bibtexFile);
	}

}
