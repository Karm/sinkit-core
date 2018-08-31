package biz.karms.sinkit.ejb;

import biz.karms.sinkit.eventlog.EventLogRecord;
import biz.karms.sinkit.exception.ArchiveException;
import biz.karms.sinkit.ioc.IoCAccuCheckerReport;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCVirusTotalReport;

import javax.ejb.Local;
import java.util.List;

/**
 * @author Michal Karm Babacek
 */
@Local
public interface ArchiveService {

    List<IoCRecord> findIoCsForDeactivation(int hours) throws ArchiveException;

    List<IoCRecord> findIoCsForWhitelisting(String sourceId) throws ArchiveException;

    boolean archiveReceivedIoCRecord(IoCRecord ioc) throws ArchiveException;

    IoCRecord deactivateRecord(IoCRecord ioc) throws ArchiveException;

    IoCRecord setRecordWhitelisted(IoCRecord ioc, String whitelistName) throws ArchiveException;

    EventLogRecord archiveEventLogRecord(EventLogRecord logRecord) throws ArchiveException;

    EventLogRecord archiveEventLogRecordUsingLogstash(EventLogRecord logRecord) throws ArchiveException;

    List<IoCRecord> getActiveNotWhitelistedIoCs(int from, int size) throws ArchiveException;

    List<IoCRecord> getMatchingEntries(String name, String value) throws ArchiveException;

    IoCRecord getIoCRecordById(String id) throws ArchiveException;

    IoCRecord getIoCRecordByUniqueRef(String uniqueRef) throws ArchiveException;

    EventLogRecord getLogRecordWaitingForVTScan(int notAllowedFailedMinutes) throws ArchiveException;

    EventLogRecord getLogRecordWaitingForVTReport(int notAllowedFailedMinutes) throws ArchiveException;

    boolean setVirusTotalReportToIoCRecord(IoCRecord ioc, IoCVirusTotalReport[] reports) throws ArchiveException;

    boolean setReportToIoCRecord(IoCAccuCheckerReport ioc, String documentId) throws ArchiveException;
}
