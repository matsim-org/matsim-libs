package playground.wrashid.artemis.parking.rss;


class RSSItem
{
    private String title       = null;
    private String url         = null;
    private String description = null;

    public RSSItem() {}

    public RSSItem(String title,String url,String description)
    {
        this.title       = title;
        this.url         = url;
        this.description = description;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void setURL(String url)
    {
        this.url = url;
    }

    public String getURL()
    {
        return url;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }
}
