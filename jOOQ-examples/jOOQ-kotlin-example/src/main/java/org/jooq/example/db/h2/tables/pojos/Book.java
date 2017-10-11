/*
 * This file is generated by jOOQ.
*/
package org.jooq.example.db.h2.tables.pojos;


import java.io.Serializable;
import java.sql.Timestamp;

import javax.annotation.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.1"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Book implements Serializable {

    private static final long serialVersionUID = -2101054336;

    private Integer   id;
    private Integer   authorId;
    private Integer   coAuthorId;
    private Integer   detailsId;
    private String    title;
    private Integer   publishedIn;
    private Integer   languageId;
    private String    contentText;
    private byte[]    contentPdf;
    private Integer   recVersion;
    private Timestamp recTimestamp;

    public Book() {}

    public Book(Book value) {
        this.id = value.id;
        this.authorId = value.authorId;
        this.coAuthorId = value.coAuthorId;
        this.detailsId = value.detailsId;
        this.title = value.title;
        this.publishedIn = value.publishedIn;
        this.languageId = value.languageId;
        this.contentText = value.contentText;
        this.contentPdf = value.contentPdf;
        this.recVersion = value.recVersion;
        this.recTimestamp = value.recTimestamp;
    }

    public Book(
        Integer   id,
        Integer   authorId,
        Integer   coAuthorId,
        Integer   detailsId,
        String    title,
        Integer   publishedIn,
        Integer   languageId,
        String    contentText,
        byte[]    contentPdf,
        Integer   recVersion,
        Timestamp recTimestamp
    ) {
        this.id = id;
        this.authorId = authorId;
        this.coAuthorId = coAuthorId;
        this.detailsId = detailsId;
        this.title = title;
        this.publishedIn = publishedIn;
        this.languageId = languageId;
        this.contentText = contentText;
        this.contentPdf = contentPdf;
        this.recVersion = recVersion;
        this.recTimestamp = recTimestamp;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAuthorId() {
        return this.authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getCoAuthorId() {
        return this.coAuthorId;
    }

    public void setCoAuthorId(Integer coAuthorId) {
        this.coAuthorId = coAuthorId;
    }

    public Integer getDetailsId() {
        return this.detailsId;
    }

    public void setDetailsId(Integer detailsId) {
        this.detailsId = detailsId;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPublishedIn() {
        return this.publishedIn;
    }

    public void setPublishedIn(Integer publishedIn) {
        this.publishedIn = publishedIn;
    }

    public Integer getLanguageId() {
        return this.languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }

    public String getContentText() {
        return this.contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public byte[] getContentPdf() {
        return this.contentPdf;
    }

    public void setContentPdf(byte... contentPdf) {
        this.contentPdf = contentPdf;
    }

    public Integer getRecVersion() {
        return this.recVersion;
    }

    public void setRecVersion(Integer recVersion) {
        this.recVersion = recVersion;
    }

    public Timestamp getRecTimestamp() {
        return this.recTimestamp;
    }

    public void setRecTimestamp(Timestamp recTimestamp) {
        this.recTimestamp = recTimestamp;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Book (");

        sb.append(id);
        sb.append(", ").append(authorId);
        sb.append(", ").append(coAuthorId);
        sb.append(", ").append(detailsId);
        sb.append(", ").append(title);
        sb.append(", ").append(publishedIn);
        sb.append(", ").append(languageId);
        sb.append(", ").append(contentText);
        sb.append(", ").append("[binary...]");
        sb.append(", ").append(recVersion);
        sb.append(", ").append(recTimestamp);

        sb.append(")");
        return sb.toString();
    }
}
