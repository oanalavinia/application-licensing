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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link DefaultLicensedExtensionManager}.
 * 
 * @version $Id$
 */
public class DefaultLicensedExtensionManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultLicensedExtensionManager> mocker =
        new MockitoComponentMockingRule<>(DefaultLicensedExtensionManager.class);

    InstalledExtensionRepository installedExtensionRepository;

    Map<String, Collection<InstalledExtension>> licensorDependencies;

    InstalledExtension licensorExtension;

    InstalledExtension pollsExtension;

    InstalledExtension flashV1Extension;

    ExtensionDependency flashV1Dependency;

    InstalledExtension flashV2Extension;

    ExtensionId flashV2ExtensionId;

    List<String> pollsNamespaces;

    @Before
    public void configure() throws Exception
    {
        this.installedExtensionRepository = this.mocker.getInstance(InstalledExtensionRepository.class);

        this.licensorDependencies = new HashMap<>();
        this.licensorExtension = mock(InstalledExtension.class);
        when(this.installedExtensionRepository
            .getInstalledExtension(DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null))
                .thenReturn(this.licensorExtension);

        this.pollsExtension = mock(InstalledExtension.class);
        ExtensionId pollsExtensionId = new ExtensionId("application-xpoll", "1.0");
        when(this.pollsExtension.getId()).thenReturn(pollsExtensionId);
        this.pollsNamespaces = Arrays.asList("main");
        when(this.pollsExtension.getNamespaces()).thenReturn(this.pollsNamespaces);

        this.flashV1Extension = mock(InstalledExtension.class);
        ExtensionId flashV1ExtensionId = new ExtensionId("application-flash", "1.1");
        when(this.flashV1Extension.getId()).thenReturn(flashV1ExtensionId);
        this.flashV1Dependency = mock(ExtensionDependency.class);
        when(this.flashV1Dependency.getId()).thenReturn(flashV1ExtensionId.getId());
        when(this.flashV1Extension.getNamespaces()).thenReturn(this.pollsNamespaces);

        this.flashV2Extension = mock(InstalledExtension.class);
        this.flashV2ExtensionId = new ExtensionId("application-flash", "2.1");
        when(this.flashV2Extension.getId()).thenReturn(this.flashV2ExtensionId);
        ExtensionDependency flashV2Dependency = mock(ExtensionDependency.class);
        when(flashV2Dependency.getId()).thenReturn(this.flashV2ExtensionId.getId());

        when(this.installedExtensionRepository.getInstalledExtension(pollsExtensionId)).thenReturn(this.pollsExtension);
        when(this.installedExtensionRepository.getInstalledExtension(pollsExtensionId.getId(),
            this.pollsNamespaces.get(0))).thenReturn(this.pollsExtension);

        Collection<ExtensionDependency> pollsDependencies = Arrays.asList(this.flashV1Dependency);
        when(this.pollsExtension.getDependencies()).thenReturn(pollsDependencies);

        when(this.installedExtensionRepository.getInstalledExtension(this.flashV1Dependency.getId(),
            this.pollsNamespaces.get(0))).thenReturn(this.flashV1Extension);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV1Extension.getId()))
            .thenReturn(this.flashV1Extension);

    }

    @Test
    public void getMandatoryLicensedExtensions() throws Exception
    {

        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension, this.flashV1Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId()))
            .thenReturn(this.licensorDependencies);

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        Set<ExtensionId> result = this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsWithDifferentVersionOnNamespaces() throws Exception
    {
        this.licensorDependencies.put(null,
            Arrays.asList(this.pollsExtension, this.flashV1Extension, this.flashV2Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId()))
            .thenReturn(this.licensorDependencies);

        List<String> flashV2Namespaces = Arrays.asList("wiki2");
        when(this.flashV2Extension.getNamespaces()).thenReturn(flashV2Namespaces);
        when(this.installedExtensionRepository.getInstalledExtension(this.flashV2ExtensionId.getId(),
            flashV2Namespaces.get(0))).thenReturn(this.flashV2Extension);

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());
        expected.add(this.flashV2ExtensionId);

        Set<ExtensionId> result = this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsWithTransitivePaidDependency() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension, this.flashV1Extension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId()))
            .thenReturn(this.licensorDependencies);

        InstalledExtension freeExtension = mock(InstalledExtension.class);
        ExtensionId freeExtensionId = new ExtensionId("application-free", "1.1");
        when(freeExtension.getId()).thenReturn(freeExtensionId);
        ExtensionDependency freeExtensionDependency = mock(ExtensionDependency.class);
        when(freeExtensionDependency.getId()).thenReturn(freeExtensionId.getId());
        when(this.installedExtensionRepository.getInstalledExtension(freeExtensionDependency.getId(),
            this.pollsNamespaces.get(0))).thenReturn(freeExtension);

        when(this.pollsExtension.getDependencies()).thenReturn(Arrays.asList(freeExtensionDependency));
        when(freeExtension.getDependencies()).thenReturn(Arrays.asList(this.flashV1Dependency));

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        Set<ExtensionId> result = this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions();

        assertEquals(expected, result);
    }

    @Test
    public void getMandatoryLicensedExtensionsIsCached() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId()))
            .thenReturn(this.licensorDependencies);
        when(this.pollsExtension.getDependencies()).thenReturn(Collections.<ExtensionDependency>emptyList());

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        assertEquals(expected, this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions());
        assertEquals(expected, this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions());

        verify(this.installedExtensionRepository, times(1))
            .getInstalledExtension(DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null);
    }

    @Test
    public void invalidateMandatoryLicensedExtensionsCache() throws Exception
    {
        this.licensorDependencies.put(null, Arrays.asList(this.pollsExtension));
        when(this.installedExtensionRepository.getBackwardDependencies(this.licensorExtension.getId()))
            .thenReturn(this.licensorDependencies);
        when(this.pollsExtension.getDependencies()).thenReturn(Collections.<ExtensionDependency>emptyList());

        Set<ExtensionId> expected = new HashSet<>();
        expected.add(this.pollsExtension.getId());

        assertEquals(expected, this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions());
        this.mocker.getComponentUnderTest().invalidateMandatoryLicensedExtensionsCache();
        assertEquals(expected, this.mocker.getComponentUnderTest().getMandatoryLicensedExtensions());

        verify(this.installedExtensionRepository, times(2))
            .getInstalledExtension(DefaultLicensedExtensionManager.LICENSOR_EXTENSION_ID, null);
    }

}