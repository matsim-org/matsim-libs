package playground.clruch.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;

/** Created by Joel on 26.06.2017.
 * reworked by clruch */

public class HtmlUtils {
    public StringBuilder stringBuilder = new StringBuilder();
    public boolean html = false;
    public boolean head = false;
    public boolean header = false;
    public boolean body = false;
    public boolean paragraph = false;
    public boolean footer = false;
    public boolean style = false;
    
    /*package*/ HtmlUtils(){
        
    }

    /** called at the begin and end of document */
    public void html() {
        if (html) {
            stringBuilder.append("</html>");
            html = false;
        } else {
            stringBuilder.append("<html>");
            html = true;
            this.insertCSS("header, footer {font-family: arial;", //
                    "background-color: #000099;", //
                    "color: white;", //
                    "padding: 20px;", //
                    "float: left;", //
                    "width: 100%}", //
                    "body {font-family: verdana;", //
                    "font-size: 16px;", //
                    "line-height: 1.75;}", //
                    "img {padding: 5px;}", //
                    "#footer_link {color: white;}");
        }
    }

    /** called at the begin and end of head */
    public void head() {
        if (head) {
            stringBuilder.append("</head>");
            head = false;
        } else {
            stringBuilder.append("<head>");
            head = true;
        }
    }

    /** called at the begin and end of header */
    public void header() {
        if (header) {
            stringBuilder.append("</header>");
            header = false;
        } else {
            stringBuilder.append("<header>");
            header = true;
        }
    }

    /** called at the begin and end of document */
    public void body() {
        if (body) {
            stringBuilder.append("</body>");
            body = false;
        } else {
            stringBuilder.append("<body>");
            body = true;
        }
    }

    /** called at the begin and end of paragraph */
    public void paragraph() {
        if (paragraph) {
            stringBuilder.append("</p>");
            paragraph = false;
        } else {
            stringBuilder.append("<p>");
            paragraph = true;
        }
    }

    public void newLine() {
        stringBuilder.append("<br style=\"clear:both\">");
    }

    /** called at the begin and end of footer */
    public void footer() {
        if (footer) {
            stringBuilder.append("</footer>");
            header = false;
        } else {
            stringBuilder.append("<footer>");
            footer = true;
        }
    }

    /** called at the begin and end of CSS */
    public void style() {
        if (style) {
            stringBuilder.append("</style>");
            style = false;
        } else {
            stringBuilder.append("<style>");
            style = true;
        }
    }

    /** tab title
     *
     * @param title */
    public void setTitle(String title) {
        head();
        stringBuilder.append("<title>" + title + "</title>");
        head();
    }

    /** page title
     *
     * @param title */
    public void insertTitle(String title) {
        header();
        stringBuilder.append("<h1>" + title + "</h1>");
        header();
    }

    public void insertSubTitle(String title) {
        stringBuilder.append("<h2>" + title + "</h2>");
    }

    public void title(String title) {
        insertTitle(title);
        setTitle(title);
    }

    public void insertText(String... lines) {
        paragraph();
        for (int i = 0; i < lines.length; i++)
            stringBuilder.append(lines[i] + "<br>");
        paragraph();
    }

    public void insertText(String text) {
        stringBuilder.append("<pre>" + text + "</pre>");
    }

    public void insertTextLeft(String text) {
        stringBuilder.append("<pre id=\"pre_left\">" + text + "</pre>");
    }

    public void insertTextRight(String text) {
        stringBuilder.append("<pre id=\"pre_right\">" + text + "</pre>");
    }

    public void insertLink(String url, String link) {
        this.insertCSS("a {font-family: arial;}");
        if (footer || header)
            stringBuilder.append("<a id=\"footer_link\" target=\"_blank\"" + " href=\"" + url + "\"> <b>" + link + "</b></a>");
        else
            stringBuilder.append("<a target=\"_blank\" href=\"" + url + "\"> <b>" + link + "</b></a>");
    }

    public void insertCSS(String... line) {
        head();
        style();
        for (int i = 0; i < line.length; i++)
            stringBuilder.append(line[i]);
        style();
        head();
    }

    public void insertImg(String relPath) {
        insertImg(relPath, 800, 600);
    }

    public void insertImg(String relPath, int width, int heigth) {
        stringBuilder.append("<img src=" + relPath + " alt=\"Image not found\" style=\"width:" + //
                width + "px;height:" + heigth + "px;\">");
    }

    public void insertImgIfExists(String relPath, String reportLoc, int width, int heigth) {

        File imgFile = new File(reportLoc, relPath);
        System.out.println("relative Path = " + relPath);
        System.out.println("searching for file " + imgFile.getAbsolutePath());
        if (imgFile.exists()) {
            stringBuilder.append("<img src=" + relPath + " alt=\"Image not found\" style=\"width:" + //
                    width + "px;height:" + heigth + "px;\">");
        } else
            System.out.println("file " + imgFile.getAbsolutePath() + " not found.");
    }

    public void insertImgRight(String relPath, int width, int heigth) {
        stringBuilder.append("<img id=\"img_right\" float=\"right\" src=" + relPath + " " + //
                "alt=\"Image not found\" style=\"width:" + width + "px;height:" + heigth + "px;\">");
    }

    /** first elements are automatically set as headings
     *
     * @param columns
     * @param rows
     * @param borders
     * @param content */
    public void insertTable(int columns, int rows, boolean borders, String... content) {
        if (content.length != columns * rows)
            stringBuilder.append("unable to create table");
        else {
            if (borders)
                insertCSS("th, td {border: 1px solid black;}");
            insertCSS("th, td {padding: 5px; margin: 5px}");
            stringBuilder.append("<table style=\"border-collapse: collapse;\">");
            for (int i = 0; i < rows; i++) {
                stringBuilder.append("<tr>");
                for (int j = 0; j < columns; j++) {
                    if (i == 0)
                        stringBuilder.append("<th>" + content[j] + "</th>");
                    else
                        stringBuilder.append("<td>" + content[i * columns + j] + "</td>");
                }
                stringBuilder.append("</tr>");
            }
            stringBuilder.append("</table>");
        }
    }

    public void insertList(String... lines) {
        stringBuilder.append("<ul style=\"list-style-type:disc\">");
        for (int i = 0; i < lines.length; i++)
            stringBuilder.append("<li>" + lines[i] + "</li>");
        stringBuilder.append("</ul>");
    }

    public String bold(String text) {
        return "<b>" + text + "</b>";
    }

    public void saveFile(String fileName, String outputdirectory) throws IOException {
        File file = new File(outputdirectory + "/report", fileName + ".html");
        // if file does exists, then delete and create a new file
        Files.deleteIfExists(file.toPath());

        // write to file with OutputStreamWriter
        OutputStream outputStream = new FileOutputStream(file.getAbsoluteFile());
        Writer writer = new OutputStreamWriter(outputStream);
        writer.write(stringBuilder.toString());
        writer.close();

    }
}
