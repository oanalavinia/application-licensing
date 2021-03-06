/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.licensing.internal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.instance.InstanceId;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xwiki.licensing.License;
import com.xwiki.licensing.LicenseId;
import com.xwiki.licensing.LicenseSerializer;
import com.xwiki.licensing.LicenseType;
import com.xwiki.licensing.LicensedFeatureId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link XmlStringLicenseSerializer}.
 *
 * @version $Id$
 */
@ComponentList(Base64BinaryStringEncoder.class)
public class XmlStringLicenseSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<LicenseSerializer<String>> mockedSerializer =
        new MockitoComponentMockingRule<>(XmlStringLicenseSerializer.class);

    @Rule
    public AllLogRule logRule = new AllLogRule();

    private LicenseSerializer<String> serializer;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Before
    public void setUp() throws Exception
    {
        serializer = mockedSerializer.getComponentUnderTest();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyLicenseSerialization() throws Exception
    {
        serializer.serialize(new License());
    }


    @Test(expected=IllegalArgumentException.class)
    public void testNoLicenseeLicenseSerialization() throws Exception
    {
        License license = new License();
        license.addFeatureId(new LicensedFeatureId("test","1.0"));
        serializer.serialize(license);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNoLicensedItemLicenseSerialization() throws Exception
    {
        License license = new License();
        license.addLicenseeInfo("firstName","John");
        serializer.serialize(license);
    }

    @Test
    public void testMinimalLicenseSerialization() throws Exception
    {
        License license = new License();
        license.addFeatureId(new LicensedFeatureId("test"));
        license.addLicenseeInfo("firstName","John");
        license.addLicenseeInfo("lastName","Doe");
        license.addLicenseeInfo("email","john.doe@example.com");
        license.addLicenseeInfo("support","platinum");
        license.addLicenseeInfo("orderId","4701");
        // Deprecated field.
        license.addLicenseeInfo("name","foo");

        assertThat(serializer.serialize(license),
            equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"" + license.getId().toString() + "\">\n"
                + "    <modelVersion>2.0.0</modelVersion>\n"
                + "    <type>FREE</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <licencee>\n"
                + "        <firstName>John</firstName>\n"
                + "        <lastName>Doe</lastName>\n"
                + "        <email>john.doe@example.com</email>\n"
                + "        <meta key=\"support\">platinum</meta>\n"
                + "        <meta key=\"orderId\">4701</meta>\n"
                + "        <meta key=\"name\">foo</meta>\n"
                + "    </licencee>\n"
                + "</license>\n"
            )
        );
    }

    @Test
    public void testNoRestrictionLicenseSerialization() throws Exception
    {
        License license = new License();
        license.setId(new LicenseId("00000000-0000-0000-0000-000000000000"));
        license.setType(LicenseType.TRIAL);
        license.addFeatureId(new LicensedFeatureId("test-ui","1.0"));
        license.addLicenseeInfo("firstName","John");
        license.addLicenseeInfo("lastName","Doe");
        license.addLicenseeInfo("email","user@example.com");

        assertThat(serializer.serialize(license),
            equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>2.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test-ui</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <licencee>\n"
                + "        <firstName>John</firstName>\n"
                + "        <lastName>Doe</lastName>\n"
                + "        <email>user@example.com</email>\n"
                + "    </licencee>\n"
                + "</license>\n"
            )
        );
    }

    @Test
    public void testInstanceRestrictionLicenseSerialization() throws Exception
    {
        License license = new License();
        license.addFeatureId(new LicensedFeatureId("test","1.0"));
        license.addInstanceId(new InstanceId("11111111-2222-3333-4444-555555555555"));
        license.addLicenseeInfo("email","user@example.com");

        assertThat(serializer.serialize(license),
            equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"" + license.getId().toString() + "\">\n"
                + "    <modelVersion>2.0.0</modelVersion>\n"
                + "    <type>FREE</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <instances>\n"
                + "            <instance>11111111-2222-3333-4444-555555555555</instance>\n"
                + "        </instances>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <email>user@example.com</email>\n"
                + "    </licencee>\n"
                + "</license>\n"
            )
        );
    }

    @Test
    public void testDateRestrictionLicenseSerialization() throws Exception
    {
        License license = new License();
        license.addFeatureId(new LicensedFeatureId("test","1.0"));
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2017,Calendar.JUNE,2);
        license.setExpirationDate(calendar.getTimeInMillis());
        license.addLicenseeInfo("firstName","John");
        license.addLicenseeInfo("lastName","Doe");

        assertThat(serializer.serialize(license),
            equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"" + license.getId().toString() + "\">\n"
                + "    <modelVersion>2.0.0</modelVersion>\n"
                + "    <type>FREE</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <expire>" + dateFormatter.format(calendar.getTimeInMillis()) + "</expire>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <firstName>John</firstName>\n"
                + "        <lastName>Doe</lastName>\n"
                + "    </licencee>\n"
                + "</license>\n"
            )
        );
    }

    @Test
    public void testUserRestrictionLicenseSerialization() throws Exception
    {
        License license = new License();
        license.addFeatureId(new LicensedFeatureId("test","1.0"));
        license.setMaxUserCount(1000L);
        license.addLicenseeInfo("firstName","John");
        license.addLicenseeInfo("lastName","Doe");

        assertThat(serializer.serialize(license),
            equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"" + license.getId().toString() + "\">\n"
                + "    <modelVersion>2.0.0</modelVersion>\n"
                + "    <type>FREE</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test</id>\n"
                + "                <version>1.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <users>1000</users>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <firstName>John</firstName>\n"
                + "        <lastName>Doe</lastName>\n"
                + "    </licencee>\n"
                + "</license>\n"
            )
        );
    }

    @Test
    public void testRestrictedLicenseSerialization() throws Exception
    {
        License license = new License();
        license.setId(new LicenseId("00000000-0000-0000-0000-000000000000"));
        license.setType(LicenseType.TRIAL);
        license.addFeatureId(new LicensedFeatureId("test-api","2.0"));
        license.addInstanceId(new InstanceId("11111111-2222-3333-4444-555555555555"));
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(2017,Calendar.JUNE,2);
        license.setExpirationDate(calendar.getTimeInMillis());
        license.setMaxUserCount(100L);
        license.addLicenseeInfo("firstName","John");
        license.addLicenseeInfo("lastName","Doe");
        license.addLicenseeInfo("email","user@example.com");

        assertThat(serializer.serialize(license),
            equalTo("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<license xmlns=\"http://www.xwiki.com/license\" id=\"00000000-0000-0000-0000-000000000000\">\n"
                + "    <modelVersion>2.0.0</modelVersion>\n"
                + "    <type>TRIAL</type>\n"
                + "    <licensed>\n"
                + "        <features>\n"
                + "            <feature>\n"
                + "                <id>test-api</id>\n"
                + "                <version>2.0</version>\n"
                + "            </feature>\n"
                + "        </features>\n"
                + "    </licensed>\n"
                + "    <restrictions>\n"
                + "        <instances>\n"
                + "            <instance>11111111-2222-3333-4444-555555555555</instance>\n"
                + "        </instances>\n"
                + "        <expire>" + dateFormatter.format(calendar.getTimeInMillis()) + "</expire>\n"
                + "        <users>100</users>\n"
                + "    </restrictions>\n"
                + "    <licencee>\n"
                + "        <firstName>John</firstName>\n"
                + "        <lastName>Doe</lastName>\n"
                + "        <email>user@example.com</email>\n"
                + "    </licencee>\n"
                + "</license>\n"
            )
        );
    }
}
