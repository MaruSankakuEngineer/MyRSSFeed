package com.example.myrssfeed.data.remote.dto

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

@Root(name = "RDF", strict = false)
@Namespace(reference = "http://www.w3.org/1999/02/22-rdf-syntax-ns#", prefix = "rdf")
class RssResponseRdf {
    @field:Element(name = "channel", required = false)
    var channel: RssChannelRdf? = null
    
    @field:ElementList(entry = "item", inline = true, required = false)
    var items: MutableList<RssItemRdf> = mutableListOf()
}

@Root(name = "channel", strict = false)
class RssChannelRdf {
    @field:Element(name = "title", required = false)
    var title: String = ""
    
    @field:Element(name = "description", required = false)
    var description: String? = null
}

@Root(name = "item", strict = false)
class RssItemRdf {
    @field:Element(name = "title", required = false)
    var title: String = ""
    
    @field:Element(name = "description", required = false)
    var description: String? = null
    
    @field:Element(name = "link", required = false)
    var link: String = ""
    
    @field:Element(name = "date", required = false)
    var date: String? = null
    
    @field:Element(name = "pubDate", required = false)
    var pubDate: String? = null
    
    @field:Element(name = "dc:date", required = false)
    @Namespace(reference = "http://purl.org/dc/elements/1.1/", prefix = "dc")
    var dcDate: String? = null
    
    // 日時を取得するメソッド（複数のフィールドから優先順位で取得）
    fun getPublicationDate(): String? {
        return pubDate ?: date ?: dcDate
    }
} 