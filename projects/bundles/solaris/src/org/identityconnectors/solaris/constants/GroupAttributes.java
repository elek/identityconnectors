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
package org.identityconnectors.solaris.constants;

import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;

/**
 * List of allowed GROUP attribute names.
 * @author David Adam
 */
public enum GroupAttributes {
 // TODO add command line switches that are used for altering these
 // attributes
    GROUPNAME("groupName", UpdateSwitches.UNKNOWN),
    GID("gid", UpdateSwitches.UNKNOWN),
    USERS("users", UpdateSwitches.UNKNOWN);

    private static final Map<String, GroupAttributes> map = CollectionUtil.newCaseInsensitiveMap();
    
    static {
        for (GroupAttributes value : GroupAttributes.values()) {
            map.put(value.attrName, value);
        }
    }
    
    /** name of the attribute, it comes from the adapter's schema (prototype xml) */
    private String attrName;
    /** the command line switch used by the solaris connector */
    private UpdateSwitches cmdSwitch;
    
    private GroupAttributes(String attrName, UpdateSwitches cmdSwitch) {
        this.attrName = attrName;
        this.cmdSwitch = cmdSwitch;
    }
    
    /**
     * Translate the Group's attribute name to item from list of allowed
     * group attributes.
     * @return the name of attribute, or null if it doesn't exist.
     */
    public static GroupAttributes fromGroupName(String s) {
        return map.get(s);
    }
    
    /** 
     * generates the command line switch for the attribute 
     * @param argument the argument used after the switch
     */
    public static String formatCommandSwitch(Attribute argument) {
        return AttributeHelper.formatCommandSwitch(argument, getCmdSwitch(argument));
    }
    
    private static String getCmdSwitch(Attribute argument) {
        return fromGroupName(argument.getName()).getCmdSwitch();
    }
    
    /** @return the command line switch */
    String getCmdSwitch() {
        return cmdSwitch.getCmdSwitch();
    }
}
