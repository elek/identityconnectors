/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.solaris.operation.search.nodes;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.solaris.attr.NativeAttribute;
import org.identityconnectors.solaris.operation.search.SolarisEntry;



/** node of search filter tree for Solaris */
public interface Node {
    public static class Traverser {
        public static Set<NativeAttribute> collectAttributeNames(Node node) {
            Set<NativeAttribute> result = new HashSet<NativeAttribute>();
            if (node instanceof BinaryOpNode) {
                BinaryOpNode binNode = (BinaryOpNode) node;
                result.addAll(collectAttributeNames(binNode.getLeft()));
                result.addAll(collectAttributeNames(binNode.getRight()));
            } else if (node instanceof AttributeNode) {
                result.add(((AttributeNode) node).getAttributeName());
            }
            return result;
        }
    }
    
    public abstract boolean evaluate(SolarisEntry entry);
}