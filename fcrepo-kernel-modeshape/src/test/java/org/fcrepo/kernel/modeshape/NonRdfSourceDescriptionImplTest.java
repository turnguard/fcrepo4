/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.modeshape.jcr.api.ValueFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import java.io.InputStream;
import java.util.Calendar;
import java.time.Instant;

import static org.fcrepo.kernel.modeshape.NonRdfSourceDescriptionImpl.hasMixin;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_CREATED;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.JCR_LASTMODIFIED;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>{@link NonRdfSourceDescriptionImplTest} class.</p>
 *
 * @author ksclarke
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NonRdfSourceDescriptionImplTest implements FedoraTypes {

    private static final String testDsId = "testDs";

    private NonRdfSourceDescription testObj;

    @Mock
    private Session mockSession;

    @Mock
    private Node mockRootNode, mockDsNode;

    @Mock
    private InputStream mockStream;

    @Mock
    private ValueFactory mockVF;

    @Mock
    private NodeType mockDsNodeType;

    @Before
    public void setUp() {
        final NodeType[] nodeTypes = new NodeType[] { mockDsNodeType };
        try {
            when(mockDsNodeType.getName()).thenReturn(FEDORA_NON_RDF_SOURCE_DESCRIPTION);
            when(mockDsNode.getMixinNodeTypes()).thenReturn(nodeTypes);
            when(mockDsNode.getName()).thenReturn(testDsId);
            when(mockDsNode.getSession()).thenReturn(mockSession);
            final NodeType mockNodeType = mock(NodeType.class);
            when(mockNodeType.getName()).thenReturn("nt:file");
            when(mockDsNode.getPrimaryNodeType()).thenReturn(mockNodeType);
            testObj = new NonRdfSourceDescriptionImpl(mockDsNode);
        } catch (final RepositoryException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        mockSession = null;
        mockRootNode = null;
        mockDsNode = null;
    }

    @Test
    public void testGetNode() {
        assertEquals(getJcrNode(testObj), mockDsNode);
    }

    @Test
    public void testGetCreatedDate() throws RepositoryException {
        final Calendar cal = Calendar.getInstance();
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(cal);
        when(mockDsNode.hasProperty(JCR_CREATED)).thenReturn(true);
        when(mockDsNode.getProperty(JCR_CREATED)).thenReturn(mockProp);
        final Instant actual = testObj.getCreatedDate();
        assertEquals(cal.getTimeInMillis(), actual.toEpochMilli());
    }

    @Test
    public void testGetLastModifiedDate() throws RepositoryException {
        final Calendar cal = Calendar.getInstance();
        final Property mockProp = mock(Property.class);
        when(mockProp.getDate()).thenReturn(cal);
        when(mockDsNode.hasProperty(JCR_LASTMODIFIED)).thenReturn(true);
        when(mockDsNode.getProperty(JCR_LASTMODIFIED)).thenReturn(mockProp);
        final Instant actual = testObj.getLastModifiedDate();
        assertEquals(cal.getTimeInMillis(), actual.toEpochMilli());
    }

    @Test
    public void testHasDatastreamMixin() throws RepositoryException {
        final Node test = mock(Node.class);
        when(test.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(true);
        assertTrue("mockYes should have mixin", hasMixin(test));
    }

    @Test
    public void testHasNoMixin() throws RepositoryException {
        final Node test = mock(Node.class);
        when(test.isNodeType(FEDORA_NON_RDF_SOURCE_DESCRIPTION)).thenReturn(false);
        assertFalse("mockNo should not have mixin", hasMixin(test));
    }

}
