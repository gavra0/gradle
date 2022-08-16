/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.execution.plan;

import com.google.common.collect.ImmutableSortedSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

import static org.gradle.execution.plan.NodeSets.newSortedNodeSet;

/**
 * Maintains the state for the dependencies of a node.
 */
public class NodeDependencySet {
    private NavigableSet<Node> orderedDependencies;
    private Set<Node> waitingFor;
    private boolean hasFailures;
    private boolean pruned;

    public NavigableSet<Node> getOrderedNodes() {
        if (orderedDependencies == null) {
            return ImmutableSortedSet.of();
        } else {
            return orderedDependencies;
        }
    }

    public void addDependency(Node node) {
        if (orderedDependencies == null) {
            orderedDependencies = newSortedNodeSet();
        }
        orderedDependencies.add(node);
        if (waitingFor == null) {
            waitingFor = new HashSet<>();
        }
        pruned = false;
        waitingFor.add(node);
    }

    public void onNodeComplete(Node node) {
        if (waitingFor != null) {
            if (waitingFor.remove(node)) {
                if (!node.isSuccessful()) {
                    hasFailures = true;
                    waitingFor = null;
                }
            }
        }
    }

    public Node.DependenciesState getState() {
        if (!pruned) {
            if (waitingFor != null) {
                Iterator<Node> iterator = waitingFor.iterator();
                while (iterator.hasNext()) {
                    Node node = iterator.next();
                    if (node.isComplete()) {
                        iterator.remove();
                        if (!node.isSuccessful()) {
                            hasFailures = true;
                            waitingFor = null;
                            break;
                        }
                    }
                }
            }
            pruned = true;
        }
        if (hasFailures) {
            return Node.DependenciesState.COMPLETE_AND_NOT_SUCCESSFUL;
        } else if (waitingFor == null || waitingFor.isEmpty()) {
            return Node.DependenciesState.COMPLETE_AND_SUCCESSFUL;
        } else {
            return Node.DependenciesState.NOT_COMPLETE;
        }
    }
}
