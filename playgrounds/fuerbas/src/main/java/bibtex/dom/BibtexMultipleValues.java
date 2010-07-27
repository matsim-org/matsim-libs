/*
 * Created on Jul 14, 2004
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import bibtex.Assertions;

/**
 * Some bibtex files have multiple values per field - this is a container that
 * contains these values. For example, the following BibtexEntry has multiple
 * values for the url field:
 * 
 * <pre>
 *   &#064;inproceedings{diwan98typebased,
 *      year=1998,
 *      pages={106-117},
 *      title={Type-Based Alias Analysis},
 *      url={citeseer.nj.nec.com/diwan98typebased.html},
 *      booktitle={SIGPLAN Conference on Programming Language Design and Implementation},
 *      author={Amer Diwan and Kathryn S. McKinley and J. Eliot B. Moss},
 *      url={http://www-plan.cs.colorado.edu/diwan/},
 *   }
 * </pre>
 * 
 * Note that the bibtex parser in this package will discard duplicate values
 * unless you set the appropriate policy
 * with BibtexParser.setMultipleFieldValuesPolicy(int).
 * 
 * @see bibtex.parser.BibtexParser#setMultipleFieldValuesPolicy(int)
 * @see bibtex.parser.BibtexMultipleFieldValuesPolicy#KEEP_ALL
 * 
 * @author henkel
 */
public final class BibtexMultipleValues extends BibtexAbstractValue {

    private final ArrayList values = new ArrayList(3);

    /**
     * @param bibtexFile
     */
    BibtexMultipleValues(BibtexFile bibtexFile) {
        super(bibtexFile);
    }

    /**
     * This will add the value object to this BibtexMultipleValues object. Do
     * not try to add the same object multiple times.
     * 
     * @param value
     *            is the value you want to add to this instance. Note that this
     *            may be anything but another BibtexAbstractValue instance - we
     *            don't want to have nested BibtexMultipleValue instances.
     */
    public void addValue(BibtexAbstractValue value) {

        assert value != null : "value parameter may not be null.";
        assert !(value instanceof BibtexMultipleValues) : "You cannot add a BibtexMultipleValues instance to a BibtexMultipleValues instance.";
        assert !Assertions.ENABLE_EXPENSIVE_ASSERTIONS || !values.contains(value) : "value is already contained in this BibtexMultipleValues object.";

        values.add(value);
    }

    /**
     * Removes value from this BibtexMultipleValues object.
     * 
     * @param value
     */
    public void removeValue(BibtexAbstractValue value) {
        assert value != null : "value parameter may not be null.";

        boolean wasFound = values.remove(value);

        assert wasFound : "value parameter was not found inside this BibtexMultipleValues object.";
    }

    /**
     * This method will print all the bibtex values contained in this
     * BibtexMultipleValues instance, separated with empty lines. Note that the
     * output will not be valid bibtex - in valid bibtex, you'd have to print
     * the field name before printing each of these values. Therefore, this
     * method is intended to be used for debugging only.
     * 
     * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
     */
    public void printBibtex(PrintWriter writer) {
        assert writer != null : "writer parameter may not be null.";

        for (Iterator it = this.values.iterator(); it.hasNext();) {
            ((BibtexAbstractValue) it.next()).printBibtex(writer);
            if (it.hasNext()) {
                writer.println();
                writer.println();
            }
        }
    }

    /**
     * This method returns the values of this BibtexMultipleValues instance in
     * an unmodifiable List.
     * 
     * @return an unmodifiable List with instances of type BibtexAbstractValue
     */
    public List getValues() {
        return Collections.unmodifiableList(this.values);
    }

    /**
     * This method will print all the bibtex values contained in this
     * BibtexMultipleValues instance, separated with empty lines. Note that the
     * output will not be valid bibtex - in valid bibtex, you'd have to print
     * the field name before printing each of these values. Therefore, this
     * method is intended to be used for debugging only.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return super.toString();
    }

}