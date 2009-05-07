/**
 * 
 */
package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleMessages.CS_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.CS_HELP;
import static org.identityconnectors.oracle.OracleMessages.DATABASE_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.DATABASE_HELP;
import static org.identityconnectors.oracle.OracleMessages.DATASOURCE_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.DATASOURCE_HELP;
import static org.identityconnectors.oracle.OracleMessages.DRIVER_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.DRIVER_HELP;
import static org.identityconnectors.oracle.OracleMessages.DSJNDIENV_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.DSJNDIENV_HELP;
import static org.identityconnectors.oracle.OracleMessages.HOST_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.HOST_HELP;
import static org.identityconnectors.oracle.OracleMessages.PASSWORD_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.PASSWORD_HELP;
import static org.identityconnectors.oracle.OracleMessages.PORT_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.PORT_HELP;
import static org.identityconnectors.oracle.OracleMessages.URL_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.URL_HELP;
import static org.identityconnectors.oracle.OracleMessages.USER_DISPLAY;
import static org.identityconnectors.oracle.OracleMessages.USER_HELP;

import java.sql.Connection;
import java.sql.SQLException;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.JNDIUtil;
import org.identityconnectors.dbcommon.LocalizedAssert;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.oracle.OracleDriverConnectionInfo.Builder;

/**
 * Set of configuration properties for connecting to Oracle database
 * @author kitko
 *
 */
public final class OracleConfiguration extends AbstractConfiguration implements Cloneable{
    private String host;
    private String port;
    private String driver;
    private String driverClassName;
    private String database;
    private String user;
    private GuardedString password;
    private String dataSource;
    private String url;
    private String[] dsJNDIEnv;
    private ConnectionType connType;
    private OracleCaseSensitivitySetup cs;
    private String caseSensitivityString;
    
    /**
     * Creates configuration
     */
    public OracleConfiguration() {
        //Set casesensitivity setup to default one
        cs = new OracleCaseSensitivityBuilder().build();
        caseSensitivityString = "default";
        port = OracleSpecifics.LISTENER_DEFAULT_PORT;
        driver = OracleSpecifics.THIN_DRIVER;
    }
    
    
    /** Type of connection we will use to connect to Oracle */
    static enum ConnectionType{
        /** Connecting using datasource */
        DATASOURCE,
        /** Connecting using type 4 driver (host,port,databaseName)*/
        THIN,
        /** Connecting using type 2 driver (using TNSNAMES.ora) */
        OCI,
        /** Custom driver with custom URL */
        FULL_URL
    }
    
    /**
     * Default clone implementation.
     * @throws ConnectorException when super.clone fails
     */
    public OracleConfiguration clone() throws ConnectorException{
        try {
            return (OracleConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ConnectorException("Clone of OracleConfiguration super class failed",e);
        }
    }
    
    
    /**
     * @return the host
     */
    @ConfigurationProperty(order = 0,displayMessageKey=HOST_DISPLAY,helpMessageKey=HOST_HELP)
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    @ConfigurationProperty(order = 1,displayMessageKey=PORT_DISPLAY,helpMessageKey=PORT_HELP)
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * @return the driver
     */
    @ConfigurationProperty(order = 2,displayMessageKey=DRIVER_DISPLAY,helpMessageKey=DRIVER_HELP)
    public String getDriver() {
        return driver;
    }

    /**
     * @param driver the driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * @return the database
     */
    @ConfigurationProperty(order = 3,displayMessageKey=DATABASE_DISPLAY,helpMessageKey=DATABASE_HELP)
    public String getDatabase() {
        return database;
    }

    /**
     * @param database the database to set
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @return the user
     */
    @ConfigurationProperty(order = 4,displayMessageKey=USER_DISPLAY,helpMessageKey=USER_HELP)
    public String getUser() {
        return user;
    }
    
    String getUserOwner(){
    	//if we were logged as system, owner will be SYSTEM
    	if("".equals(cs.getAttributeFormatter(OracleUserAttributeCS.SYSTEM_USER).getQuatesChar())){
    		return user.toUpperCase();
    	}
    	return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    @ConfigurationProperty(order = 5,displayMessageKey=PASSWORD_DISPLAY,helpMessageKey=PASSWORD_HELP,confidential=true)
    public GuardedString getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(GuardedString password) {
        this.password = password;
    }

    /**
     * @return the dataSource
     */
    @ConfigurationProperty(order = 6,displayMessageKey=DATASOURCE_DISPLAY,helpMessageKey=DATASOURCE_HELP)
    public String getDataSource() {
        return dataSource;
    }

    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * @return the dsJNDIEnv
     */
    @ConfigurationProperty(order = 6,displayMessageKey=DSJNDIENV_DISPLAY,helpMessageKey=DSJNDIENV_HELP)
    public String[] getDsJNDIEnv() {
        return dsJNDIEnv;
    }

    /**
     * @param dsJNDIEnv the dsJNDIEnv to set
     */
    public void setDsJNDIEnv(String[] dsJNDIEnv) {
        this.dsJNDIEnv = dsJNDIEnv;
    }
    
    

    /**
     * @return the url
     */
    @ConfigurationProperty(order = 7,displayMessageKey=URL_DISPLAY,helpMessageKey=URL_HELP)
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * @return caseSensitivityString
     */
    @ConfigurationProperty(order = 8,displayMessageKey=CS_DISPLAY,helpMessageKey=CS_HELP,required=true)
    public String getCaseSensitivity(){
        return caseSensitivityString;
    }
    
    /** Sets case sensitivity from string map 
     * @param cs */
    public void setCaseSensitivity(String cs){
        this.caseSensitivityString = cs;
    }
    
    OracleCaseSensitivitySetup getCSSetup(){
        return cs;
    }
    
    void setCSSetup(OracleCaseSensitivitySetup cs){
        this.cs = new LocalizedAssert(getConnectorMessages()).assertNotNull(cs, "cs");
    }

    
    @Override
    public void validate() {
    	LocalizedAssert la = new LocalizedAssert(getConnectorMessages(),true);
        la.assertNotBlank(caseSensitivityString, CS_DISPLAY);
        this.cs = new OracleCaseSensitivityBuilder().parseMap(caseSensitivityString).build();
        if(dataSource != null){
			la.assertNotBlank(dataSource, DATASOURCE_DISPLAY);
			la.assertBlank(host, HOST_DISPLAY);
			la.assertBlank(database,DATABASE_DISPLAY);
			la.assertBlank(driver,DRIVER_DISPLAY);
			la.assertBlank(port,PORT_DISPLAY);
            connType = ConnectionType.DATASOURCE;
        }
        else{
        	la.assertNotBlank(driver, DRIVER_DISPLAY);
            if(StringUtil.isNotBlank(url)){
                la.assertNotBlank(user,USER_DISPLAY);
                la.assertNotNull(password, PASSWORD_DISPLAY);
    			la.assertBlank(host, HOST_DISPLAY);
    			la.assertBlank(database,DATABASE_DISPLAY);
    			la.assertBlank(port,PORT_DISPLAY);
                if(OracleSpecifics.THIN_DRIVER.equals(driver)){
                    driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                }
                else if(OracleSpecifics.OCI_DRIVER.equals(driver)){
                    driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                }
                else{
                	driverClassName = driver;
                }
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot load driver class : + " + driverClassName,e);
                }
                connType = ConnectionType.FULL_URL;
            }
            else if(OracleSpecifics.THIN_DRIVER.equals(driver)){
            	la.assertNotBlank(host, HOST_DISPLAY);
            	la.assertNotBlank(port, PORT_DISPLAY);
            	la.assertNotBlank(user, USER_DISPLAY);
            	la.assertNotNull(password, PASSWORD_DISPLAY);
            	la.assertNotBlank(database, DATABASE_DISPLAY);
                driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot load thin driver class : + " + driverClassName,e);
                }
                connType = ConnectionType.THIN;
            }
            else if(OracleSpecifics.OCI_DRIVER.equals(driver)){
            	la.assertNotBlank(user, USER_DISPLAY);
            	la.assertNotNull(password, PASSWORD_DISPLAY);
            	la.assertNotBlank(database, DATABASE_DISPLAY);
                driverClassName = OracleSpecifics.THIN_AND_OCI_DRIVER_CLASSNAME;
                try {
                    Class.forName(driverClassName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Cannot load driver class : + " + driverClassName,e);
                }
                connType = ConnectionType.OCI;
            }
            else{
            	throw new IllegalArgumentException("Must specify thin/oci driver or custom driver with full url");
            }
        }
        
    }
    
    Connection createUserConnection(String user, GuardedString password){
    	user = cs.normalizeAndFormatToken(OracleUserAttributeCS.USER, user);
    	password = cs.normalizeAndFormatToken(OracleUserAttributeCS.PASSWORD, password);
    	return createConnection(user,password);
    }
    
    Connection createAdminConnection(){
    	String user = cs.normalizeAndFormatToken(OracleUserAttributeCS.SYSTEM_USER, this.user);
    	GuardedString password = cs.normalizeAndFormatToken(OracleUserAttributeCS.SYSTEM_PASSWORD, this.password);
        return createConnection(user,password);
    }
    
    
    private Connection createConnection(String user,GuardedString password){
        validate();
        Connection connection = null;
        boolean disableAutoCommit = true;
        if(ConnectionType.DATASOURCE.equals(connType)){
        	disableAutoCommit = false;
            if(user != null){
            	connection = OracleSpecifics.createDataSourceConnection(dataSource,user,password,JNDIUtil.arrayToHashtable(dsJNDIEnv, getConnectorMessages()));
            }
            else{
            	connection =  OracleSpecifics.createDataSourceConnection(dataSource,JNDIUtil.arrayToHashtable(dsJNDIEnv,getConnectorMessages()));
            }
        }
        else if(ConnectionType.THIN.equals(connType)){
        	connection =  OracleSpecifics.createThinDriverConnection(new Builder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build()
                    );
        }
        else if(ConnectionType.OCI.equals(connType)){
        	connection =  OracleSpecifics.createOciDriverConnection(new Builder().
                    setDatabase(database).setDriver(driverClassName).setHost(host).setPassword(password).
                    setPort(port).setUser(user).build()
                    );
        }
        else if(ConnectionType.FULL_URL.equals(connType)){
        	connection =  OracleSpecifics.createCustomDriverConnection(new Builder().
                    setUrl(url).setDriver(driverClassName).setUser(user).setPassword(password).build()
            );
        }
        else{
        	throw new IllegalStateException("Invalid state of OracleConfiguration");
        }
        if(disableAutoCommit){
        	try {
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				throw new ConnectorException("Cannot switch off autocommit",e);
			}
        }
        try {
			connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		} catch (SQLException e) {
			throw new ConnectorException("Cannot set transaction isloation",e);
		}
        return connection;
    }
    

}
