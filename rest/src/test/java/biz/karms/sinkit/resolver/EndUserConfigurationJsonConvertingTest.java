package biz.karms.sinkit.resolver;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

public class EndUserConfigurationJsonConvertingTest {

    @Test
    public void testConvertJsonToObject() {
        final InputStream jsonFileInputStream = EndUserConfigurationJsonConvertingTest.class.getClassLoader()
                .getResourceAsStream("end_user_configuration.json");
        final EndUserConfiguration configuration = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                .fromJson(new InputStreamReader(jsonFileInputStream), EndUserConfiguration.class);


        assertThat(configuration, notNullValue());
        assertThat(configuration.getBlacklist(), notNullValue());
        assertThat(configuration.getBlacklist(), CoreMatchers.hasItems("malware.com"));
        assertThat(configuration.getWhitelist(), notNullValue());
        assertThat(configuration.getWhitelist(), CoreMatchers.hasItems("google.com", "whalebone.io"));
        assertThat(configuration.getIdentities(), notNullValue());
        assertThat(configuration.getIdentities(), CoreMatchers.hasItems("device567", "user123", "9865xyz"));
        assertThat(configuration.getPolicyId(), is(123));
        assertThat(configuration.getClientId(), is(2));
        assertThat(configuration.getUserId(), is("someuser"));
        assertThat(configuration.getId(), is("2:someuser"));

    }

    @Test
    public void testConvertJsonToObjectWithIPRanges() {
        final InputStream jsonFileInputStream = EndUserConfigurationJsonConvertingTest.class.getClassLoader()
                .getResourceAsStream("end_user_configuration_ipranges.json");
        final EndUserConfiguration configuration = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
                .fromJson(new InputStreamReader(jsonFileInputStream), EndUserConfiguration.class);


        assertThat(configuration, notNullValue());
        assertThat(configuration.getBlacklist(), notNullValue());
        assertThat(configuration.getBlacklist(), CoreMatchers.hasItems("domain.example.com", "lalala.example.com"));
        assertThat(configuration.getWhitelist(), notNullValue());
        assertThat(configuration.getWhitelist(), CoreMatchers.hasItems("w1.example.com", "w2.example.com"));
        assertThat(configuration.getIdentities(), notNullValue());
        assertThat(configuration.getIdentities(), CoreMatchers.hasItems("device567", "user123", "9865xyz"));
        assertThat(configuration.getPolicyId(), is(123456));
        assertThat(configuration.getClientId(), is(270));
        assertThat(configuration.getUserId(), is("someuser"));
        assertThat(configuration.getId(), is("270:someuser"));
        assertThat(configuration.getIpRanges(), hasItems("10.10.0.0/16", "10.20.30.0/8", "2001:0db8:85a3:1234:5678:8a2e:0370:0/8"));

    }
}
