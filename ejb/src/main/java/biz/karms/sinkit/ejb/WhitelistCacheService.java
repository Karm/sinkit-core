package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ejb.cache.pojo.WhitelistedRecord;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * @author Tomas Kozel
 */
@Local
public interface WhitelistCacheService {
    WhitelistedRecord put(final IoCRecord iocRecord, boolean completed);

    WhitelistedRecord setCompleted(WhitelistedRecord partialWhite);

    WhitelistedRecord get(String id);

    boolean remove(String id);

    boolean isWhitelistEmpty();

    boolean dropTheWholeCache();

}
