/*
 * Created on Jul 14, 2004
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.parser;

/**
 * These constants determine how the parser deals with multiple values for the
 * same field. For example, consider the following bibtex entry, which has
 * multiple (2) values for the url field.
 * 
 * <pre>
 * 
 *          &#064;inproceedings{diwan98typebased,
 *            year=1998,
 *            pages={106-117},
 *            title={Type-Based Alias Analysis},
 *            url={citeseer.nj.nec.com/diwan98typebased.html},
 *            booktitle={SIGPLAN Conference on Programming Language Design and Implementation},
 *            author={Amer Diwan and Kathryn S. McKinley and J. Eliot B. Moss},
 *            url={http://www-plan.cs.colorado.edu/diwan/},
 *         }
 *  
 * </pre>
 * 
 * 
 * @author henkel
 */
public class BibtexMultipleFieldValuesPolicy {

    private BibtexMultipleFieldValuesPolicy() {
    }

    /**
     * If a field in a bibtex entry has multiple values, then keep the first
     * value and ignore the other values - this is what bibtex does, so it's the
     * default. For the example above, this means the parser will use
     * 
     * <pre>
     * 
     *  
     *   
     *     {citeseer.nj.nec.com/diwan98typebased.html}
     *    
     *   
     *  
     * </pre>.
     */
    public static final int KEEP_FIRST = 0;
    
    
    /**
     * If a field in a bibtex entry has multiple values, then keep the last
     * value and ignore the other values. For the example above, this means the
     * parser will use
     * 
     * <pre>
     * 
     *  
     *   
     *     {http://www-plan.cs.colorado.edu/diwan/}
     *    
     *   
     *  
     * </pre>.
     */
    public static final int KEEP_LAST = 1;


    /**
     * If a field in a bibtex entry has multiple values, then keep all of them.
     * In this case, we'll use instances of BibtexMultipleValues as field values
     * whenever there is more than one value.
     * 
     * @see bibtex.dom.BibtexMultipleValues
     */
    public static final int KEEP_ALL = 2;

}