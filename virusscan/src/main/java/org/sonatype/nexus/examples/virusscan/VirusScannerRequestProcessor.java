/*
 * Copyright (c) 2007-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package org.sonatype.nexus.examples.virusscan;

import com.google.common.annotations.VisibleForTesting;
import org.sonatype.nexus.logging.AbstractLoggingComponent;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RequestProcessor;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Virus scanning {@link RequestProcessor}.
 *
 * @since 1.0
 */
@Named(VirusScannerRequestProcessor.ID)
public class VirusScannerRequestProcessor
    extends AbstractLoggingComponent
    implements RequestProcessor
{
    public static final String ID = "virus-scanner";

    private final EventBus eventBus;

    private final List<VirusScanner> scanners;

    @Inject
    public VirusScannerRequestProcessor(final EventBus eventBus,
                                        final List<VirusScanner> scanners)
    {
        this.eventBus = checkNotNull(eventBus);
        this.scanners = checkNotNull(scanners);
    }

    @VisibleForTesting
    boolean hasVirus(final StorageFileItem item) {
        getLogger().debug("Scanning item for viruses: {}", item.getPath());

        boolean infected = false;
        for (VirusScanner scanner : scanners) {
            if (scanner.hasVirus(item)) {
                infected = true;
                eventBus.post(new InfectedItemFoundEvent(item.getRepositoryItemUid().getRepository(), item));
            }
        }

        if (infected) {
            getLogger().warn("Infection detected in item: {}", item.getPath());
        }

        return infected;
    }

    @Override
    public boolean process(final Repository repository, final ResourceStoreRequest request, final Action action) {
        // don't decide until have content
        return true;
    }

    @Override
    public boolean shouldProxy(final ProxyRepository repository, final ResourceStoreRequest request) {
        // don't decide until have content
        return true;
    }

    @Override
    public boolean shouldRetrieve(final Repository repository, final ResourceStoreRequest request, final StorageItem item)
        throws IllegalOperationException, ItemNotFoundException, AccessDeniedException
    {
        // don't decide until have content
        return true;
    }

    @Override
    public boolean shouldCache(final ProxyRepository repository, final AbstractStorageItem item) {
        if (item instanceof StorageFileItem) {
            StorageFileItem file = (StorageFileItem) item;
            return !hasVirus(file);
        }
        else {
            return true;
        }
    }
}
