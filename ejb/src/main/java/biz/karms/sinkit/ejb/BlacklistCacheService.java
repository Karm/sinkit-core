package biz.karms.sinkit.ejb;

import biz.karms.sinkit.ioc.IoCAPI;
import biz.karms.sinkit.ioc.IoCRecord;

import javax.ejb.Local;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface BlacklistCacheService {
    boolean addToCache(IoCRecord ioCRecord, IoCAPI apiSource);

    boolean removeFromCache(IoCRecord ioCRecord);

    boolean removeWholeObjectFromCache(IoCRecord record);

    boolean dropTheWholeCache();
}
