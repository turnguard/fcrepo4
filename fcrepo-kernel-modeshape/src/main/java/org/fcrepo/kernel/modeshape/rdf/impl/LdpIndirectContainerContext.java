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
package org.fcrepo.kernel.modeshape.rdf.impl;

import java.util.stream.Stream;
import static java.util.stream.Stream.of;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.graph.Triple.create;
import org.apache.jena.rdf.model.Resource;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_MEMBER_RESOURCE;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author jakobitschj
 */
public class LdpIndirectContainerContext extends NodeRdfContext {
    private static final Logger LOGGER = getLogger(LdpIndirectContainerContext.class);
    private final ValueConverter valueConverter;

    /**
     * Default Constructor
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException in case of a repository exception
     */
    public LdpIndirectContainerContext(final FedoraResource resource,
            final IdentifierConverter<Resource, FedoraResource> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);
        final Node resourceAsNode = getJcrNode(resource);
        valueConverter = new ValueConverter(resourceAsNode.getSession(), translator());
        LOGGER.debug("EntityMembershipContext for: " + resourceAsNode);
        final Stream<Property> refs = Stream.concat(
                iteratorToStream(resourceAsNode.getReferences(LDP_MEMBER_RESOURCE)),
                iteratorToStream(resourceAsNode.getWeakReferences(LDP_MEMBER_RESOURCE))
        );
        final PropertyConverter pc = new PropertyConverter();
        refs.forEach((final Property p) -> {
            try {
                LOGGER.debug("references: " + p + " " + p.getNode());
                final Node indirectContainer = p.getParent();
                final Node ldpMembershipResource = p.getNode();
                final Property ldpInsertedContentRelation = indirectContainer
                        .getProperty(LDP_INSERTED_CONTENT_RELATION);
                final Property ldpHasMemberRelation = indirectContainer.getProperty(LDP_HAS_MEMBER_RELATION);
                
                LOGGER.debug("convert: " + nodeConverter().convert(indirectContainer));
                LOGGER.debug("ldpMembershipResource: " + nodeConverter().convert(ldpMembershipResource));
                LOGGER.debug("ldpInsertedContentRelation: " + createURI(ldpInsertedContentRelation.getString()));
                LOGGER.debug("ldpHasMemberRelation: " + createURI(ldpHasMemberRelation.getString()));
                LOGGER.debug("==>" + ldpInsertedContentRelation.getName());
                LOGGER.debug("==>" + ldpInsertedContentRelation.getPath());
                LOGGER.debug("==>" + pc.convert(ldpInsertedContentRelation));
                String insertedContentRelation = getPropertyNameFromPredicate(
                    resourceAsNode,
                    createResource(ldpInsertedContentRelation.getString()), null
                );
                indirectContainer.accept(new ItemVisitor() {
                    @Override
                    public void visit(final Property property) throws RepositoryException {
                    }
                    @Override
                    public void visit(final Node node) throws RepositoryException {
                        if (node.hasProperty(insertedContentRelation.concat("_ref"))) {
                            LOGGER.debug("got: " + node);
                            Stream.of(node.getProperty(insertedContentRelation.concat("_ref"))
                                    .getValues()).forEach(id->{
                                try {
                                    Node gotit = resourceAsNode.getSession().getNodeByIdentifier(id.getString());
                                    LOGGER.debug("GOT IT!! " + gotit);
                                    concat(of(
                                        create(
                                                createURI(nodeConverter().convert(ldpMembershipResource).getURI()),
                                                createURI(ldpHasMemberRelation.getString()),
                                                createURI(nodeConverter().convert(gotit).getURI())
                                    )));
                                } catch (RepositoryException ex) {
                                }
                                    });
                        }
                        final NodeIterator children = node.getNodes();
                        while (children.hasNext()) {
                            visit(children.nextNode());
                        }
                    }
                });
            } catch (ValueFormatException ex) {
                LOGGER.warn("", ex);
            } catch (RepositoryException ex) {
                LOGGER.warn("", ex);
            }
        });
    }
}
