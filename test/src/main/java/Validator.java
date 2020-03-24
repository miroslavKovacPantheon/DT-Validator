/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

public class Validator {

    private static final YangParserFactory PARSER_FACTORY;

    public Validator() throws IOException, YangParserException, DataValidationFailedException {
        main(new String[]{});
    }

    static {
        final Iterator<YangParserFactory> it = ServiceLoader.load(YangParserFactory.class).iterator();
        if (!it.hasNext()) {
            throw  new IllegalStateException("No yang parser factory found");
        }
        PARSER_FACTORY = it.next();
    }

    public static void main(String[] args) throws IOException, YangParserException, DataValidationFailedException {
        final YangParser parser = PARSER_FACTORY.createParser();
        YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.forResource("/all_the_modules/ietf-inet-types@2013-07-15.yang");
        YangTextSchemaSource yangTextSchemaSource2 = YangTextSchemaSource.forResource("/mainSources/ietf-network@2018-02-26.yang");
        YangTextSchemaSource yangTextSchemaSource3 = YangTextSchemaSource.forResource("/mainSources/ietf-network-topology@2018-02-26.yang");

        parser.addSource(yangTextSchemaSource3);
        parser.addSource(yangTextSchemaSource2);
        parser.addLibSource(yangTextSchemaSource);

        final SchemaContext schemaContext = parser.buildSchemaContext();

        final Module module = schemaContext.findModule("ietf-network-topology", Revision.of("2018-02-26")).get();
        final String name = module.getName();
        final Set<AugmentationSchemaNode> augmentations = module.getAugmentations();
        final Set<GroupingDefinition> groupings = module.getGroupings();
        final Set<RpcDefinition> rpcs = module.getRpcs();
        final DataSchemaNode dataSchemaNode = module.findDataTreeChild(
                QName.create(module.getQNameModule(), "cont"),
                QName.create(module.getQNameModule(), "cont2"),
                QName.create(module.getQNameModule(), "foo")).get();

        final DataTree dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION);
        dataTree.setSchemaContext(schemaContext);

        final MapEntryNode mapEntryNode = Builders.mapEntryBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                        QName.create(module.getQNameModule(), "list1"), QName.create(module.getQNameModule(), "name"), "sample name"
                )
        ).withChild(
                Builders.leafBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(QName.create(module.getQNameModule(), "name")))
                        .withValue("sample name").build()
        ).build();

        final MapNode list1 = Builders.mapBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(QName.create(module.getQNameModule(), "list1")))
                .withChild(mapEntryNode).build();

        final ContainerNode cont2 = Builders.containerBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(QName.create(module.getQNameModule(), "cont2"))
        ).withChild(list1).build();

        final ContainerNode rootContainer = Builders.containerBuilder().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(QName.create(module.getQNameModule(), "cont"))
        ).withChild(cont2).build();

        final DataTreeSnapshot dataBefore = dataTree.takeSnapshot();
        final DataTreeModification dataTreeModification = dataTree.takeSnapshot().newModification();
        dataTreeModification.write(YangInstanceIdentifier.of(QName.create(module.getQNameModule(), "cont")),
                rootContainer);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        dataTree.commit(dataTree.prepare(dataTreeModification));

        final DataTreeSnapshot dataTreeSnapshotAfter = dataTree.takeSnapshot();


        schemaContext.getModules();

    }

}
