package biz.karms.sinkit.resolver;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
public class EndUserConfiguration implements Serializable {
    private Integer clientId;
    private String userId;
    /**
     * While ipRanges for @{@link Policy} MUST NOT be empty and are enforced in @{@link biz.karms.sinkit.ejb.util.ResolverConfigurationValidator},
     * ipRanges in {@link EndUserConfiguration} MAY be empty.
     */
    private Set<String> ipRanges;

    @SerializedName("policy")
    private Integer policyId;

    private Set<String> identities;
    private Set<String> whitelist;
    private Set<String> blacklist;

    public String getId() {
        return String.valueOf(clientId) + ":" + userId;
    }
}
