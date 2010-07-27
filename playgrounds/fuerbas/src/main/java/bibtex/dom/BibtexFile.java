/*
 * Created on Mar 17, 2003
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
 * This is the root of a bibtex DOM tree and the factory for any bibtex model -
 * the only way to create nodes. For an example, check out the documentation for
 * the constructor of BibtexNode.
 * 
 * @author henkel
 */
public final class BibtexFile extends BibtexNode {

    private final ArrayList entries = new ArrayList();

    public BibtexFile() {
        super(null);
    }

    public void addEntry(BibtexAbstractEntry entry) {
        assert entry != null : "entry parameter may not be null.";
        assert !Assertions.ENABLE_EXPENSIVE_ASSERTIONS || !this.entries.contains(entry) :
            "entry parameter is already contained within this BibtexFile object.";

        this.entries.add(entry);
    }

    public void removeEntry(BibtexAbstractEntry entry) {
        assert entry != null;

        boolean found = this.entries.remove(entry);

        assert found : "entry parameter was not found.";
    }

    /**
     * returns an unmodifiable view of the entries.
     * 
     * @return List
     */
    public List getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public BibtexConcatenatedValue makeConcatenatedValue(BibtexAbstractValue left, BibtexAbstractValue right) {

        assert left != null : "left parameter may not be null.";
        assert right != null : "right parameter may not be null.";

        return new BibtexConcatenatedValue(this, left, right);
    }

    /**
     * @param entryType
     * @param entryKey
     *            This parameter may be null, but we'll use an empty String in
     *            that case.
     * @return the new entry
     */
    public BibtexEntry makeEntry(String entryType, String entryKey) {

        assert entryType != null : "entryType parameter may not be null.";

        return new BibtexEntry(this, entryType, entryKey == null ? "" : entryKey);
    }

    public BibtexPersonList makePersonList() {
        return new BibtexPersonList(this);
    }

    public BibtexPerson makePerson(String first, String preLast, String last, String lineage, boolean isOthers) {
        assert isOthers || last != null : "(isOthers||last!=null) has to be true.";

        return new BibtexPerson(this, first, preLast, last, lineage, isOthers);
    }

    public BibtexPreamble makePreamble(BibtexAbstractValue content) {
        assert content != null : "content parameter may not be null.";

        return new BibtexPreamble(this, content);
    }

    /**
     * @param content
     *            does not include the quotes or curly braces around the string!
     */
    public BibtexString makeString(String content) {
        assert content != null : "content parameter may not be null.";

        return new BibtexString(this, content);
    }

    public BibtexMultipleValues makeBibtexMultipleValues() {

        return new BibtexMultipleValues(this);
    }

    public BibtexMacroDefinition makeMacroDefinition(String key, BibtexAbstractValue value) {
        assert key != null : "key parameter may not be null.";
        assert value != null : "value parameter may not be null.";
        assert !(value instanceof BibtexMultipleValues) : "value parameter may not be an instance of BibtexMultipleValues";

        return new BibtexMacroDefinition(this, key, value);
    }

    public BibtexMacroReference makeMacroReference(String key) {
        assert key != null : "key parameter may not be null.";

        return new BibtexMacroReference(this, key);
    }

    public BibtexToplevelComment makeToplevelComment(String content) {
        assert content != null : "content parameter may not be null.";

        return new BibtexToplevelComment(this, content);
    }

    public void printBibtex(PrintWriter writer) {
        assert writer != null : "writer parameter may not be null.";

        for (Iterator iter = this.entries.iterator(); iter.hasNext();) {
            BibtexNode node = (BibtexNode) iter.next();
            node.printBibtex(writer);
        }
        writer.flush();
    }

}