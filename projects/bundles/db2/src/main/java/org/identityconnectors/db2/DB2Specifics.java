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
import java.util.*;

import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

/**
 * Here we hide DB2 specifics constants,mappings,restrictions ...
 * @author kitko
 *
 */
abstract class DB2Specifics {
	private DB2Specifics(){}
	/** Classname of DB2 jcc driver , type 4 or type2 driver*/
	final static String JCC_DRIVER = "com.ibm.db2.jcc.DB2Driver";
	/** Old driver that uses local db2 client with stored aliases , type 2 driver */
	final static String CLI_LEGACY_DRIVER = "COM.ibm.db2.jdbc.app.DB2Driver";
	final static String DEFAULT_TYPE4_PORT = "50000";
	/** DB2 limitation on accountId size */
    final static int MAX_NAME_SIZE = 30;

	
	
    // These names come from the DB2 SQL Reference manual.
    // None of these names are legal for starting account id
    // or password.  The prohibition is case insensitive.
    private static final Collection<String> excludedNamePrefixes = Arrays.asList("SQL", "SYS", "IBM");
    
    static final String AUTH_TYPE_DATABASE = "database";
    static final String AUTH_TYPE_INDEX = "index";
    static final String AUTH_TYPE_PACKAGE = "package";
    static final String AUTH_TYPE_SCHEMA = "schema";
    static final String AUTH_TYPE_SERVER = "server";
    static final String AUTH_TYPE_TABLE = "table";
    static final String AUTH_TYPE_TABLESPACE = "tablespace";

    // A map from authority table type to authority table.
    private static final Map<String,DB2AuthorityTable> databaseAuthTableMap = new HashMap<String,DB2AuthorityTable>();
    static {
        databaseAuthTableMap.put(AUTH_TYPE_DATABASE,
            new DB2AuthorityTable("ON DATABASE"));
        databaseAuthTableMap.put(AUTH_TYPE_INDEX,
            new DB2AuthorityTable("ON INDEX"));
        databaseAuthTableMap.put(AUTH_TYPE_PACKAGE,
            new DB2AuthorityTable("ON PACKAGE"));
        databaseAuthTableMap.put(AUTH_TYPE_SCHEMA,
            new DB2AuthorityTable("ON SCHEMA"));
        databaseAuthTableMap.put(AUTH_TYPE_SERVER,
            new DB2AuthorityTable("ON SERVER"));
        databaseAuthTableMap.put(AUTH_TYPE_TABLESPACE,
            new DB2AuthorityTable("OF TABLESPACE"));
        databaseAuthTableMap.put(AUTH_TYPE_TABLE,
            new DB2AuthorityTable("ON"));
    }
    
    static DB2AuthorityTable authType2DB2AuthorityTable(String authType){
    	return databaseAuthTableMap.get(authType);
    }
    

	/** List of db2 keywords */
	private static volatile Collection<String> excludeNames;
	
	private static Collection<String> readExcludeNames() {
		if(excludeNames == null){
			synchronized (DB2Configuration.class) {
				if(excludeNames == null){
					//We will read exclude names from resource named "exclude.names"
					String names = IOUtil.getResourceAsString(DB2Configuration.class, "exclude.names");
					if(names == null){
						throw new IllegalStateException("Cannot load exclude names for DB2 connector");
					}
					excludeNames = new HashSet<String>();
					StringTokenizer tokenizer = new StringTokenizer(names,",\n",false);
					while(tokenizer.hasMoreTokens()){
						excludeNames.add(tokenizer.nextToken());
					}
				}
			}
		}
		return excludeNames;
	}
	
	static Collection<String> getExcludeNames(){
		return Collections.unmodifiableCollection(readExcludeNames());
	}
	
	static Collection<String> getExcludedNamePrefixes(){
		return Collections.unmodifiableCollection(excludedNamePrefixes);
	}
	
	static boolean isReservedName(String name){
		return getExcludeNames().contains(name) || includesPrefix(name,excludedNamePrefixes);
	}
	
    /**
     *  Utility method for determining whether the passed target string
     *  begins with any of the passed prefixes.
     */
    private static boolean includesPrefix(String target, Collection<String> prefixes) {
        for (String prefix : prefixes) {
            if (target.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     *  Returns whether a passed string contains characters that DB2 deems
     *  to be illegal for use in account ids and passwords.  These characters
     *  come from the DB2 SQL Reference Manual.
     */
    static boolean containsIllegalDB2Chars(char[] target) {
        for (int i = 0; i < target.length; i++) {
            char c = target[i];
            // this hard coded rule is fast and simple for now.
            // if the complexity of the criteria ever increases,
            // it would be better to check in a Set or some such.
            boolean charOk = 
                       (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || (c == '@')
                    || (c == '#')
                    || (c == '$');
            if(!charOk){
                return true;
            }
        }
        return false;
    }
    
    
	static void testConnection(Connection connection,String testSQL){
		PreparedStatement st = null;
		ResultSet rs = null;
		try{
			st = connection.prepareStatement(testSQL);
			rs = st.executeQuery();
		}
		catch(SQLException e){
			if("08001".equals(e.getSQLState()) && -4499 == e.getErrorCode()){
				throw new IllegalStateException("DB2 connection is stale",e);
			}
			else{
				throw new IllegalStateException("Unknown DB2 error while testing connection " + e.getMessage(),e); 
			}
		}
		finally{
		    SQLUtil.closeQuietly(rs);
			SQLUtil.closeQuietly(st);
		}
	}
	
	static String findTestSQL(Connection connection){
	    String[] testSQL = new String[]{"select 1 from sysibm.dual","select 1 from sysibm.SYSDUMMY1","select 1 from syscat.SCHEMATA where schemaname = 'DUMMY'"};
        Statement st = null;
        String sql = null;
        try{
            st = connection.createStatement();
        }
        catch(SQLException e){
            throw new IllegalStateException("Cannot create statement",e);
        }
        for(String s : testSQL){
            sql = testTestSQL(st,s);
            if(sql != null){
                break;
            }
        }
        SQLUtil.closeQuietly(st);
        if(sql == null){
            throw new ConnectorException("Cannot find valid test SQL for DB2");
        }
        return  sql;
	}
	
	private static String testTestSQL(Statement st,String sql){
	    ResultSet rs = null;
        try{
            rs = st.executeQuery(sql);
            return sql;
        }
        catch(SQLException e){
            return null; 
        }
        finally{
            SQLUtil.closeQuietly(rs);
        }
	}
	
	static Connection createType4Connection(Type4ConnectionInfo info){
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("jdbc:").append(info.getSubprotocol());
		if(info.getHost() != null && info.getHost().length() > 0){
			urlBuilder.append("://").append(info.getHost());
		}
		if(info.getPort() != null){
			urlBuilder.append(":").append(info.getPort());
		}
		urlBuilder.append("/").append(info.getDatabase());
		return SQLUtil.getDriverMangerConnection(info.getDriver(), urlBuilder.toString(), info.getUser(), info.getPassword());
	}
	
	static Connection createType2Connection(Type2ConnectionInfo info){
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder.append("jdbc:").append(info.getSubprotocol()).append(':');
		urlBuilder.append(info.getAliasName());
		return SQLUtil.getDriverMangerConnection(info.getDriver(), urlBuilder.toString(), info.getUser(), info.getPassword());
	}
	
	static Connection createDataSourceConnection(String dsName,Hashtable<?,?> env){
		return SQLUtil.getDatasourceConnection(dsName,env);
	}
	
	static Connection createDataSourceConnection(String dsName,String user,GuardedString password,Hashtable<?,?> env){
		return SQLUtil.getDatasourceConnection(dsName,user,password,env);
	}

	static boolean hasInvalidPrefix(String name) {
		char firstChar = name.charAt(0);
		if(Character.isDigit(firstChar)){
			return true;
		}
		if('.' == firstChar){
			return true;
		}
		if(',' == firstChar){
			return true;
		}
		return false;
	}


	
    
}
