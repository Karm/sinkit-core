package biz.karms.sinkit.tests.core;

import biz.karms.sinkit.ejb.ArchiveService;
import biz.karms.sinkit.ejb.CoreService;
import biz.karms.sinkit.ejb.DNSApi;
import biz.karms.sinkit.ejb.impl.ArchiveServiceEJB;
import biz.karms.sinkit.eventlog.EventLogAction;
import biz.karms.sinkit.exception.TooOldIoCException;
import biz.karms.sinkit.ioc.IoCRecord;
import biz.karms.sinkit.ioc.IoCSourceIdType;
import biz.karms.sinkit.tests.util.IoCFactory;
import com.gargoylesoftware.htmlunit.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static org.testng.Assert.*;


/**
 * Created by tkozel on 29.8.15.
 */
public class CoreTest extends Arquillian {

    private static final Logger LOGGER = Logger.getLogger(CoreTest.class.getName());
    private static final String TOKEN = System.getenv("SINKIT_ACCESS_TOKEN");

    @EJB
    CoreService coreService;

    @EJB
    ArchiveService archiveService;

    @EJB
    DNSApi dnsApi;

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 12)
    public void deduplicationTest() throws Exception {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        Date lastObservation = c.getTime();
        c.add(Calendar.MINUTE, -10);
        Date firstObservation = c.getTime();
        assertNotEquals(firstObservation, lastObservation, "Expected last and first observation times to be different, but are the same: " + firstObservation);

        IoCRecord ioc = IoCFactory.getIoCRecordAsRecieved("deduplication", "phishing", "phishing.ru", IoCSourceIdType.FQDN, firstObservation, null);
        ioc = coreService.processIoCRecord(ioc);
        assertNotNull(ioc.getDocumentId(), "Expecting documentId generated by elastic, but got null");
        assertTrue(ioc.isActive(), "Expected ioc to be active, but got inactive");
        IoCRecord iocDupl = IoCFactory.getIoCRecordAsRecieved("deduplication", "phishing", "phishing.ru", IoCSourceIdType.FQDN, lastObservation, null);
        iocDupl = coreService.processIoCRecord(iocDupl);
        assertEquals(iocDupl.getDocumentId(), ioc.getDocumentId(), "Expected documentId: " + ioc.getDocumentId() + ", but got: " + iocDupl.getDocumentId());
        assertTrue(iocDupl.isActive(), "Expected iocDupl to be active, but got inactive");
        IoCRecord iocIndexed = archiveService.getIoCRecordById(iocDupl.getDocumentId());
        assertNotNull(iocIndexed, "Expecting ioc to be found in elastic, but got null");
        assertEquals(iocIndexed.getDocumentId(), ioc.getDocumentId(), "Expexted found document id: " + ioc.getDocumentId() + ", but got: " + iocIndexed.getDocumentId());
        assertEquals(iocIndexed.getSeen().getLast(), lastObservation, "Expected seen.last: " + lastObservation + ", but got: " + iocIndexed.getSeen().getLast());
        assertEquals(iocIndexed.getTime().getObservation(), firstObservation, "Expected time.observation: " + firstObservation + ", but got " + iocIndexed.getTime().getObservation());
        assertEquals(iocIndexed.getSeen().getFirst(), firstObservation, "Expected seen.first: " + firstObservation + ", but got: " + iocIndexed.getSeen().getFirst());
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 13, expectedExceptions = TooOldIoCException.class)
    public void tooOldSourceTimeTest() throws Exception {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        Date timeObservation = c.getTime();
        c.add(Calendar.HOUR, -coreService.getIocActiveHours());
        Date timeSource = c.getTime();

        IoCRecord ioc = IoCFactory.getIoCRecordAsRecieved("tooOldIoc", "phishing", "phishing.ru", IoCSourceIdType.FQDN, timeObservation, timeSource);
        coreService.processIoCRecord(ioc);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 14, expectedExceptions = TooOldIoCException.class)
    public void tooOldObservationTimeTest() throws Exception {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.HOUR, -coreService.getIocActiveHours());
        Date timeObservation = c.getTime();
        IoCRecord ioc = IoCFactory.getIoCRecordAsRecieved("tooOldIoc", "phishing", "phishing.ru", IoCSourceIdType.FQDN, timeObservation, null);
        coreService.processIoCRecord(ioc);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 15)
    public void goodTimeTest() throws Exception {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        Date timeObservation = c.getTime();
        c.add(Calendar.HOUR, -coreService.getIocActiveHours());
        c.add(Calendar.SECOND, 1);
        Date timeSource = c.getTime();

        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.MILLISECOND, -1);
        Date receivedByCore = c1.getTime();

        IoCRecord source = IoCFactory.getIoCRecordAsRecieved("sourceTime", "phishing", "phishing.ru", IoCSourceIdType.FQDN, timeObservation, timeSource);
        source = coreService.processIoCRecord(source);
        assertEquals(source.getSeen().getFirst(), timeSource, "Expected seen.first: " + timeSource + ", but got: " + source.getSeen().getFirst());
        assertEquals(source.getSeen().getLast(), timeSource, "Expected seen.last: " + timeSource + ", but got: " + source.getSeen().getLast());
        assertTrue(receivedByCore.before(source.getTime().getReceivedByCore()), "Expected time.receivedByCore to be after " + receivedByCore + ", but was: " + source.getTime().getReceivedByCore());

        IoCRecord observation = IoCFactory.getIoCRecordAsRecieved("observationTime", "phishing", "phishing.ru", IoCSourceIdType.FQDN, timeObservation, null);
        observation = coreService.processIoCRecord(observation);
        assertEquals(observation.getSeen().getFirst(), timeObservation, "Expected seen.first: " + timeObservation + ", but got: " + observation.getSeen().getFirst());
        assertEquals(observation.getSeen().getLast(), timeObservation, "Expected seen.last: " + timeObservation + ", but got: " + observation.getSeen().getLast());
        assertTrue(receivedByCore.before(observation.getTime().getReceivedByCore()), "Expected time.receivedByCore to be after " + receivedByCore + ", but was: " + observation.getTime().getReceivedByCore());

    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 16)
    public void deactivationTest() throws Exception {
        Calendar c = Calendar.getInstance();
        Date deactivationTime = c.getTime();
        c.add(Calendar.HOUR, -coreService.getIocActiveHours());
        c.add(Calendar.SECOND, 1);
        Date inactiveDate = c.getTime();
        c.add(Calendar.HOUR, 1);
        Date activeDate = c.getTime();

        IoCRecord willNotBeActive = IoCFactory.getIoCRecordAsRecieved("notActive", "phishing", "phishing.ru", IoCSourceIdType.FQDN, inactiveDate, null);
        IoCRecord willBeActive = IoCFactory.getIoCRecordAsRecieved("active", "phishing", "phishing.ru", IoCSourceIdType.FQDN, activeDate, null);

        willNotBeActive = coreService.processIoCRecord(willNotBeActive);
        willBeActive = coreService.processIoCRecord(willBeActive);

        //wait until the willNotBeActive is too old to be active
        Thread.sleep(2100);
        c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(Calendar.HOUR, -coreService.getIocActiveHours());
        Date deactivationLimit = c.getTime();
        assertTrue(deactivationLimit.after(willNotBeActive.getSeen().getLast()), "Expected seen.last to be before: " + deactivationLimit + ", but was: " + willNotBeActive.getSeen().getLast());
        int deactivated = coreService.deactivateIocs();
        assertTrue(deactivated > 0, "Expecting at least 1 deactivated IoC, but got 0");
        willNotBeActive = archiveService.getIoCRecordByUniqueRef(willNotBeActive.getUniqueRef());
        assertFalse(willNotBeActive.isActive(), "Expected not active IoC, but was active");
        assertTrue(deactivationTime.before(willNotBeActive.getTime().getDeactivated()), "Expected time activation to be after: " + deactivationTime + ", but was: " + willNotBeActive.getTime().getDeactivated());

        willBeActive = archiveService.getIoCRecordByUniqueRef(willBeActive.getUniqueRef());
        assertTrue(willBeActive.isActive(), "Expeced active IoC, but was inactive");
    }

    /**
     * Not test exactly, just cleaning old data in elastic DNS event logs
     *
     * @param context
     * @throws Exception
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 17)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void cleanElasticTest(@ArquillianResource URL context) throws Exception {

        String index = IoCFactory.getLogIndex();

        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(
                new URL("http://" + System.getenv("SINKIT_ELASTIC_HOST") + ":" + System.getenv("SINKIT_ELASTIC_PORT") +
                        "/" + index + "/"), HttpMethod.DELETE);
        Page page;
        try {
            page = webClient.getPage(requestSettings);
            assertEquals(200, page.getWebResponse().getStatusCode());
        } catch (FailingHttpStatusCodeException ex) {
            //NO-OP index does not exist yet, but it's ok
        }
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 18)
    public void dnsEventLogTestPrepare() throws Exception {
        String iocId1 = "d056ec334e3c046f0d7fdde6f3d02c8b";
        String iocId2 = "1c9b683e445fcb631cd86b06c882dd07";

        IoCRecord ioc1 = archiveService.getIoCRecordById(iocId1);
        IoCRecord ioc2 = archiveService.getIoCRecordById(iocId2);

        Future a = dnsApi.logDNSEvent(EventLogAction.BLOCK,
                "10.1.1.1",
                "10.1.1.2",
                "requestRaw",
                "seznam.cz",
                "10.1.1.3",
                new HashSet<String>(Arrays.asList(iocId1,iocId2))
        );
        a.get(); //wait for asynch task result
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, priority = 19)
    @OperateOnDeployment("ear")
    @RunAsClient
    public void dnsEventLogTestAssert() throws Exception {

        String index = IoCFactory.getLogIndex();

        WebClient webClient = new WebClient();
        WebRequest requestSettings = new WebRequest(new URL(
                "http://" + System.getenv("SINKIT_ELASTIC_HOST") + ":" + System.getenv("SINKIT_ELASTIC_PORT") + "/" +
                        index + "/" + ArchiveServiceEJB.ELASTIC_LOG_TYPE + "/_search"
        ), HttpMethod.POST);
        requestSettings.setAdditionalHeader("Content-Type", "application/json");
        requestSettings.setAdditionalHeader("X-sinkit-token", TOKEN);
        requestSettings.setRequestBody("{\n" +
                        "   \"query\" : {\n" +
                        "       \"filtered\" : {\n" +
                        "           \"query\" : {\n" +
                        "               \"query_string\" : {\n" +
                        "                   \"query\": \"action : \\\"block\\\" AND " +
                        "                       client : \\\"10.1.1.1\\\" AND " +
                        "                       request.ip : \\\"10.1.1.2\\\" AND " +
                        "                       request.raw : \\\"requestRaw\\\" AND " +
                        "                       reason.fqdn : \\\"seznam.cz\\\" AND " +
                        "                       reason.ip : \\\"10.1.1.3\\\"\"\n" +
                        "               }\n" +
                        "           }\n" +
                        "       }\n" +
                        "   }\n" +
                        "}\n"
        );
        Page page = webClient.getPage(requestSettings);
        assertEquals(200, page.getWebResponse().getStatusCode());
        String responseBody = page.getWebResponse().getContentAsString();
        LOGGER.info("Response:" + responseBody);
        JsonParser jsonParser = new JsonParser();
        JsonArray hits = jsonParser.parse(responseBody).getAsJsonObject()
                .get("hits").getAsJsonObject()
                .get("hits").getAsJsonArray();
        assertTrue(hits.size() == 1);

        JsonObject logRecord = hits.get(0).getAsJsonObject().get("_source").getAsJsonObject();
        assertEquals(logRecord.get("action").getAsString(), "block");
        assertEquals(logRecord.get("client").getAsString(), "10.1.1.1");
        assertEquals(logRecord.get("request").getAsJsonObject().get("ip").getAsString(), "10.1.1.2");
        assertEquals(logRecord.get("request").getAsJsonObject().get("raw").getAsString(), "requestRaw");
        assertEquals(logRecord.get("reason").getAsJsonObject().get("fqdn").getAsString(), "seznam.cz");
        assertEquals(logRecord.get("reason").getAsJsonObject().get("ip").getAsString(), "10.1.1.3");
        assertNotNull(logRecord.get("logged").getAsString());
        assertEquals(logRecord.get("virus_total_request").getAsJsonObject().get("status").getAsString(), "waiting");
        JsonArray matchedIocs = logRecord.get("matched_iocs").getAsJsonArray();
        assertTrue(matchedIocs.size() == 2);
        JsonObject matchedIoc1 = matchedIocs.get(0).getAsJsonObject();
        assertNotNull(matchedIoc1.get("unique_ref"));
        assertEquals(matchedIoc1.get("feed").getAsJsonObject().get("url").getAsString(), "http://www.greatfeed.com/feed.txt");
        assertEquals(matchedIoc1.get("feed").getAsJsonObject().get("name").getAsString(), "integrationTest");
        assertEquals(matchedIoc1.get("description").getAsJsonObject().get("text").getAsString(), "description");
        assertEquals(matchedIoc1.get("classification").getAsJsonObject().get("type").getAsString(), "phishing");
        assertEquals(matchedIoc1.get("classification").getAsJsonObject().get("taxonomy").getAsString(), "Fraud");
        assertEquals(matchedIoc1.get("protocol").getAsJsonObject().get("application").getAsString(), "ssh");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("id").getAsJsonObject().get("value").getAsString(), "phishing.ru");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("id").getAsJsonObject().get("type").getAsString(), "fqdn");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("fqdn").getAsString(), "phishing.ru");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("asn").getAsInt(), 123456);
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("asn_name").getAsString(), "some_name");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("cc").getAsString(), "RU");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("city").getAsString(), "City");
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("latitude").getAsDouble(), 85.12645);
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("geolocation").getAsJsonObject().get("longitude").getAsDouble(), -12.9788);
        assertEquals(matchedIoc1.get("source").getAsJsonObject().get("bgp_prefix").getAsString(), "some_prefix");
        assertNotNull(matchedIoc1.get("time").getAsJsonObject().get("observation"));
        assertNotNull(matchedIoc1.get("time").getAsJsonObject().get("received_by_core"));
    }
}
