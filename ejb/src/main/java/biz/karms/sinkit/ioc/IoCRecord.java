package biz.karms.sinkit.ioc;

import biz.karms.sinkit.ejb.elastic.Indexable;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomas Kozel
 */
public class IoCRecord implements Indexable {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    private static final long serialVersionUID = -4129116350560247954L;

    //@JestId
    @SerializedName("document_id")
    private String documentId;
    @SerializedName("unique_ref")
    private String uniqueRef;
    private IoCFeed feed;
    private IoCDescription description;
    private IoCClassification classification;
    private IoCProtocol protocol;
    private String raw;
    private IoCSource source;
    private IoCTime time;
    private IoCSeen seen;
    private Boolean active;
    @SerializedName("whitelist_name")
    private String whitelistName;
    @SerializedName("virus_total_reports")
    private IoCVirusTotalReport[] virusTotalReports;
    private HashMap<String, Integer> accuracy;

    public IoCRecord() {
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getUniqueRef() {
        return uniqueRef;
    }

    public void setUniqueRef(String uniqueRef) {
        this.uniqueRef = uniqueRef;
    }

    public IoCFeed getFeed() {
        return feed;
    }

    public void setFeed(IoCFeed feed) {
        this.feed = feed;
    }

    public IoCDescription getDescription() {
        return description;
    }

    public void setDescription(IoCDescription description) {
        this.description = description;
    }

    public IoCClassification getClassification() {
        return classification;
    }

    public void setClassification(IoCClassification classification) {
        this.classification = classification;
    }

    public IoCProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(IoCProtocol protocol) {
        this.protocol = protocol;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public IoCSource getSource() {
        return source;
    }

    public void setSource(IoCSource source) {
        this.source = source;
    }

    public IoCTime getTime() {
        return time;
    }

    public void setTime(IoCTime time) {
        this.time = time;
    }

    public IoCSeen getSeen() {
        return seen;
    }

    public void setSeen(IoCSeen seen) {
        this.seen = seen;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getWhitelistName() {
        return whitelistName;
    }

    public void setWhitelistName(String whitelistName) {
        this.whitelistName = whitelistName;
    }

    public IoCVirusTotalReport[] getVirusTotalReports() {
        return virusTotalReports;
    }

    public void setVirusTotalReports(IoCVirusTotalReport[] virusTotalReports) {
        this.virusTotalReports = virusTotalReports;
    }

    public HashMap<String, Integer> getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(HashMap<String, Integer> accuracy) {
        this.accuracy = accuracy;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
