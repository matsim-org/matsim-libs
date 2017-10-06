package playground.clruch.html;

import java.io.File;

/**
 * Created by Joel on 26.06.2017.
 */
public class HtmlDemo {
    public static void main(String[] args) {
        String outputdirectory = "output";
        String fileName = "htmlDemo";

        File config = new File(args[0]);
        File file = new File(config.getParent());
        file.mkdir();

        // write document
        // -------------------------------------------------------------------------------------------------------------
        // set up
        HtmlUtils.html();
        // specific formatting for the entire document
        HtmlUtils.insertCSS( //
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
        HtmlUtils.body();
        // set title in header and as tab title
        HtmlUtils.title("HTML Demo");
        // text displayed line by line
        HtmlUtils.insertText("1st line", "2nd line", "3rd line", "and so forth");
        HtmlUtils.insertImg("data/binnedWaitingTimes.png", 400, 300);
        HtmlUtils.newLine();
        // text written in this exact manner
        HtmlUtils.insertText("1st line of preformatted text\n" + //
                "2nd line of preformatted text\n" + //
                "3rd line and \t so \t forth");
        HtmlUtils.insertTable(3,2, true, "h1", "h2", "h3", "c1", "c2", "c3");
        HtmlUtils.newLine();
        HtmlUtils.insertList("element 1", "element 2", "element 3", "element 3");
        // begin of footer
        HtmlUtils.footer();
        HtmlUtils.insertLink("http://www.idsc.ethz.ch/", "www.idsc.ethz.ch");
        // end of footer
        HtmlUtils.footer();
        // end of body
        HtmlUtils.body();
        HtmlUtils.html();

        // save document
        // -------------------------------------------------------------------------------------------------------------
        try {
            HtmlUtils.saveFile(fileName, outputdirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
