package com.example.elderlycare;

import java.util.List;

public class DocumentModel {
    public String id;
    public String date;
    public String type;
    public String title;
    public String notes;


    public List<String> healthTags;
    public List<String> fileUrls; // multiple uploaded URLs
    public List<String> fileNames; // optional: original filenames
    public List<String> fileSizes;

    public List<String> filePublicIds;
    public long timestamp;

    public DocumentModel() {}
}
