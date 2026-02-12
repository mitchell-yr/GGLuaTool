// LicenseInfo.java
package mituran.gglua.tool.licenseModel;

public class LicenseInfo {
    private String libraryName;
    private String author;
    private String version;
    private String licenseType;
    private String licenseContent;
    private String url;
    private boolean isExpanded;

    public LicenseInfo(String libraryName, String author, String version,
                       String licenseType, String licenseContent, String url) {
        this.libraryName = libraryName;
        this.author = author;
        this.version = version;
        this.licenseType = licenseType;
        this.licenseContent = licenseContent;
        this.url = url;
        this.isExpanded = false;
    }

    // Getters and Setters
    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getLicenseContent() { return licenseContent; }
    public void setLicenseContent(String licenseContent) { this.licenseContent = licenseContent; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}