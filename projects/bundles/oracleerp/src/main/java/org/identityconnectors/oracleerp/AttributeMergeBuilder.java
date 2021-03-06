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
package org.identityconnectors.oracleerp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
/**
 * Builder class to create a {@link ConnectorObject}.
 * 
 * Merge is done by joining the attribute unique values list
 */
final class AttributeMergeBuilder {
    private Set<String> _attrToGet = null;
    private Map<String, List<Object>> _attrs = new HashMap<String, List<Object>>();
    /**
     * @param attrToGet  the attribute merge filter
     */
    public AttributeMergeBuilder(Collection<String> attrToGet) {
        initAttributesToGet(attrToGet);
    }
    /**
     * @param names names attributes to get
     */
    public AttributeMergeBuilder(String... names) {
        initAttributesToGet(Arrays.asList(names));
    }
    /**
     * 
     */
    public AttributeMergeBuilder() {
        //empty
    }
    
    /**
     * @param attr 
     * @return this
     */
    public AttributeMergeBuilder setAttribute(Attribute attr) {
        if (attr != null) {
            _attrs.put(attr.getName(), attr.getValue());
        }
        return this;
    }
    /**
     * Adds each object in the collection.
     * 
     * @param name
     * @param obj
     * @return the builder
     */
    public AttributeMergeBuilder setAttribute(String name, Collection<?> obj) {
        if (obj != null) {
            _attrs.put(name, new ArrayList<Object>(obj));
        }
        return this;
    }
    /**
     * Adds values to the attribute.
     * 
     * @param name
     * @param objs
     * @return the builder
     */
    public AttributeMergeBuilder setAttribute(String name, Object... objs) {
        if (objs != null) {
            _attrs.put(name, objs == null ? null : Arrays.asList(objs));
        }
        return this;
    }    
    
    /**
     * @param attr 
     * @return this
     */
    public AttributeMergeBuilder addAttribute(Attribute attr) {
        if (attr != null) {
            mergeValue(attr.getName(), attr.getValue());
        }
        return this;
    }
    /**
     * Adds each object in the collection.
     * 
     * @param name
     * @param obj
     * @return the builder
     */
    public AttributeMergeBuilder addAttribute(String name, Collection<?> obj) {
        if (obj != null) {
            mergeValue(name, obj);
        }
        return this;
    }
    /**
     * Adds values to the attribute.
     * 
     * @param name
     * @param objs
     * @return the builder
     */
    public AttributeMergeBuilder addAttribute(String name, Object... objs) {
        mergeValue(name, objs == null ? null : Arrays.asList(objs));
        return this;
    }
    /**
     * @param merge
     */
    private void mergeValue(String name, Collection<?> merge) {
        if (!isInAttributesToGet(name)) {
            return;
        }
        List<Object> old = _attrs.get(name);
        if (old != null) {
            if (merge != null) {
                for (Object add : merge) {
                    if (add != null && !old.contains(add)) {
                        old.add(add);
                    }
                }
            }
        } else if (merge != null) {
            _attrs.put(name, CollectionUtil.newList(merge));
        } else {
            _attrs.put(name, null);
        }
    }
    /**
     * Builds a 'List<Attribute>' .
     * 
     * @return the list
     */
    public List<Attribute> build() {
        final List<Attribute> attrs = CollectionUtil.newList();
        for (Entry<String, List<Object>> entry : _attrs.entrySet()) {
            final String name = entry.getKey();
            final List<Object> value = entry.getValue();
            if (!isInAttributesToGet(name)) {
                continue;
            }
            final Attribute attr = AttributeBuilder.build(name, value);
            attrs.add(attr);
        }
        return attrs;
    }
    /**
     * 
     * @param name
     * @return boolean the attribute will be skipped
     */
    public boolean isInAttributesToGet(String name) {
        if (_attrToGet == null || _attrToGet.contains(name)) {
            return true;
        }
        return false;
    }
    /**
     * Builds a 'List<Attribute>' .
     * @param attrToGet 
     * 
     * @return the list
     */
    public List<Attribute> build(Collection<String> attrToGet) {
        initAttributesToGet(attrToGet);
        return build();
    }
    /**
     * @param attrToGet
     */
    private void initAttributesToGet(Collection<String> attrToGet) {
        if (attrToGet != null) {
            _attrToGet = CollectionUtil.newCaseInsensitiveSet();
            for (String toGet : attrToGet) {
                _attrToGet.add(toGet);
            }
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder bld = new StringBuilder();
        bld.append("Atttribute map:");
        bld.append(_attrs);
        bld.append(", active filter:");
        bld.append(_attrToGet);
        return bld.toString();
    }
    /**
     * @param name
     * @param index 
     * @param value 
     * @return internal list
     */
    public boolean hasExpectedValue(String name, int index, Object value) {
        Assertions.nullCheck(_attrs, "_attrs");
        final List<Object> list = _attrs.get(name);
        if (list == null || list.get(index) == null) {
            return false;
        }
        return list.get(index).equals(value);
    }
}
