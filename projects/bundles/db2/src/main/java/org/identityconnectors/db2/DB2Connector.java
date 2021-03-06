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
package org.identityconnectors.db2;

import java.sql.*;
import java.text.*;
import java.util.*;

import org.identityconnectors.common.*;
import org.identityconnectors.common.logging.*;
import org.identityconnectors.common.security.*;
import org.identityconnectors.dbcommon.*;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.AttributeInfo.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;

/**
 * Connector to DB2 database.
 * DB2Connector is main class of connector contract when DB2 database is target resource. DB2 uses external authentication provider and internal
 * authorization service. DB2 stores authorization for users of DB2 objects : database,schema,table,index,procedure,package,server,tablespace.
 * It supports just one ObjectClass : ObjectClass.ACCOUNT.
 * DB2 connector is case insensitive, DB2 stores uppercase values in system tables.
 * <br/>
 * 
 * DB2 connector uses following attributes :
 * <ol>
 * 	<li>Name : is name of user</li>
 * 	<li>grants : is multivalue attribute that means list of grants user has</li>
 * </ol>
 * 
 * DB2 connector supports following operations : 
 * <ol>
 *      <li>AuthenticateOp : We try to create JDBC connection to authenticate</li>
 *      <li>CreateOp : We store passed user's grants in DB2 system tables, actually we perform 'execute' on passed grants.
 *          At least user is granted 'CONNECT ON DATABASE'.  
 *      </li>
 *      <li>SearchOp : Natively we support search by user name. Search by grants is also supported, but in this case we
 *          return all users with grants and framework does the filtering.
 *      </li>
 *      <li>DeleteOp : We delete all users's grants</li>
 *      <li>UpdateAttributeValuesOp : We update user grants
 *          <ul>
 *              <li>For update we replace existing grants with passed ones</li>
 *              <li>For addAttributeValues we add passed grants to existing grants</li>
 *              <li>For removeAttributeValues we revoke passed grants</li>
 *          </ul>
 *      </li>
 *      <li>TestOp : We test whether connection to DB2 is still alive</li>
 * </ol>
 * 
 * DB2 connector implements AttributeNormalizer to uppercase passed user name, uid,grants, because DB2 stores users and grants in uppercase. 
 * 
 * Look at {@link DB2Configuration} for more information about configuration properties.
 * 
 * 
 * @author kitko
 *
 */
@ConnectorClass(
        displayNameKey = DB2Messages.DB2_CONNECTOR_DISPLAY,
        configurationClass = DB2Configuration.class,
        messageCatalogPaths={"org/identityconnectors/dbcommon/Messages","org/identityconnectors/db2/Messages"})
public class DB2Connector implements AuthenticateOp,SchemaOp,CreateOp,SearchOp<FilterWhereBuilder>,DeleteOp,UpdateAttributeValuesOp,TestOp,PoolableConnector,AttributeNormalizer {
	
	private final static Log log = Log.getLog(DB2Connector.class);
	private Connection adminConn;
	private DB2Configuration cfg;
    static final String USER_AUTH_GRANTS = "grants";
    private String testSQL;
    /**
     * Default constructor called using reflection from framework
     */
    public DB2Connector(){
    }

	/**
	 * Authenticates user in DB2 database. Here we create new SQL connection with passed credentials to authenticate user. 
	 * We check SQL state and return code to verify that possibly thrown exception really means user/password is invalid.
	 * When we are able to get connection using passed credentials, we consider that authenticate passed.
	 */
    public Uid authenticate(ObjectClass objectClass, String username, GuardedString password,OperationOptions options) {
		log.info("Authenticate user: {0}", username);
		//just try to create connection with passed credentials
		Connection conn = null;
		try{
			conn = createConnection(username, password);
		}
		catch(RuntimeException e){
			if(e.getCause() instanceof SQLException){
				SQLException sqlE = (SQLException) e.getCause();
				if("28000".equals(sqlE.getSQLState()) && -4214 ==sqlE.getErrorCode()){
					//Wrong user or password, log it here and rethrow
					log.info(e,"DB2.authenticate : Invalid user/passord for user: {0}",username);
					throw new InvalidCredentialException(cfg.getConnectorMessages().format(DB2Messages.AUTHENTICATE_INVALID_CREDENTIALS,null),e.getCause());
				}
			}
			throw e;
		}
		finally{
			SQLUtil.closeQuietly(conn);
		}
		log.info("User authenticated : {0}",username);
		return new Uid(username.toUpperCase());
	}
	
	public Schema schema() {
        //The Name is supported attribute
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(Name.NAME,String.class,EnumSet.of(Flags.NOT_UPDATEABLE,Flags.REQUIRED)));
        /*
        AttributeInfo password = AttributeInfoBuilder.build(
                OperationalAttributes.PASSWORD_NAME, GuardedString.class,
                EnumSet.of(Flags.NOT_READABLE,Flags.NOT_RETURNED_BY_DEFAULT,Flags.NOT_CREATABLE));
        attrInfoSet.add(password);
        */
        AttributeInfoBuilder grantsBuilder = new AttributeInfoBuilder();
        grantsBuilder.setName(USER_AUTH_GRANTS).setCreateable(true).
        setUpdateable(true).setRequired(true).setReadable(true).
        setMultiValued(true).setReturnedByDefault(true);
        attrInfoSet.add(grantsBuilder.build());

        // Use SchemaBuilder to build the schema. Currently, only ACCOUNT type is supported.
        SchemaBuilder schemaBld = new SchemaBuilder(getClass());
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        return schemaBld.build();
    }
	
	private String getTestSQL(){
	    if(testSQL != null){
	        return testSQL;
	    }
	    testSQL = DB2Specifics.findTestSQL(adminConn);
	    return testSQL;
	}

	public void checkAlive() {
		DB2Specifics.testConnection(adminConn,getTestSQL());
	}

	public void dispose() {
		SQLUtil.closeQuietly(adminConn);
	}

	public DB2Configuration getConfiguration() {
		return cfg;
	}

	public void init(Configuration cfg) {
		this.cfg = (DB2Configuration) cfg;
		this.adminConn = createAdminConnection();
	}
	
	private Connection createAdminConnection(){
	    final Connection conn =  cfg.createAdminConnection();
        //switch off auto commit, but not when connecting using datasource.
        //Probably connection from DS would throw exception  when trying to change autocommit
	    if(!DB2Configuration.ConnectionType.DATASOURCE.equals(cfg.getConnType())){
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                throw new ConnectorException("Cannot switch off autocommit",e);
            }
        }
        return conn;
	}
	
	private Connection createConnection(String user,GuardedString password){
		final Connection conn = cfg.createConnection(user, password);
		return conn;
	}
    
    List<String> buildAuthorityAttributeValue(String userName){
    	DB2AuthorityReader dB2AuthorityReader = new DB2AuthorityReader(adminConn);
    	Collection<DB2Authority> allAuths = null;
		try {
			allAuths = dB2AuthorityReader.readAllAuthorities(userName);
		} catch (SQLException e) {
			throw new ConnectorException("Error reading db2 authorities",e);
		}
    	List<String> result = new ArrayList<String>(2);
    	for(DB2Authority authority : allAuths){
    		final DB2AuthorityTable authorityTable = DB2Specifics.authType2DB2AuthorityTable(authority.authorityType);
            String grantString = authorityTable.generateGrant(authority);
            result.add(grantString);
    	}
    	return result;
    }
    
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new DB2FilterTranslator(oclass, options);
    }
	
    /**
     * Executes search using DB2 system table. We support searching by UID/Name and grants. When searching by uid/name we use
     * <code>SYSIBM.SYSDBAUTH table</code> to execute search. We always include grants attribute by reading all grants from system tables.
     * Then when searching by grants, we will return now all users and framework will filter users that have filtered grants. 
     */
	public void executeQuery(ObjectClass oclass, FilterWhereBuilder where,ResultsHandler handler, OperationOptions options) {
        checkObjectClass(oclass);
		//Read users from SYSIBM.SYSDBAUTH table
		//DB2 stores users in UPPERCASE , we must do UPPER(TRIM(GRANTEE)) = upper('john')
        final String ALL_USER_QUERY = "SELECT GRANTEE FROM SYSIBM.SYSDBAUTH WHERE GRANTEETYPE = 'U' AND CONNECTAUTH = 'Y'";
        // Database query builder will create SQL query.
        // if where == null then all users are returned
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(ALL_USER_QUERY);
        query.setWhere(where);
        final String sql = query.getSQL();
        log.info("Executing search query : {0}", sql);
        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = adminConn.prepareStatement(sql);
            SQLUtil.setParams(statement, query.getParams());
            result = statement.executeQuery();
            while (result.next()) {
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                
                final String userName = result.getString("GRANTEE").trim();
                //if(options.getAttributesToGet() != null && Arrays.asList(options.getAttributesToGet()).contains(USER_AUTH_GRANTS)){
                List<String> authStrings = buildAuthorityAttributeValue(userName);
                bld.addAttribute(USER_AUTH_GRANTS,authStrings);
                //}
                
                bld.setUid(new Uid(userName));
                bld.setName(userName);
                //No other attributes are now supported.
                //Password can be encoded and it is not provided as an attribute
                
                // only deals w/ accounts..
                bld.setObjectClass(ObjectClass.ACCOUNT);
                
                // create the connector object..
                ConnectorObject ret = bld.build();
                if (!handler.handle(ret)) {
                    break;
                }
            }
        } catch (SQLException e) {
            String detailMsg = new SQLMsgRetriever().retrieveMsg(e);
            throw new ConnectorException(cfg.getConnectorMessages().format(DB2Messages.SEARCH_FAILED, null, detailMsg),e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
	}

    private void checkObjectClass(ObjectClass oclass) {
        if (!ObjectClass.ACCOUNT.equals(oclass)) {
            throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.UNSUPPORTED_OBJECT_CLASS,null,oclass));
        }
    }
	
	/**
	 * Create user in case of DB2 means only storing passed grants. We will actually not create any new user, DB2 uses externally authentication
	 * provider to do real authentication, default to underlying OS. So here we just verify name of user and verify if same user is not already stored
	 * in DB2 system tables. Then we store passed user grants using grant statement. 
	 */
	public Uid create(ObjectClass oclass, Set<Attribute> attrs,OperationOptions options) {
	    checkObjectClass(oclass);
	    checkCreateAttributes(attrs);
        Name user = AttributeUtil.getNameFromAttributes(attrs);
		if (user == null || StringUtil.isBlank(user.getNameValue())) {
            throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.NAME_IS_NULL_OR_EMPTY,null));
        }
        final String userName = user.getNameValue();
		log.info("Creating user : {0}", userName);
        checkUserNotExist(userName);
        checkDB2Validity(userName);
        try{
        	updateAuthority(userName,attrs,UpdateType.ADD);
        	adminConn.commit();
        	log.info("User created : {0}", userName);
        }
        catch(Exception e){
            SQLUtil.rollbackQuietly(adminConn);
            String detailMsg = new SQLMsgRetriever().retrieveMsg(e);
        	throw new ConnectorException(cfg.getConnectorMessages().format(DB2Messages.CREATE_OF_USER_FAILED, null, userName, detailMsg),e);
        }
		return new Uid(userName);
	}
	
	
	private void checkCreateAttributes(Set<Attribute> attrs) {
		for(Attribute attribute : attrs){
			if(attribute.is(Name.NAME)){
			}
			else if(attribute.is(USER_AUTH_GRANTS)){
			}
			else{
				throw new IllegalArgumentException(MessageFormat.format("Unrecognized argument [{0}] in DB2 create operation",attribute.getName()));
			}
		}
	}
	
	private void checkUpdateAttributes(Set<Attribute> attrs) {
		for(Attribute attribute : attrs){
			if(attribute.is(Uid.NAME)){
			}
			else if(attribute.is(USER_AUTH_GRANTS)){
			}
			else{
				throw new IllegalArgumentException(MessageFormat.format("Unrecognized argument [{0}] in DB2 update operation",attribute.getName()));
			}
		}
	}
	

	private void checkUserNotExist(String user) {
		boolean userExist = userExist(user);
		if(userExist){
			throw new AlreadyExistsException(cfg.getConnectorMessages().format(DB2Messages.USER_ALREADY_EXISTS, null, user));
		}
	}
	
	private void checkUserExist(String user) {
		boolean userExist = userExist(user);
		if(!userExist){
			throw new UnknownUidException(new Uid(user),ObjectClass.ACCOUNT);
		}
	}
	
	
	private boolean userExist(String user){
		final String ALL_USER_QUERY = "SELECT GRANTEE FROM SYSIBM.SYSDBAUTH WHERE GRANTEETYPE = 'U' AND CONNECTAUTH = 'Y' AND TRIM(GRANTEE) = ?";
		PreparedStatement st = null;
		ResultSet rs = null;
		try{
			st = adminConn.prepareStatement(ALL_USER_QUERY);
			st.setString(1,user.toUpperCase());
			rs = st.executeQuery();
			return rs.next(); 
		}
		catch(SQLException e){
			throw new ConnectorException("Cannot test whether user exist",e);
		}
		finally{
			SQLUtil.closeQuietly(rs);
			SQLUtil.closeQuietly(st);
		}
	}

	/**
     *  Applies resources grants and revokes to the passed user.  Updates
     *  occurs in a transaction.  Assumes connection is already open.
     */
    @SuppressWarnings("unchecked")
	private void updateAuthority(String user,Set<Attribute> attrs,UpdateType type)  throws SQLException {
        checkAdminConnection();
        Attribute wsAttr = AttributeUtil.find(USER_AUTH_GRANTS, attrs);
        Collection<String> grants = (Collection<String>) (wsAttr != null ? new ArrayList<Object>(wsAttr.getValue()) : new ArrayList<String>(3)); 
        switch(type){
        	case ADD : 		{
        					 addMandatoryConnect(grants);
        					 executeGrants(grants,user);
        					 break;
        	}
        	case REPLACE :  {
        					addMandatoryConnect(grants);
        					revokeAllGrants(user);
        					executeGrants(grants,user);
        					break;
        	}
        	case DELETE : 	{
        					removeMandatoryRevoke(grants);
        					executeRevokes(grants, user);
        					break;
        	}
        }
    }
    
    private void addMandatoryConnect(Collection<String> grants){
    	boolean addConnect = true;
    	for(String grant : grants){
    		if(grant.trim().equalsIgnoreCase("CONNECT ON DATABASE")){
    			addConnect = false;
    		}
    	}
    	if(addConnect){
    		grants.add("CONNECT ON DATABASE");
    	}
    }
    
    private void removeMandatoryRevoke(Collection<String> grants){
    	for(Iterator<String> i = grants.iterator();i.hasNext();){
    		if(i.next().trim().equalsIgnoreCase("CONNECT ON DATABASE")){
    			i.remove();
    		}
    	}
    }
    
    
    private void checkAdminConnection() {
		if(adminConn == null){
			throw new IllegalStateException("No admin connection present");
		}
	}

	/**
     *  Checks a given account id to make sure they follow DB2
     *  rules for validity.  The rules are given in the DB2 SQL Reference
     *  Manual.  They include length limits, forbidden prefixes, and forbidden
     *  keywords.  Throws and exception if the name or password are invalid.
     */
    void checkDB2Validity(String accountID)  {
        if (accountID.length() > DB2Specifics.MAX_NAME_SIZE) {
        	throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.USERNAME_LONG, null, DB2Specifics.MAX_NAME_SIZE, accountID));
        }
        if (DB2Specifics.containsIllegalDB2Chars(accountID.toCharArray())) {
            throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.USERNAME_CONTAINS_ILLEGAL_CHARACTERS, null, accountID));
        }
        if (DB2Specifics.isReservedName(accountID.toUpperCase())) {
            throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.USERNAME_IS_RESERVED_WORD, null, accountID));
        }
    }
    
    /**
     *  Removes all grants for a user on the resource.  Effectively
     *  deletes them from the resource.
     */
    private void revokeAllGrants(String user) throws SQLException {
        checkDB2Validity(user);
        Collection<DB2Authority> allAuthorities = new DB2AuthorityReader(adminConn).readAllAuthorities(user);
        revokeGrants(allAuthorities);
    }
    
    /**
     *  For a given grant type and user, revokes the passed collection
     *  of grant objects from the resource.
     */
    private void revokeGrants(Collection<DB2Authority> db2AuthoritiesToRevoke)   throws  SQLException {
        for(DB2Authority auth : db2AuthoritiesToRevoke){
            DB2AuthorityTable authTable = DB2Specifics.authType2DB2AuthorityTable(auth.authorityType);
            String revokeSQL = authTable.generateRevokeSQL(auth);
            executeSQL(revokeSQL);
        }
    }


    
    private void executeSQL(String sql) throws SQLException {
        checkAdminConnection();
        Statement statement = null;
        try {
            statement = adminConn.createStatement();
            statement.execute(sql);
        }
        catch(SQLException e){
        	log.error(e,"Error executing sql {0}", sql);
        	throw e;
        }
        finally {
            SQLUtil.closeQuietly(statement);
        }
    }
    
    
    /**
     *  Executes a set of sql GRANT statements built using an sql
     *  prefix, a collection of grant objects, a postfix, and a user.
     *  Throws if anything goes wrong.
     */
    private void executeGrants(Collection<String> grants,String user)  throws SQLException{
        for(String grant : grants){
            String sql = "GRANT " + grant  + " TO USER " + user.toUpperCase() ;
            executeSQL(sql);
        }
    }
    
    /**
     *  Executes a set of sql REVOKE statements built using an sql
     *  prefix, a collection of grant objects, a postfix, and a user.
     *  Throws if anything goes wrong.
     */
    private void executeRevokes(Collection<String> grants,String user)  throws SQLException{
        for(String grant : grants){
            String sql = "REVOKE " + grant  + " FROM USER " + user.toUpperCase() ;
            executeSQL(sql);
        }
    }
    

	/**
	 * Removes all associated grants from user, so do all revoke statement.
	 */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        checkObjectClass(objClass);
		final String uidValue = uid.getUidValue();
        checkUserExist(uidValue);
		log.info("Deleting user : {0}", uidValue);
        try {
			revokeAllGrants(uidValue);
			adminConn.commit();
			log.info("User deleted : {0}", uidValue);
		} catch (Exception e) {
		    SQLUtil.rollbackQuietly(adminConn);
		    String detailMsg = new SQLMsgRetriever().retrieveMsg(e);
			throw new ConnectorException(cfg.getConnectorMessages().format(DB2Messages.DELETE_OF_USER_FAILED, null, uidValue, detailMsg),e);
		}
	}
	

    /**
     * Test of configuration and validity of connection
     */
	public void test() {
		cfg.validate();
		DB2Specifics.testConnection(adminConn,getTestSQL());
	}

    /**
     * Replaces value of grants attribute. 
     */
	public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
	    if(cfg.isReplaceAllGrantsOnUpdate()){
	        return update(UpdateType.REPLACE,objclass,AttributeUtil.addUid(attrs,uid),options);
	    }
	    else{
	        return update(UpdateType.ADD,objclass,AttributeUtil.addUid(attrs,uid),options); 
	    }
    }
    
    /**
     * Add grants to existing grants of user
     */
	public Uid addAttributeValues(ObjectClass objclass,
            Uid uid,
            Set<Attribute> valuesToAdd,
            OperationOptions options) {
        return update(UpdateType.ADD,objclass,AttributeUtil.addUid(valuesToAdd, uid),options);
    }

    /**
     * Removes grants from user
     */
	public Uid removeAttributeValues(ObjectClass objclass,
            Uid uid,
            Set<Attribute> valuesToRemove,
            OperationOptions options) {
        return update(UpdateType.DELETE,objclass,AttributeUtil.addUid(valuesToRemove, uid),options);
    }
	
	private Uid update(UpdateType type, ObjectClass objclass, Set<Attribute> attrs,OperationOptions options) {
	    checkObjectClass(objclass);
	    checkUpdateAttributes(attrs);
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        if(name != null){
        	throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.NAME_IS_NOT_UPDATABLE,null));
        }
        Uid uid = AttributeUtil.getUidAttribute(attrs);
		if (uid == null || StringUtil.isBlank(uid.getUidValue())){
            throw new IllegalArgumentException(cfg.getConnectorMessages().format(DB2Messages.UPDATE_UID_CANNOT_BE_NULL_OR_EMPTY, null));
        }
		final String uidValue = uid.getUidValue();
		checkUserExist(uidValue);
        try{
    		log.info("Update user : {0}", uidValue);
        	updateAuthority(uidValue, attrs, type);
        	adminConn.commit();
        	log.info("User updated : {0}", uidValue);
        }
        catch(Exception e){
            SQLUtil.rollbackQuietly(adminConn);
            String detailMsg = new SQLMsgRetriever().retrieveMsg(e);
        	throw new ConnectorException(cfg.getConnectorMessages().format(DB2Messages.UPDATE_OF_USER_FAILED,null,uidValue, detailMsg),e);
        }
		return uid;
	}

	public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
		if(attribute.is(Name.NAME)){
			String value = (String) attribute.getValue().get(0);
			return new Name(value.trim().toUpperCase());
		}
		else if(attribute.is(Uid.NAME)){
			String value = (String) attribute.getValue().get(0);
			return new Uid(value.trim().toUpperCase());
		}
		else if(attribute.is(USER_AUTH_GRANTS)){
		    List<Object> grants = attribute.getValue();
		    List<String> upGrants = new ArrayList<String>(grants.size());
		    for(Object grant : grants){
		        upGrants.add(grant.toString().toUpperCase());
		    }
		    return AttributeBuilder.build(USER_AUTH_GRANTS, upGrants);
		}
		return attribute;
	}
	
	private enum UpdateType {
	    ADD,
	    REPLACE,
	    DELETE
	}
    
    
    
    
  
}
