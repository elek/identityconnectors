/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.oracleerp;

import java.text.MessageFormat;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Implements the {@link Configuration} interface to provide all the necessary
 * parameters to initialize the OracleErp Connector.
 *
 * @version 1.0
 * @since 1.0
 */
public class OracleERPConfiguration extends AbstractConfiguration {

    /** */
    private static final String DEFAULT_DATA_SOURCE = "jdbc/SampleDataSourceName";
    /*
     * Set up base configuration elements
     */

    /**
     * Datasource attributed
     * The attribute has precedence over other databaseName connection related attributes.
     * 
     * imported adapter attribute
     * name="dataSource" type="string" multi="false" value="jdbc/SampleDataSourceName"
     * displayName="DATA_SOURCE_NAME" description="HELP_393"
     */
    String dataSource=DEFAULT_DATA_SOURCE;

    /**
     * Getter for the driver dataSource.
     * @return dataSource
     * TODO add support for dataSource in the future
     */
//    @ConfigurationProperty(displayMessageKey="DATA_SOURCE_DISPLAY", helpMessageKey="DATA_SOURCE_DISPLAY",  order=1)
//    public String getDataSource() {
//        return dataSource;
//    }

    /**
     * Setter for the dataSource attribute.
     * @param dataSource 
     */
//    public void setDataSource(String dataSource) {
//        this.dataSource = dataSource;
//    }    

    /** */
    private static final String DEFAULT_DRIVER = "oracle.jdbc.driver.OracleDriver";

    /**
     * Driver attribute,
     * Ignored if <b>dataSource</b> attribute or <b>connUrl</b> attribute is specified
     * 
     * imported adapter attribute
     * name="driver" type="string" multi="false" value="oracle.jdbc.driver.OracleDriver"
     * displayName="DRIVER" description="HELP_369"
     */
    private String driver = DEFAULT_DRIVER;

    /**
     * Getter for the driver attribute.
     * @return driver
     */
    @ConfigurationProperty(displayMessageKey="DRIVER_DISPLAY", helpMessageKey="DRIVER_HELP",  order=1)
    public String getDriver() {
        return driver;
    }

    /**
     * Setter for the driver attribute.
     * @param driver 
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }    

    /** */
    private static final String ORACLE_THIN_CONN_URL = "java:oracle:thin:@{0}:{1}:{2}";

    /** */
    private static final String DEFAULT_CONN_URL = "java:oracle:thin:@HOSTNAME:PORT:DB";
    
    /**
     * Database connection url
     * Ignored if <b>dataSource</b> attribute is specified
     * 
     * imported adapter attribute
     * name="connUrl" type="string" multi="false" value="java:oracle:thin:@HOSTNAME:PORT:DB" 
     * displayName="CONN_URL" description="HELP_394"   
     */ 
    private String connUrl = DEFAULT_CONN_URL;

    
    /**
     * Getter for the connUrl attribute. 
     * @return connUrl
     */
    @ConfigurationProperty(displayMessageKey="CONN_URL_DISPLAY", helpMessageKey="CONN_URL_HELP",  order=2)
    public String getConnUrl() {
        return connUrl;
    }
    
    /**
     * Setter for the connUrl attribute.
     * @param connUrl 
     */
    public void setConnUrl(String connUrl) {
        this.connUrl = connUrl;
    }

    /**
     * Host attribute
     * Ignored if <b>dataSource</b> attribute or <b>connUrl</b> attribute is specified
     * 
     * imported adapter attribute
     * name="hostName" type="string" multi="false"
     * displayName="HOST"  description="HELP_239"
     */
    private String hostName;

    /**
     * Getter for the connUrl attribute. 
     * @return connUrl
     */
    @ConfigurationProperty(displayMessageKey="HOST_NAME_DISPLAY", helpMessageKey="HOST_NAME_HELP",  order=3)
    public String getHostName() {
        return hostName;
    }

    /**
     * Setter for the hostName attribute.
     * @param hostName attribute.
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /** */
    private static final String DEFAULT_PORT = "1521";

    /**
     * Port attribute
     * Ignored if <b>dataSource</b> attribute or <b>connUrl</b> attribute is specified
     * 
     * imported adapter attribute
     * name="port" type="string" multi="false" value="1521"
     * displayName="PORT" description="HELP_269" +
     */
    private String port = DEFAULT_PORT;
    
    /**
     * Getter for the port attribute. 
     * @return port
     */
    @ConfigurationProperty(displayMessageKey="PORT_DISPLAY", helpMessageKey="PORT_HELP",  order=4)
    public String getPort() {
        return port;
    }

    /**
     * Setter for the port attribute.
     * @param port attribute.
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * Database attribute
     * Ignored if <b>dataSource</b> attribute or <b>connUrl</b> attribute is specified
     * 
     * imported adapter attribute
     * name="databaseName" type="string" multi="false"
     * displayName="DATABASE" description="HELP_80"
     */
    private String databaseName;

    /**
     * Getter for the databaseName attribute. 
     * @return databaseName
     */
    @ConfigurationProperty(displayMessageKey="DATABASE_NAME_DISPLAY", helpMessageKey="DATABASE_NAME_HELP",  order=5)
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Setter for the databaseName attribute.
     * @param databaseName attribute.
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    
    /** */
    private static final String DEFAULT_USER = "APPL";
    
    /**
     * User attribute
     * 
     * imported adapter attribute
     * name="user" displayName="USER" type="string" multi="false"
     * description="HELP_286"  value="APPL"
     */
    private String user = DEFAULT_USER;

    /**
     * Getter for the user attribute. 
     * @return user
     */
    @ConfigurationProperty(displayMessageKey="USER_DISPLAY", helpMessageKey="USER_HELP",  order=6)
    public String getUser() {
        return user;
    }

    /**
     * Setter for the user attribute.
     * @param user attribute.
     */
    public void setUser(String user) {
        this.user = user;
    }
    
    /**
     * Password attribute
     * 
     * imported adapter attribute
     * name="password" type="encrypted" multi="false"
     * displayName="PASSWORD" description="HELP_262"
     */
    private GuardedString password;
    
    /**
     * Getter for the user attribute. 
     * @return user
     */
    @ConfigurationProperty(displayMessageKey="PASSWORD_DISPLAY", helpMessageKey="PASSWORD_HELP",  order=7, confidential=true)
    public GuardedString getPassword() {
        return password;
    }
    
    /**
     * Setter for the password attribute.
     * @param password attribute.
     */
    public void setPassword(GuardedString password) {
        this.password = password;
    }    
    
    /*
     * implemented by framework, left as comment for the reference
     * name="useConnectionPool" type="string" multi="false" value="FALSE"
     * displayName="CONN_POOLING" description="HELP_395"
     */ 
    
    /*
     * implemented by framework, left as comment for the reference
     * name="idleTimeout" type="string" multi="false" value="" + IDLE_TIMEOUT
     * displayName="DBPOOL_IDLETIMEOUT" description="DBPOOL_IDLETIMEOUT_HELP"
     */ 
       
    /*
     * implemented by framework, left as comment for the reference
     * name="encryptionTypesClient"  type="string" multi="false"  value="RC4_128"
     * displayName="ENCRYPTION_TYPES_CLIENT" description="HELP_ORACLE_ERP_CLIENT_ENCRYPTION_ALGORITHMS"
     */ 
    
    /*
     * implemented by framework, left as comment for the reference
     * name="encryptionClient" type="string" multi="false" value="ACCEPTED"
     * displayName="ENCRYPTION_CLIENT" description="HELP_ORACLE_ERP_CLIENT_ENCRYPTION_LEVEL"
     */  
    
    /**
     * Audit Responsibility attribute,  desired Oracle ERP responsibility
     * 
     * imported adapter attribute
     * name="auditResponsibility"  type="string" multi="false"
     * displayName="AUDIT_RESPONSIBILITY" description="HELP_ORACLE_ERP_ADMIN_USER_RESPONSIBILITY"
     */
    private String auditResponsibility="";

    /**
     * Getter for the user attribute. 
     * @return user
     */
    @ConfigurationProperty(displayMessageKey="AUDIT_RESPONSIBILITY_DISPLAY", helpMessageKey="AUDIT_RESPONSIBILITY_HELP",  order=8)
    public String getAuditResponsibility() {
        return auditResponsibility;
    }

    /**
     * Setter for the auditResponsibility attribute.
     * @param auditResponsibility attribute.
     */
    public void setAuditResponsibility(String auditResponsibility) {
        this.auditResponsibility = auditResponsibility;
    }    
    
    /**
     * Manage Securing Attributes attribute
     * TRUE to manage securing attributes, or FALSE to ignore the schema attribute
     * 
     * imported adapter attribute
     * name="manageSecuringAttrs" type="string" multi="false" value="TRUE" 
     * displayName="MANAGE_SECURING_ATTRS" description="HELP_ORACLE_ERP_MANAGE_SECURING_ATTRS"
     */
    private boolean manageSecuringAttrs=true; 

    /**
     * Getter for the manageSecuringAttrs attribute.
     * @return manageSecuringAttrs attribute
     */
    @ConfigurationProperty(displayMessageKey="MANAGE_SECURING_ATTRS_DISPLAY", helpMessageKey="MANAGE_SECURING_ATTRS_HELP",  order=9)
    public boolean isManageSecuringAttrs() {
        return manageSecuringAttrs;
    }

    /**
     * Setter for the manageSecuringAttrs attribute.
     * @param manageSecuringAttrs attribute.
     */
    public void setManageSecuringAttrs(boolean manageSecuringAttrs) {
        this.manageSecuringAttrs = manageSecuringAttrs;
    }    
    
    /**
     * Return the Set of Books and/or Organization associated with auditor responsibility
     * false will increase performance
     * 
     * imported adapter attribute
     * name="returnSobOrgAttrs" type="string" multi="false" value="FALSE"
     * displayName="RETURN_SOB_AND_ORG" description="HELP_ORACLE_ERP_RETURN_SOB_AND_ORG_ATTRS"
     */
    private boolean returnSobOrgAttrs = false;

    /**
     * Getter for the returnSobOrgAttrs attribute.
     * @return returnSobOrgAttrs attribute
     */
    @ConfigurationProperty(displayMessageKey="RETURN_SOB_AND_ORG_DISPLAY", helpMessageKey="RETURN_SOB_AND_ORG_HELP",  order=10)
    public boolean isReturnSobOrgAttrs() {
        return returnSobOrgAttrs;
    }

    /**
     * Setter for the returnSobOrgAttrs attribute.
     * @param returnSobOrgAttrs attribute.
     */
    public void setReturnSobOrgAttrs(boolean returnSobOrgAttrs) {
        this.returnSobOrgAttrs = returnSobOrgAttrs;
    }    
    
    /**
     * Set to a value to limit accounts returned
     * If true, then only accounts with START_DATE and END_DATE spanning SYSDATE are returned. 
     * The default value is false; in this case, all accounts on the resource are returned.
     * 
     * imported adapter attribute
     * name="activeAccountsOnly"  type="string" multi="false" value="FALSE"
     * displayName="ORACLE_ERP_ACTIVE_ACCOUNTS_ONLY" description="HELP_ORACLE_ERP_ACTIVE_ACCOUNTS_ONLY"
     */  
    private boolean activeAccountsOnly=false;

    /**
     * Getter for the activeAccountsOnly attribute.
     * @return activeAccountsOnly attribute
     */
    @ConfigurationProperty(displayMessageKey="ACTIVE_ACCOUNTS_ONLY_DISPLAY", helpMessageKey="ACTIVE_ACCOUNTS_ONLY_HELP",  order=11)
    public boolean isActiveAccountsOnly() {
        return activeAccountsOnly;
    }

    /**
     * Setter for the activeAccountsOnly attribute.
     * @param activeAccountsOnly attribute.
     */
    public void setActiveAccountsOnly(boolean activeAccountsOnly) {
        this.activeAccountsOnly = activeAccountsOnly;
    }    
    
    /**
     * Parameter indicate to limit which accounts to be managed by IDM. 
     * The limitation is by adding WHERE clause
     * If enabled, 'Active Accounts Only' parameter is ignored.
     * Default value is false
     * 
     * imported adapter attribute
     * name="ACCOUNTS_INCLUDED" type="string" multi="false"
     * displayName="ORACLE_ERP_ACCOUNTS_INCLUDED" description="ORACLE_ERP_ACCOUNTS_INCLUDED_HELP"
     */
    private boolean accountsIncluded=false;

    /**
     * Getter for the accountsIncluded attribute.
     * @return accountsIncluded attribute
     */
    @ConfigurationProperty(displayMessageKey="ACCOUNTS_INCLUDED_DISPLAY", helpMessageKey="ACCOUNTS_INCLUDED_HELP",  order=12)
    public boolean isAccountsIncluded() {
        return accountsIncluded;
    }

    /**
     * Setter for the accountsIncluded attribute.
     * @param accountsIncluded attribute.
     */
    public void setAccountsIncluded(boolean accountsIncluded) {
        this.accountsIncluded = accountsIncluded;
    }    
        
    /**
     * Enter the name of the resource action that contains the script
     * used to retrieve additional custom account attributes 
     * for a user from this resource
     * 
     * imported adapter attribute
     * name="GetUser Actions"  type="string" multi="false" required="false"
     * displayName="GETUSER_AFTER_ACTION" description="GETUSER_AFTER_ACTION_HELP"
     */     
    private String userActions = "";

    /**
     * Getter for the userActions attribute.
     * @return userActions attribute
     */
    @ConfigurationProperty(displayMessageKey="AFTER_ACTION_DISPLAY", helpMessageKey="AFTER_ACTION_HELP",  order=13)
    public String getUserActions() {
        return userActions;
    }

    /**
     * Setter for the userActions attribute.
     * @param userActions attribute.
     */
    public void setUserActions(String userActions) {
        this.userActions = userActions;
    }    
    
    /**
     * When true, the schema identifier will not be prefixed to table names. 
     * When false, a schema identifier is prefixed to tables names. 
     * Defaults to false.
     * 
     * imported adapter attribute
     * name='noSchemaId' type='string' multi='false' required='false'
     * description='HELP_NO_SCHEMA_ID" value='FALSE'
     */
    private boolean noSchemaId = false;

    
    /**
     * Getter for the noSchemaId attribute.
     * @return the noSchemaId value
     */
    @ConfigurationProperty(displayMessageKey="NO_SCHEMA_ID_DISPLAY", helpMessageKey="NO_SCHEMA_ID_HELP",  order=14)
    public boolean isNoSchemaId() {
        return noSchemaId;
    }

    /**
     * Setter for the noSchemaId attribute.
     * @param noSchemaId
     */
    public void setNoSchemaId(boolean noSchemaId) {
        this.noSchemaId = noSchemaId;
    }
    
    /**
     * Constructor
     */
    public OracleERPConfiguration() {
        //empty
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        Assertions.blankCheck(user, "user");
        Assertions.nullCheck(password, "password");
 // TODO DataSource
 //       if (StringUtil.isBlank(dataSource)) {
            if(StringUtil.isBlank(connUrl)) {
                Assertions.blankCheck(hostName, "databaseName"); 
                Assertions.blankCheck(port, "databaseName"); 
                Assertions.blankCheck(databaseName, "databaseName"); 
            }
//        }
    }

    /**
     * The schema id from the user
     * see the bug id. 19352
     * @return The user and dot et the end.
     */
    public String getSchemaId() {
        if(noSchemaId) return "";
        return user.toUpperCase()+".";
    }
    
    /**
     * The connection url constructed from host, port and database name, or from connection urlS
     * 
     * @return The user and dot et the end.
     */
    public String getConnectionUrl() {
        if (StringUtil.isBlank(connUrl)) {
            return MessageFormat.format(ORACLE_THIN_CONN_URL, getHostName(), getPort(), getDatabaseName());
        }
        return connUrl;
    }
    
}