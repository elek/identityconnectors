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
package org.identityconnectors.dbcommon;


import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;

import javax.naming.*;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.identityconnectors.framework.common.objects.*;
import org.junit.*;


/**
 * The SQL util tests
 * @version $Revision 1.0$
 * @since 1.0
 */
public class SQLUtilTests {
 
    /**
     * Test method for {@link SQLUtil#closeQuietly(Connection)}.
     * @throws Exception
     */
    @Test
    public void quietConnectionClose() throws Exception {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        tp.expectAndReturn("isClosed",Boolean.FALSE); 
        tp.expectAndThrow("close",new SQLException("expected")); 
        Connection c = tp.getProxy(Connection.class);
        DatabaseConnection dbc = new DatabaseConnection(c);
        SQLUtil.closeQuietly(dbc);
        Assert.assertTrue(tp.isDone());
        
        tp = new ExpectProxy<Connection>();
        tp.expectAndReturn("isClosed",Boolean.TRUE); 
        c = tp.getProxy(Connection.class);
        dbc = new DatabaseConnection(c);
        SQLUtil.closeQuietly(dbc);
        Assert.assertTrue(tp.isDone());
        
        //null tests
        dbc = null;
        SQLUtil.closeQuietly(dbc);
    }
    
    /**
     * Test method for {@link SQLUtil#rollbackQuietly(Connection)}.
     * @throws Exception
     */
    @Test
    public void quietConnectionRolback() throws Exception {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        tp.expectAndReturn("isClosed",Boolean.FALSE); 
        tp.expectAndThrow("rollback",new SQLException("expected")); 
        Connection s = tp.getProxy(Connection.class);
        DatabaseConnection dbc = new DatabaseConnection(s);
        SQLUtil.rollbackQuietly(dbc);
        Assert.assertTrue(tp.isDone());
        
        tp = new ExpectProxy<Connection>();
        tp.expectAndReturn("isClosed",Boolean.TRUE); 
        s = tp.getProxy(Connection.class);
        dbc = new DatabaseConnection(s);
        SQLUtil.rollbackQuietly(dbc);
        Assert.assertTrue(tp.isDone());
        
        //null tests
        dbc = null;
        SQLUtil.rollbackQuietly(dbc);                
    }
    

    /**
     * Test method for {@link SQLUtil#closeQuietly(Statement)}.
     */
    @Test
    public void quietStatementClose() {
        ExpectProxy<Statement> tp = new ExpectProxy<Statement>().expectAndThrow("close",new SQLException("expected")); 
        Statement s = tp.getProxy(Statement.class);
        SQLUtil.closeQuietly(s);
        Assert.assertTrue(tp.isDone());
    }

    /**
     * Test method for {@link SQLUtil#closeQuietly(ResultSet)}.
     */
    @Test
    public void quietResultSetClose() {
        ExpectProxy<ResultSet> tp = new ExpectProxy<ResultSet>().expectAndThrow("close",new SQLException("expected")); 
        ResultSet c = tp.getProxy(ResultSet.class);
        SQLUtil.closeQuietly(c);
        Assert.assertTrue(tp.isDone());
    }

    /**
     * Test method for {@link SQLUtil#closeQuietly(Connection)}.
     * Test method for {@link SQLUtil#closeQuietly(ResultSet)}.
     * Test method for {@link SQLUtil#closeQuietly(Statement)}.
     */
    @Test
    public void withNull() {
        // attempt to close on a null..
        SQLUtil.closeQuietly((Connection)null);
        SQLUtil.closeQuietly((ResultSet)null);
        SQLUtil.closeQuietly((Statement)null);
    }
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test(expected=NullPointerException.class)
    public void testBuildConnectorObjectBuilderNull() throws SQLException {
        SQLUtil.getAttributeSet(null);
    }
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertBlobToAttribute() throws SQLException {
        ExpectProxy<Blob> tb = new ExpectProxy<Blob>();
        byte[] expected = new byte[] {'a','h','o','j'};
        final ByteArrayInputStream is = new ByteArrayInputStream(expected);
        tb.expectAndReturn("getBinaryStream", is);
        Blob blob = tb.getProxy(Blob.class);
        Attribute actual = SQLUtil.convertToAttribute("test", blob);
        final Object object = AttributeUtil.getSingleValue(actual);
        assertEquals(expected[0], ((byte[]) object)[0]);
        assertEquals(expected[3], ((byte[]) object)[3]);
        assertTrue(tb.isDone());
    }

    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertSringToAttribute() throws SQLException {
        String expected = "test";
        Attribute actual = SQLUtil.convertToAttribute(expected, expected);
        final Object object = AttributeUtil.getSingleValue(actual);
        assertEquals(expected, object);
    }

    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertDateToAttribute() throws SQLException {
        Timestamp src = new Timestamp(System.currentTimeMillis());
        long expected = src.getTime();
        Attribute actual = SQLUtil.convertToAttribute("test", src);
        final Object object = AttributeUtil.getSingleValue(actual);
        assertEquals(expected, object);
    }    
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertTimestampClassNameToJDBC() {
        Timestamp expected = new Timestamp(System.currentTimeMillis());
        long src = expected.getTime();
        Object actual = SQLUtil.convertToJDBC(src, expected.getClass().getName());
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }   
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertTimestampToJDBC() {
        Timestamp expected = new Timestamp(System.currentTimeMillis());
        long src = expected.getTime();
        Object actual = SQLUtil.convertToJDBC(src, expected.getClass());
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertDateToJDBC() {
        Date expected = new Date(System.currentTimeMillis());
        long src = expected.getTime();
        Object actual = SQLUtil.convertToJDBC(src, expected.getClass());
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }    
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertNullToJDBC() {
        Object actual = SQLUtil.convertToJDBC(null, Long.class);
        assertNull(actual);
    }    
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertSqlDateToJDBC() {
        java.sql.Date expected = new java.sql.Date(System.currentTimeMillis());
        long src = expected.getTime();
        Object actual = SQLUtil.convertToJDBC(src, expected.getClass());
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }  
   
        
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertStringToJDBC() {
        String expected = "test";
        Object actual = SQLUtil.convertToJDBC(expected, expected.getClass());
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }  

    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testConvertLongToJDBC() {
        Long expected = 10l;
        Object actual = SQLUtil.convertToJDBC(expected, expected.getClass());
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected, actual);
    }     
    

    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testNormalizeNullValues() {
        final String sql = "insert into table values(?, ?, ?)";
        final List<Object> params = new ArrayList<Object>();
        params.add("test");
        params.add(null);
        params.add(1);
        final List<Object> out = new ArrayList<Object>();
        String actual = SQLUtil.normalizeNullValues(sql, params, out);
        assertNotNull("sql",actual);
        assertEquals("sql","insert into table values(?, null, ?)", actual);
        assertEquals("out value", 2, out.size());
    }    

    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testNormalizeNullValuesSame() {
        final String sql = "insert into table values(?, ?, ?)";
        final List<Object> params = new ArrayList<Object>();
        params.add("test");
        params.add(3);
        params.add(1);
        final List<Object> out = new ArrayList<Object>();
        String actual = SQLUtil.normalizeNullValues(sql, params, out);
        assertNotNull("sql",actual);
        assertEquals("sql","insert into table values(?, ?, ?)", actual);
        assertEquals("out value", 3, out.size());
    }  
    
    /**
     * Test method
     * @throws SQLException 
     */
    @Test
    public void testNormalizeNullValuesLess() {
        final String sql = "insert into table values(?, ?, ?)";
        final List<Object> params = new ArrayList<Object>();
        params.add("test");
        params.add(3);
        final List<Object> out = new ArrayList<Object>();
        try {
            SQLUtil.normalizeNullValues(sql, params, out);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
            // expected
        }
        params.add(3);
        params.add(3);
        try {
            SQLUtil.normalizeNullValues(sql, params, out);
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
            // expected
        }
    } 
    /**
     * GetAttributeSet test method
     * @throws SQLException 
     */
    @Test
    public void testGetAttributeSet() throws SQLException {
        final String TEST1 = "test1";
        final String TEST_VAL1 = "testValue1";
        final String TEST2 = "test2";
        final String TEST_VAL2 = "testValue2";

        //Resultset
        final ExpectProxy<ResultSet> trs = new ExpectProxy<ResultSet>();
        ResultSet resultSetProxy = trs.getProxy(ResultSet.class);
        
        //Metadata
        final ExpectProxy<ResultSetMetaData> trsmd = new ExpectProxy<ResultSetMetaData>();
        ResultSetMetaData metaDataProxy = trsmd.getProxy(ResultSetMetaData.class);

        trs.expectAndReturn("getMetaData",metaDataProxy);
        trsmd.expectAndReturn("getColumnCount", 2);
        trsmd.expectAndReturn("getColumnName", TEST1);
        trs.expectAndReturn("getObject", TEST_VAL1);
        trsmd.expectAndReturn("getColumnName", TEST2);        
        trs.expectAndReturn("getObject", TEST_VAL2);
        
        final Set<Attribute> actual = SQLUtil.getAttributeSet(resultSetProxy);
        assertTrue(trs.isDone());
        assertTrue(trsmd.isDone());
        assertEquals(2, actual.size());
        assertNotNull(AttributeUtil.find(TEST1, actual));
        assertNotNull(AttributeUtil.find(TEST2, actual));
        assertEquals(TEST_VAL1,AttributeUtil.find(TEST1, actual).getValue().get(0));
        assertEquals(TEST_VAL2,AttributeUtil.find(TEST2, actual).getValue().get(0));
     }
    
	/**
	 * We need this helper class as InitialContextFactory class name value to Hashtable into InitialContext.
	 * We must use instantiable classname and class must be accessible
	 * @author kitko
	 *
	 */
    public static class MockContextFactory implements InitialContextFactory{
		public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
			ExpectProxy<DataSource> dsProxy = new ExpectProxy<DataSource>();
			dsProxy.expectAndReturn("getConnection", new ExpectProxy<Connection>().getProxy(Connection.class));
			ExpectProxy<Context> ctxProxy = new ExpectProxy<Context>();
			ctxProxy.expectAndReturn("lookup", dsProxy.getProxy(DataSource.class));
			return ctxProxy.getProxy(Context.class);
		}
	}
    
    /**
     * Tests getting connection from dataSource
     */
    @Test
    public void testGetConnectionFromDS(){
    	Hashtable<String,String> properties = new Hashtable<String, String>();
    	properties.put("java.naming.factory.initial",MockContextFactory.class.getName());
    	final Connection conn = SQLUtil.getDatasourceConnection("",properties);
    	assertNotNull("Connection returned from datasource is null",conn);
    	
    }
}
