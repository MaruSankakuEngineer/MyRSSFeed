package com.example.myrssfeed.data.remote.dto

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "rss", strict = false)
class RssResponse {
    @field:Element(name = "channel", required = false)
    var channel: RssChannel = RssChannel()
}

@Root(name = "channel", strict = false)
class RssChannel {
    @field:Element(name = "title", required = false)
    var title: String = ""
    
    @field:Element(name = "description", required = false)
    var description: String? = null
    
    @field:ElementList(name = "item", inline = true, required = false)
    var items: MutableList<RssItem> = mutableListOf()
}

@Root(name = "item", strict = false)
class RssItem {
    @field:Element(name = "title", required = false)
    var title: String = ""
    
    @field:Element(name = "description", required = false)
    var description: String? = null
    
    @field:Element(name = "link", required = false)
    var link: String = ""
    
    @field:Element(name = "pubDate", required = false)
    var pubDate: String? = null
    
    @field:Element(name = "enclosure", required = false)
    var enclosure: RssEnclosure? = null
}

@Root(name = "enclosure", strict = false)
class RssEnclosure {
    @field:org.simpleframework.xml.Attribute(name = "url", required = false)
    var url: String = ""
    
    @field:org.simpleframework.xml.Attribute(name = "type", required = false)
    var type: String? = null
} 