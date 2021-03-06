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
using System;

namespace Org.IdentityConnectors.Common
{
    /// <summary>
    /// Description of DateTimeUtil.
    /// </summary>
    public static class DateTimeUtil
    {
        public static DateTime GetDateTimeFromUtcMillis(long dateTime)
        {
            return DateTime.FromFileTimeUtc(dateTime * 10000);
        }

        public static long GetUtcTimeMillis(DateTime dateTime)
        {
            return dateTime.ToFileTimeUtc() / 10000;
        }

        public static long GetCurrentUtcTimeMillis()
        {
            return GetUtcTimeMillis(DateTime.Now);
        }
    }
}
