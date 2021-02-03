package com.rpantieri.photoDuplicateFinder.model;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Data;

@Entity(name = "IMAGE_INFO")
@Table(indexes = { @Index(name = "imageInfoPathIndex", columnList = "imagePath", unique = true) })
@Data
public class ImageInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;
    @Column(name = "IMAGEPATH", length = 4000, unique = true)
    protected String imagePath;
    @Column(name = "FILENAME", length = 4000)
    protected String fileName;
    @Column(name = "SIZE", length = 4000)
    protected Long size;
    @Column(name = "MD5", length = 200)
    protected String md5;
    @Column(name = "SHA1", length = 200)
    protected String sha1;
    @Column(name = "CREATIONDATE", length = 200)
    protected Date creationDate;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "thumbnail_id")
    protected ImageThumbnail thumbnail;

}
