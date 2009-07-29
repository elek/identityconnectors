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

import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.DatabaseQueryBuilder;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.operations.SearchOp;

/**
 * The Account CreateOp implementation of the SPI Select attributes from fnd_user table, add person details from
 * PER_PEOPLE_F table add responsibility names add auditor data add securing attributes all filtered according
 * attributes to get
 * 
 * @author Petr Jung
 * @version $Revision 1.0$
 * @since 1.0
 */
final class AccountOperationSearch extends Operation implements SearchOp<FilterWhereBuilder>, ColumnNameResolver {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(AccountOperationSearch.class);

    OracleERPConnector co;

    /**
     * @param conn
     * @param cfg
     */
    protected AccountOperationSearch(OracleERPConnection conn, OracleERPConfiguration cfg, OracleERPConnector co) {
        super(conn, cfg);
        this.co = co;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        return new OracleERPFilterTranslator(oclass, options, AccountSQL.FND_USER_COLS, this);
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        final String method = "executeQuery";

        final String tblname = cfg.app() + "fnd_user";
        final Set<AttributeInfo> ais = getAttributeInfos(cfg.getSchema(), ObjectClass.ACCOUNT_NAME);
        final Set<String> attributesToGet = getAttributesToGet(options, ais);
        final Set<String> fndUserColumnNames = getColumnNamesToGet(attributesToGet);
        final Set<String> perPeopleColumnNames = CollectionUtil.newSet(fndUserColumnNames);
        final String filterId = getFilterId(where);

        fndUserColumnNames.retainAll(AccountSQL.FND_USER_COLS);
        perPeopleColumnNames.retainAll(AccountSQL.PER_PEOPLE_COLS);

        // For all user query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, fndUserColumnNames);
        String sqlSelect = query.getSQL();

        if (StringUtil.isNotBlank(cfg.getAccountsIncluded())) {
            sqlSelect += whereAnd(sqlSelect, cfg.getAccountsIncluded());
        } else if (cfg.isActiveAccountsOnly()) {
            sqlSelect += whereAnd(sqlSelect, ACTIVE_ACCOUNTS_ONLY_WHERE_CLAUSE);
        }

        query.setWhere(where);

        ResultSet result = null;
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            while (result.next()) {
                AttributeMergeBuilder amb = new AttributeMergeBuilder(attributesToGet);
                final Map<String, SQLParam> columnValues = SQLUtil.getColumnValues(result);
                final SQLParam userNameParm = columnValues.get(USER_NAME);
                final String userName = (String) userNameParm.getValue();
                final boolean getAuditorData = userNameParm.getValue().toString().equals(filterId);
                // get users account attributes
                buildAccountObject(amb, columnValues);

                // if person_id not null and employee_number in schema, return employee_number
                buildPersonDetails(amb, columnValues, perPeopleColumnNames);

                // get users responsibilities only if if resp || direct_resp in account attribute
                co.getRespNames().buildResponsibilitiesToAccountObject(amb, userName);
                // get user's securing attributes
                co.getSecAttrs().buildSecuringAttributesToAccountObject(amb, userName);

                //Auditor data for get user only
                log.info("get auditor data: {0}", getAuditorData);
                if (getAuditorData) {
                    co.getRespNames().buildAuditorDataObject(amb, userName);
                }

                // create the connector object..
                ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                bld.setObjectClass(ObjectClass.ACCOUNT);
                bld.addAttributes(amb.build());
                if (!handler.handle(bld.build())) {
                    break;
                }
            }
            conn.commit();
            log.ok(method);
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(result);
            SQLUtil.closeQuietly(statement);
        }
    }

    /**
     * @param attributesToGet
     *            from application
     * @return the set of the column names
     */
    private Set<String> getColumnNamesToGet(Set<String> attributesToGet) {
        Set<String> columnNamesToGet = CollectionUtil.newCaseInsensitiveSet();

        // Replace attributes to quoted columnNames
        for (String attributeName : attributesToGet) {
            final String columnName = getColumnName(attributeName);
            if (columnName != null) {
                columnNamesToGet.add(columnName);
            }
        }
        //We always wont to have user id and user name
        columnNamesToGet.add(USER_NAME);

        log.ok("columnNamesToGet {0}", columnNamesToGet);
        return columnNamesToGet;
    }

    /**
     * @param bld
     * @param columnValues
     * @param columnNames
     */
    private void buildPersonDetails(AttributeMergeBuilder bld, final Map<String, SQLParam> columnValues,
            Set<String> personColumns) {

        if (columnValues == null || columnValues.get(EMP_ID) == null) {
            // No personId(employId)
            log.ok("buildPersonDetails: No personId(employId)");
            return;
        }
        final BigDecimal personId = (BigDecimal) columnValues.get(EMP_ID).getValue();
        if (personId == null) {
            log.ok("buildPersonDetails: Null personId(employId)");
            return;
        }
        log.info("buildPersonDetails for personId: {0}", personId);

        //Names to get filter
        final String tblname = cfg.app() + "PER_PEOPLE_F";

        if (personColumns.isEmpty()) {
            // No persons column required
            log.ok("No persons column To Get");
            return;
        }
        log.ok("personColumns {0} To Get", personColumns);

        // For all account query there is no need to replace or quote anything
        final DatabaseQueryBuilder query = new DatabaseQueryBuilder(tblname, personColumns);
        final FilterWhereBuilder where = new FilterWhereBuilder();
        where.addBind(new SQLParam(PERSON_ID, personId, Types.DECIMAL), "=");
        query.setWhere(where);

        final String sql = query.getSQL();
        String msg = "Oracle ERP: sql = ''{0}''";
        log.info(msg, sql);

        ResultSet result = null; // SQL query on person_id
        PreparedStatement statement = null; // statement that generates the query
        try {
            statement = conn.prepareStatement(query);
            result = statement.executeQuery();
            if (result != null) {
                log.ok("executeQuery {0}", query.getSQL());
                if (result.next()) {
                    final Map<String, SQLParam> personValues = SQLUtil.getColumnValues(result);
                    // get users account attributes
                    this.buildAccountObject(bld, personValues);
                    log.ok("Person values {0} from result set ", personValues);
                }
            }

        } catch (SQLException e) {
            String emsg = e.getMessage();
            msg = "Caught SQLException when executing: ''{0}'': {1}";
            log.error(msg, sql, emsg);
            SQLUtil.rollbackQuietly(conn);
            throw new ConnectorException(msg, e);
        } finally {
            SQLUtil.closeQuietly(result);
            result = null;
            SQLUtil.closeQuietly(statement);
            statement = null;
        }
    }

    /**
     * Construct a connector object
     * <p>
     * Taking care about special attributes
     * </p>
     * 
     * @param attributeSet
     *            from the database table
     * @param columnValues
     * @throws SQLException
     */
    private void buildAccountObject(AttributeMergeBuilder amb, Map<String, SQLParam> columnValues) throws SQLException {
        final String method = "buildAccountObject";
        log.info(method);
        for (Map.Entry<String, SQLParam> val : columnValues.entrySet()) {
            final SQLParam param = val.getValue();
            final String columnName = param.getName();
            // Map the special attribute
            // TODO add all special attributes to account
            if (columnName.equalsIgnoreCase(USER_NAME)) {
                if (param == null || param.getValue() == null) {
                    String msg = "Name cannot be null.";
                    throw new IllegalArgumentException(msg);
                }
                amb.addAttribute(Name.NAME, param.getValue().toString());
                amb.addAttribute(Uid.NAME, param.getValue().toString());
            } else {
                mergeAttribute(amb, param);
            }
        }
    }

    /**
     * @param amb
     *            AttributeMergeBuilder
     * @param param
     *            SQLParam
     * @throws SQLException
     *             if something wrong
     */
    private void mergeAttribute(AttributeMergeBuilder amb, final SQLParam param) throws SQLException {
        final String columnName = param.getName();
        //Convert the data type and create attribute from it.
        final String attributeName = getAttributeName(columnName);
        final Set<String> readable = getReadableAttributes(getAttributeInfos(cfg.getSchema(), ObjectClass.ACCOUNT_NAME));
        //  Add only readable attributes
        if (readable.contains(attributeName)) {
            final Object value = SQLUtil.jdbc2AttributeValue(param.getValue());
            amb.addAttribute(attributeName, value);
        }
    }
    
    /**
     * @param columnName
     * @return the columnName
     */
    private String getAttributeName(String columnName) {
        if (FULL_NAME.equalsIgnoreCase(columnName)) {
            return PERSON_FULLNAME;
        }
        return columnName;
    }

    /**
     * @param attributeName
     * @return the columnName
     */
    public String getColumnName(String attributeName) {
        if (Name.NAME.equalsIgnoreCase(attributeName)) {
            return USER_NAME;
        } else if (Uid.NAME.equalsIgnoreCase(attributeName)) {
            return USER_NAME;
        } else if (PERSON_FULLNAME.equalsIgnoreCase(attributeName)) {
            return FULL_NAME;
        }
        return attributeName;
    }

}
