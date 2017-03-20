package biz.karms.sinkit.ejb.impl;

import biz.karms.sinkit.ejb.BlacklistCacheService;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCache;
import biz.karms.sinkit.ejb.cache.annotations.SinkitCacheName;
import biz.karms.sinkit.ejb.cache.pojo.BlacklistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.infinispan.client.hotrod.RemoteCache;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Michal Karm Babacek
 */
@Stateless
public class BlacklistCacheServiceEJB implements BlacklistCacheService {

    @Inject
    private Logger log;

    @Inject
    @SinkitCache(SinkitCacheName.infinispan_blacklist)
    private RemoteCache<String, BlacklistedRecord> blacklistCache;

    //TODO: Batch mode. It is wasteful to operate for 1 single update like this for thousand times.
    @Override
    public boolean addToCache(final IoCRecord ioCRecord) {
        if (ioCRecord == null || ioCRecord.getSource() == null || ioCRecord.getClassification() == null || ioCRecord.getFeed() == null || ioCRecord.getDocumentId() == null) {
            log.log(Level.SEVERE, "addToCache: ioCRecord itself or its source, documentId, classification or feed were null. Can't process that.");
            return false;
        }

        if (ioCRecord.getSource().getId() == null || ioCRecord.getSource().getId().getValue() == null) {
            log.log(Level.SEVERE, "addToCache: ioCRecord can't have source id null.");
            return false;
        }

        final String key = DigestUtils.md5Hex(ioCRecord.getSource().getId().getValue());
        try {
            if (blacklistCache.containsKey(key)) {
                final BlacklistedRecord blacklistedRecord = blacklistCache.get(key);
                if (blacklistedRecord == null) {
                    log.log(Level.SEVERE, "addToCache: blacklistedRecord allegedly exists in the Cache already under key " + key + ", but we failed to retrieve it.");
                    return false;
                }
                final HashMap<String, ImmutablePair<String, String>> feedToTypeUpdate = blacklistedRecord.getSources();
                if (ioCRecord.getFeed().getName() != null && ioCRecord.getClassification().getType() != null) {
                    feedToTypeUpdate.putIfAbsent(ioCRecord.getFeed().getName(), new ImmutablePair<>(ioCRecord.getClassification().getType(), ioCRecord.getDocumentId()));
                } else {
                    log.log(Level.SEVERE, "addToCache: ioCRecord's feed or classification type were null");
                }
                blacklistedRecord.setSources(feedToTypeUpdate);
                blacklistedRecord.setListed(Calendar.getInstance());
                log.log(Level.FINE, "Replacing key [" + ioCRecord.getSource().getId().getValue() + "], hashed: " + key);
                blacklistCache.replace(key, blacklistedRecord);
            } else {
                HashMap<String, ImmutablePair<String, String>> feedToType = new HashMap<>();
                if (ioCRecord.getFeed().getName() != null && ioCRecord.getClassification().getType() != null) {
                    feedToType.put(ioCRecord.getFeed().getName(), new ImmutablePair<>(ioCRecord.getClassification().getType(), ioCRecord.getDocumentId()));
                } else {
                    log.log(Level.SEVERE, "addToCache: ioCRecord's feed or classification type were null");
                }
                BlacklistedRecord blacklistedRecord = new BlacklistedRecord(key, Calendar.getInstance(), feedToType);
                log.log(Level.FINE, "Putting new key [" + ioCRecord.getSource().getId().getValue() + "], hashed: " + key);
                blacklistCache.put(key, blacklistedRecord);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "addToCache", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean removeFromCache(final IoCRecord ioCRecord) {
        if (ioCRecord == null || ioCRecord.getSource() == null || ioCRecord.getFeed() == null) {
            log.log(Level.SEVERE, "removeFromCache: ioCRecord itself or its source or its feed were null. Can't process that.");
            return false;
        }

        if (ioCRecord.getSource().getId() == null || ioCRecord.getSource().getId().getValue() == null) {
            log.log(Level.SEVERE, "removeFromCache: ioCRecord can't have source id null.");
            return false;
        }

        final String key = DigestUtils.md5Hex(ioCRecord.getSource().getId().getValue());
        try {
            if (blacklistCache.containsKey(key)) {
                final BlacklistedRecord blacklistedRecord = blacklistCache.get(key);
                HashMap<String, ImmutablePair<String, String>> feedToTypeUpdate = blacklistedRecord.getSources();
                if (ioCRecord.getFeed().getName() != null) {
                    feedToTypeUpdate.remove(ioCRecord.getFeed().getName());
                } else {
                    log.log(Level.FINE, "removeFromCache: ioCRecord's feed was null.");
                }
                if (MapUtils.isEmpty(feedToTypeUpdate)) {
                    // As soon as there are no feeds, we remove the IoC from the cache
                    blacklistCache.remove(key);
                } else {
                    blacklistedRecord.setSources(feedToTypeUpdate);
                    blacklistedRecord.setListed(Calendar.getInstance());
                    blacklistCache.replace(key, blacklistedRecord);
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "removeFromCache", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean removeWholeObjectFromCache(final IoCRecord iocRecord) {
        if (iocRecord == null || iocRecord.getSource() == null || iocRecord.getSource().getId() == null ||
                StringUtils.isBlank(iocRecord.getSource().getId().getValue())) {
            log.log(Level.SEVERE, "removeWholeObjectFromCache: ioc or ioc.source.id.value is null or blank");
            return false;
        }
        final String key = DigestUtils.md5Hex(iocRecord.getSource().getId().getValue());
        try {
            if (blacklistCache.containsKey(key)) {
                blacklistCache.remove(key);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "removeFromCache", e);
            return false;
        }
        return true;
    }

    /**
     * This is very evil.
     *
     * @return true if everything went well.
     */
    @Override
    public boolean dropTheWholeCache() {
        log.log(Level.SEVERE, "dropTheWholeCache: We are dropping the cache. This has severe operational implications.");
        try {
            // TODO: Clear is faster, but apparently quite ugly. Investigate clearAsync().
            // TODO: Could this handle millions of records in a dozen node cluster? :)
            blacklistCache.keySet().forEach(key -> blacklistCache.remove(key));
        } catch (Exception e) {
            log.log(Level.SEVERE, "dropTheWholeCache", e);
            return false;
        }
        return true;
    }
}
