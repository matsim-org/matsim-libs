package playground.clruch.html;

import java.io.File;

/**
 * Created by Joel on 26.06.2017.
 */
public class htmlDemo {
    public static void main(String[] args) {
        String fileName = "htmlDemo";

        File config = new File(args[0]);
        File file = new File(config.getParent());
        file.mkdir();

        // write document
        // -------------------------------------------------------------------------------------------------------------
        // set up
        htmlUtils.html();
        // specific formatting for the entire document
        htmlUtils.insertCSS( //
                "p {text-align: center;", //
                    "width: 40%;", //
                    "padding: 20px;", //
                    "margin: 10px;", //
                    "float: left;", //
                    "border: 1px solid black;}", //
                "img {float: right;", //
                    "padding: 20px;}", //
                "pre {float: left;}", //
                "table {float: right;}" //
        );
        // begin of body
        htmlUtils.body();
        // set title in header and as tab title
        htmlUtils.title("HTML Demo");
        // text displayed line by line
        htmlUtils.insertText("1st line", "2nd line", "3rd line", "and so forth");
        htmlUtils.insertImg("data/binnedWaitingTimes.png", 400, 300);
        htmlUtils.newLine();
        // text written in this exact manner
        htmlUtils.insertText("1st line of preformatted text\n" + //
                "2nd line of preformatted text\n" + //
                "3rd line and \t so \t forth");
        htmlUtils.insertTable(3,2, true, "h1", "h2", "h3", "c1", "c2", "c3");
        htmlUtils.newLine();
        htmlUtils.insertList("element 1", "element 2", "element 3", "element 3");
        // begin of footer
        htmlUtils.footer();
        htmlUtils.insertLink("http://www.idsc.ethz.ch/", "www.idsc.ethz.ch");
        // end of footer
        htmlUtils.footer();
        // end of body
        htmlUtils.body();
        htmlUtils.html();

        // save document
        // -------------------------------------------------------------------------------------------------------------
        try {
            htmlUtils.saveFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
