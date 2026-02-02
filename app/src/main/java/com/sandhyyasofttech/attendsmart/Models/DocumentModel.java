package com.sandhyyasofttech.attendsmart.Models;

public class DocumentModel {
    private String docId;
    private String docName;
    private String docType;
    private String description;
    private String fileUrl;
    private String fileName;
    private String fileSize;
    private String fileType;
    private String uploadedBy;
    private String uploadedByName;
    private String uploadDate;
    private long uploadTimestamp;
    private boolean verified;
    private String verifiedBy;
    private String verifiedDate;
    
    // Empty constructor for Firebase
    public DocumentModel() {}
    
    // Getters and Setters
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    
    public String getDocName() { return docName; }
    public void setDocName(String docName) { this.docName = docName; }
    
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    
    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }
    
    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }
    
    public long getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(long uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    
    public String getVerifiedDate() { return verifiedDate; }
    public void setVerifiedDate(String verifiedDate) { this.verifiedDate = verifiedDate; }
}