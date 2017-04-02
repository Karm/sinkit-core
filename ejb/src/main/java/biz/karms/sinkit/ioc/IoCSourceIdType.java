package biz.karms.sinkit.ioc;

import com.google.gson.annotations.SerializedName;

/**
 * @author Tomas Kozel
 */
public enum IoCSourceIdType {

    @SerializedName("url")
    URL,

    @SerializedName("ip")
    IP,

    @SerializedName("fqdn")
    FQDN
}
