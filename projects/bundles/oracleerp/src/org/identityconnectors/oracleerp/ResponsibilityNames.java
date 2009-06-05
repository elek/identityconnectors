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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;

/**
 * Main implementation of the OracleErp Connector
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class ResponsibilityNames {

    /**
     * Setup logging.
     */
    static final Log log = Log.getLog(ResponsibilityNames.class); 

    /**
     * The get Instance method
     * @param connector parent
     * @return the account
     */
    public static ResponsibilityNames getInstance(OracleERPConnector connector) {
       return new ResponsibilityNames(connector);
    }

    /**
     * The parent connector
     */
    private OracleERPConnector co;

    /**
     * If 11.5.10, determine if description field exists in responsibility views. Default to true
     */
    private boolean descrExists = true;

    /**
     * Check to see which responsibility account attribute is sent. Version 11.5.9 only supports responsibilities, and
     * 11.5.10 only supports directResponsibilities and indirectResponsibilities Default to false If 11.5.10, determine
     * if description field exists in responsibility views.
     */
    private boolean newResponsibilityViews = false;
    
    
    /**
     * Accessor for the descrExists property
     * @return the descrExists
     */
    public boolean isDescrExists() {
        return descrExists;
    }


    /**
     * Accessor for the newResponsibilityViews property
     * @return the newResponsibilityViews
     */
    public boolean isNewResponsibilityViews() {
        return newResponsibilityViews;
    }    
    
    /**
     * Responsibility Application Id
     */
    private String respApplId = "";

    /**
     * Responsibility Id
     */
    private String respId = "";
    
    /**
     * The ResponsibilityNames
     * @param connector parent
     */
    private ResponsibilityNames(OracleERPConnector connector) {
        this.co = connector;
    }
    
    /**
     * @param bld
     * @param columnValues
     * @param columnNames 
     */
    public void buildResponsibilitiesToAccountObject(ConnectorObjectBuilder bld, Map<String, SQLParam> columnValues, Set<String> columnNames) {
        final String id = getStringParamValue(columnValues, USER_ID);
        if (columnNames.contains(RESP) && !isNewResponsibilityViews()) {
            //add responsibilities
            final List<String> responsibilities = getResponsibilities(id, RESPS_TABLE, false);
            bld.addAttribute(RESP, responsibilities);

            //add resps list
            final List<String> resps = getResps(responsibilities, RESP_FMT_KEYS);
            bld.addAttribute(RESPKEYS, resps);
        } else if (columnNames.contains(DIRECT_RESP)) {
            final List<String> responsibilities = getResponsibilities(id, RESPS_DIRECT_VIEW, false);
            bld.addAttribute(DIRECT_RESP, responsibilities);

            //add resps list
            final List<String> resps = getResps(responsibilities, RESP_FMT_KEYS);
            bld.addAttribute(RESPKEYS, resps);
        }

        if (columnNames.contains(INDIRECT_RESP) ) {
            //add responsibilities
            final List<String> responsibilities = getResponsibilities(id, RESPS_INDIRECT_VIEW, false);
            bld.addAttribute(INDIRECT_RESP, responsibilities);
        }       
    }
    
    /**
     * The New responsibility format there
     * 
     * @return true/false
     */
    public boolean getNewResponsibilityViews() {
        final String sql = "select * from " + co.app()
                + "fnd_views where VIEW_NAME = 'FND_USER_RESP_GROUPS_DIRECT' and APPLICATION_ID = '0'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = co.getConn().prepareStatement(sql);
            res = ps.executeQuery();
            log.ok(sql);
            if (res != null && res.next()) {
                log.ok("ResponsibilityViews exists");
                return true;
            }
        } catch (SQLException e) {
            log.error(e, sql);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        log.ok("ResponsibilityViews does not exists");
        return false;
    }
    

    /**
     * bug#13889 : Added method to create a responsibility string with dates normalized.
     * respFmt: 
     *   RESP_FMT_KEYS: get responsibility keys (resp_name, app_name, sec_group)
     *   RESP_FMT_NORMALIZE_DATES: get responsibility string (resp_name, app_name, sec_group, description, start_date, end_date)
     *                             start_date, end_date (no time data, allow nulls)
     * @param strResp 
     * @param respFmt 
     * @return normalized resps string
     */
    public String getResp(String strResp, int respFmt) {
        final String method = "getResp(String, int)";
        log.info(method + "respFmt=" + respFmt);
        String strRespRet = null;
        StringTokenizer tok = new StringTokenizer(strResp, "||", false);
        if (tok != null && tok.countTokens() > 2) {
            StringBuffer key = new StringBuffer();
            key.append(tok.nextToken()); // responsiblity name
            key.append("||");
            key.append(tok.nextToken()); // application name
            key.append("||");
            key.append(tok.nextToken()); // security group name
            if (respFmt != RESP_FMT_KEYS) {
                key.append("||");
                // descr possibly not available in ui version 11.5.10
                if (!isNewResponsibilityViews() || isDescrExists()) {
                    key.append(tok.nextToken()); // description
                }
                key.append("||");
                key.append(normalizeStrDate(tok.nextToken())); // start_date
                key.append("||");
                key.append(normalizeStrDate(tok.nextToken())); // end_date
            }
            strRespRet = key.toString();
        }
        log.ok(method);
        return strRespRet;
    } // getRespWithNormalizeDates()  
    

    /**
     * Accessor for the respApplId property
     * @return the respApplId
     */
    public String getRespApplId() {
        return respApplId;
    }

    
    /**
     * Accessor for the respId property
     * @return the respId
     */
    public String getRespId() {
        return respId;
    }    
    

    /**
     * Init the responsibilities
     */
    public void initResponsibilities() {
        this.newResponsibilityViews = getNewResponsibilityViews();
        
        if (isNewResponsibilityViews()) {
            this.descrExists = getDescriptionExiests();
        }
        
        // three pieces of data need for apps_initialize()
        final String auditResponsibility = co.getCfg().getAuditResponsibility();
        final String userId = OracleERPUtil.getUserId(co, co.getCfg().getUser());

        if (StringUtil.isNotBlank(auditResponsibility)) {
            if (StringUtil.isNotBlank(userId)) {
                co.getSecAttrs().initAdminUserId(userId);
            }

            final String view = co.app()
                    + ((isNewResponsibilityViews()) ? OracleERPUtil.RESPS_ALL_VIEW : OracleERPUtil.RESPS_TABLE);
            final String sql = "select responsibility_id, responsibility_application_id from "
                    + view
                    + " where user_id = ? and "
                    + "(responsibility_id,responsibility_application_id) = (select responsibility_id,application_id from "
                    + "{0}fnd_responsibility_vl where responsibility_name = ?)";

            final String msg = "Oracle ERP SQL: {0} returned: RESP_ID = {1}, RESP_APPL_ID = {2}";

            ArrayList<SQLParam> params = new ArrayList<SQLParam>();
            params.add(new SQLParam(userId));
            params.add(new SQLParam(auditResponsibility));
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                log.info("Select responsibility for user_id: {0}, and audit responsibility {1}", userId,
                        auditResponsibility);
                ps = co.getConn().prepareStatement(sql, params);
                rs = ps.executeQuery();
                if (rs != null) {
                    if (rs.next()) {
                        respId = rs.getString(1);
                        respApplId = rs.getString(2);
                    }
                }

                log.ok(msg, sql, respId, respApplId);
            } catch (SQLException e) {
                log.error(e, msg, sql, respId, respApplId);
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(rs);
                rs = null;
                SQLUtil.closeQuietly(ps);
                ps = null;
            }
        }
    }    
    
    
    /**
     * Get the Account Object Class Info
     * 
     * @return ObjectClassInfo value
     */
    public ObjectClassInfo getSchema() {
        ObjectClassInfoBuilder aoc = new ObjectClassInfoBuilder();
        aoc.setType(RESP_NAMES);

        // The Name is supported attribute
        aoc.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='userMenuNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(USER_MENU_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='menuIds' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(MENU_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='userFunctionNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(USER_FUNCTION_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='functionIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='formIds' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FORM_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='formNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FORM_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='functionNames' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(FUNCTION_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='userFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(USER_FORM_NAMES, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='readOnlyFormIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='readWriteOnlyFormIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_IDS, String.class, EnumSet.of(Flags.REQUIRED)));
        // name='readOnlyFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FORM_NAMES, String.class, EnumSet
                        .of(Flags.REQUIRED)));
        // name='readOnlyFunctionNames' type='string' audit='false'    
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTION_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readOnlyUserFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_USER_FORM_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readOnlyFunctionIds' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_ONLY_FUNCTIONS_IDS, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FORM_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyUserFormNames' type='string' audit='false'
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_USER_FORM_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyFunctionNames' type='string' audit='false'        
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_NAMES, String.class, EnumSet
                .of(Flags.REQUIRED)));
        // name='readWriteOnlyFunctionIds' type='string' audit='false'                 
        aoc.addAttributeInfo(AttributeInfoBuilder.build(READ_WRITE_ONLY_FUNCTION_IDS, String.class, EnumSet
                .of(Flags.REQUIRED)));
        return aoc.build();
    }

    
    
    /**
     * getResponsibilities
     * 
     * @param id user id
     * @param respLocation The responsibilities table
     * @param activeOnly select active only
     * @return list of strings of multivalued attribute
     */
    public List<String> getResponsibilities(String id, String respLocation, boolean activeOnly) {

        final String method = "getResponsibilities";
        log.info(method);

        StringBuffer b = new StringBuffer();

        b.append("SELECT fndappvl.application_name, fndrespvl.responsibility_name, ");
        b.append("fndsecgvl.Security_group_name ");
        // descr may not be available in view or in native ui with new resp views
        // bug#15492 - do not include user tables in query if id not specified, does not return allr responsibilities
        if (id != null) {
            if (!isNewResponsibilityViews()
                    || (isDescrExists() && respLocation.equalsIgnoreCase(RESPS_DIRECT_VIEW))) {
                b.append(", fnduserg.DESCRIPTION");
            }
            b.append(", fnduserg.START_DATE, fnduserg.END_DATE ");
        }
        b.append("FROM " + co.app() + "fnd_responsibility_vl fndrespvl, ");
        b.append(co.app() + "fnd_application_vl fndappvl, ");
        // bug#15492 - don't include this join if no id is specified.
        if (id != null) {
            b.append(co.app() + "fnd_user fnduser, ");
            b.append(co.app() + respLocation + " fnduserg, ");
        }
        b.append(co.app() + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        // bug#15492 - don't include this join if no id is specified.
        if (id != null) {
            b.append("AND fnduser.user_id = fnduserg.user_id ");
            b.append("AND fndrespvl.RESPONSIBILITY_ID = fnduserg.RESPONSIBILITY_ID ");
            b.append("AND fndrespvl.APPLICATION_ID = fnduserg.RESPONSIBILITY_APPLICATION_ID ");
            b.append("AND fnduser.USER_NAME = ? ");
            b.append("AND fndsecgvl.security_group_id = fnduserg.security_group_id ");
        }
        if (activeOnly) {
            if (id != null) {
                b.append(" AND fnduserg.START_DATE - SYSDATE <= 0 "
                        + "AND (fnduserg.END_DATE IS NULL OR fnduserg.END_DATE - SysDate > 0)");
            }
        }

        PreparedStatement st = null;
        ResultSet res = null;
        List<String> arrayList = new ArrayList<String>();
        final String sql = b.toString();
        try {
            log.info("sql select {0}", sql);
            st = co.getConn().prepareStatement(sql);
            if (id != null) {
                st.setString(1, id.toUpperCase());
            }
            res = st.executeQuery();
            while (res.next()) {

                // six columns with old resp table, 5 with new views - 
                // no description available
                StringBuffer sb = new StringBuffer();
                String s = getColumn(res, 2); // fndrespvl.responsibility_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 1); // fndappvl.application_name
                sb.append(s);
                sb.append("||");
                s = getColumn(res, 3); // fndsecgvl.Security_group_name
                sb.append(s);
                sb.append("||");
                if (id != null) {
                    s = getColumn(res, 4); // fnduserg.DESCRIPTION or fnduserg.START_DATE
                    sb.append(s);
                }
                sb.append("||");
                if (id != null) {
                    s = getColumn(res, 5); // fnduserg.START_DATE or fnduserg.END_DATE
                    sb.append(s);
                }
                if (!isNewResponsibilityViews()
                        || (isDescrExists() && respLocation.equalsIgnoreCase(RESPS_DIRECT_VIEW))) {
                    sb.append("||");
                    if (id != null) {
                        s = getColumn(res, 6); // fnduserg.END_DATE
                        sb.append(s);
                    }
                }

                arrayList.add(sb.toString());
            }
        } catch (SQLException e) {
            log.error(e, method);
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(st);
            st = null;
        }

        log.ok(method);
        return arrayList;
    }    


    /**
     * bug#13889 : Added method to create a responsibilities list with dates normalized.
     * RESP_FMT_KEYS: get responsibility keys (resp_name, app_name, sec_group)
     * RESP_FMT_NORMALIZE_DATES: get responsibility keys (resp_name, app_name, sec_group, description, start_date, end_date)
     *   
     * @param resps
     * @param respFmt
     * @return list of Sting
     */
    public List<String> getResps(List<String> resps, int respFmt) {
        final String method = "getResps(ArrayList, int)";
        log.info(method + " respFmt=" + respFmt);
        List<String> respKeys = null;
        if (resps != null) {
            respKeys = new ArrayList<String>();
            for (int i = 0; i < resps.size(); i++) {
                String strResp = resps.get(i);
                String strRespReformatted = getResp(strResp, respFmt);
                log.info(method + " strResp='" + strResp + "', strRespReformatted='" + strRespReformatted + "'");
                respKeys.add(strRespReformatted);
            }
        }
        log.ok(method);
        return respKeys;
    } // getResps()  


    /**
     * 
     * @param attr resp attribute
     * @param identity
     * @param result
     * @throws WavesetException
     */
    public void updateUserResponsibilities(final Attribute attr, final String identity) {
        final String method = "updateUserResponsibilities";
        log.info(method);

        final List<String> errors = new ArrayList<String>();        
        final List<String> respList = new ArrayList<String>();
        for (Object obj : attr.getValue()) {
            respList.add(obj.toString());
        }

        // get Users Current Responsibilties
        List<String> oldResp = null;
        if (!isNewResponsibilityViews()) {
            oldResp = getResponsibilities(identity, RESPS_TABLE, false);
        } else {
            // can only update directly assigned resps; indirect resps are readonly
            // thru ui            
            oldResp = getResponsibilities(identity, RESPS_DIRECT_VIEW, false);
        }
        //preserve the previous behavior where oldResp is never null.
        if (oldResp == null) {
            oldResp = new ArrayList<String>();
        }
        List<String> oldRespKeys = getResps(oldResp, RESP_FMT_KEYS);
        List<String> newRespKeys = getResps(respList, RESP_FMT_KEYS);
        // bug#13889
        // create responsibilities list with dates normalized i.e., with no time data.
        // We ignore the time data due to potential time differences between the Oracle DB environment and the IDM client.
        // start and end dates are specified as date only from the Oracle Application GUI.
        List<String> oldRespsWithNormalizedDates = getResps(oldResp, RESP_FMT_NORMALIZE_DATES);
        // if old key is not in new list, delete it
        if (oldRespKeys != null) {
            if (!oldRespKeys.isEmpty()) {
                int index = 0;
                Iterator<String> it = oldRespKeys.iterator();
                while (it.hasNext()) {
                    Object resp = it.next();
                    if (!newRespKeys.contains(resp)) {
                        // bug#9637 check to see if resp is already 
                        // endDated (disabled), if so, ignore, if not,
                        // delete resp from User
                        java.util.Date curDate = getCurrentDate();
                        java.sql.Date endDate = null;
                        boolean delResp = false;
                        String respStr = oldResp.get(index);
                        StringTokenizer tok = new StringTokenizer(respStr, "||", false);
                        if (tok != null) {
                            String endDateStr = null;
                            while (tok.hasMoreTokens()) {
                                endDateStr = tok.nextToken();
                            }
                            if (endDateStr != null && !endDateStr.equalsIgnoreCase("null")) {
                                // format date input
                                int i = endDateStr.indexOf(" ");
                                endDate = java.sql.Date.valueOf(endDateStr.substring(0, i));
                                delResp = endDate.after(curDate);
                            } else {
                                delResp = true;
                            }
                        }
                        if (delResp) {
                            deleteUserResponsibility(identity, (String) resp, errors);
                            log.error("deleted, (end_dated), responsibility: '" + resp + "' for " + identity);
                        }
                    }
                    index++;
                }
            }
        }
        // if new key is not in old list add it and remove from respList
        // after adding
        if (respList != null) {
            if (!respList.isEmpty()) {
                // make copy of array to itereate through because we will be
                // modifying the respList
                List<String> resps = new ArrayList<String>(respList);
                Iterator<String> it = resps.iterator();
                String resp = null;
                while (it.hasNext()) {
                    resp = it.next();
                    // Add/Update resp to user
                    String respKey = getResp(resp, RESP_FMT_KEYS);
                    if (!resp.equalsIgnoreCase("") && !oldRespKeys.contains(respKey)) {
                        addUserResponsibility(identity, resp, errors);
                        respList.remove(resp);
                        log.info("added responsibility: '" + resp + "' for " + identity);
                    }
                }// end-while
            }//end-if
        }//end-if
        // if new key is both lists, update it
        if (respList != null) {
            if (!respList.isEmpty()) {
                Iterator<String> it = respList.iterator();
                String resp = null;
                String respWithNormalizedDates = null;
                while (it.hasNext()) {
                    // bug#13889 -  do not update all responsibilities
                    //              only update the ones that changed.
                    //              Updating all responsibilities every time masks the audit records.
                    //              Added check to see if oldResp list 
                    //              contains the current entire responsibility
                    //              string.
                    resp = it.next();
                    if (resp != null) {
                        log.info("checking if update required for responsibility: '" + resp + "' for " + identity);
                    } else {
                        log.warn(" resp=NULL while processing updates");
                    }
                    // Add/Update resp to user
                    if (resp != null && !resp.equalsIgnoreCase("")) {
                        // normalize the date string to only contain the date, no time information.
                        respWithNormalizedDates = getResp(resp, RESP_FMT_NORMALIZE_DATES);

                        if (respWithNormalizedDates != null) {
                            log.info("respWithNormalizedDates='" + respWithNormalizedDates + "'");
                        } else {
                            log.warn("respWithNormalizedDates=null while processing updates");
                        }

                        // Add/update resp to user if the date normalized responsibility string is not in the old date normalized list.
                        if ((oldRespsWithNormalizedDates != null) && respWithNormalizedDates != null
                                && !respWithNormalizedDates.equalsIgnoreCase("")
                                && !oldRespsWithNormalizedDates.contains(respWithNormalizedDates)) {
                            updateUserResponsibility(identity, resp, errors);

                            String msg = "updated responsibility: '" + resp + "' for " + identity;
                            log.info(msg);
                        }
                    }
                }// end-while
            }//end-if
        }//end-if

        // bug#16656: delayed error handling for missing responsibilities
        if (!errors.isEmpty()) {
            StringBuilder error = new StringBuilder();
            for (int i = 0; i < errors.size(); i++) {
                String msg = errors.get(i);
                error.append(msg);
                error.append(";");
            }
            log.error(error.toString());
            throw new ConnectorException(error.toString());
        }

        log.ok(method);

    }
    
    

    private void addUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "addUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;
        String description = null;
        String fromDate = null;
        String toDate = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        int count = tok.countTokens();
        if (tok != null && count > 4) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
            // descr optionable in 11.5.10 - check if sent
            if (count > 5) {
                description = tok.nextToken();
            }
            fromDate = tok.nextToken();
            toDate = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        // descr null conversion
        if (description != null && !description.equalsIgnoreCase("null")) {
            description = "'" + description + "'";
        } else {
            description = null;
        }
        // date field convert - start_date cannot be null, set to sysdate
        if ((fromDate == null) || fromDate.equalsIgnoreCase("null")) {
            fromDate = "sysdate";
        } else if (fromDate.length() == 10) {
            fromDate = "to_date('" + fromDate + "', 'yyyy-mm-dd')";
        } else if (fromDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            fromDate = "to_date('" + fromDate.substring(0, fromDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        if ((toDate == null) || toDate.equalsIgnoreCase("null")) {
            toDate = null;
        } else if (toDate.length() == 10) {
            toDate = "to_date('" + toDate + "', 'yyyy-mm-dd')";
        } else if (toDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            toDate = "to_date('" + toDate.substring(0, toDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        StringBuffer b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, true /* doing an insert */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            log.info("execute statement ''{0}''", sql);
            st = co.getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            //
            // 19057: check whether this is a "no data found" error;
            // if so, then perhaps the responsibility we seek doesn't
            // have a valid app name.  We'll retry the query without
            // specifying the app name.
            //
            if (e.getErrorCode() == ORA_01403) {
                doRetryWithoutAppname = true;
            } else {
                final String msg = "Can not execute the sql " + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }

        if (doRetryWithoutAppname) {
            //
            // 19057: without the responsibility's application name, must
            // fall back to using just the responsibility name to identify
            // the desired responsibility
            //
            b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                    null /* respAppName is not valid */, fromDate, toDate, description, true);

            sql = b.toString();
            try {
                log.info("execute statement ''{0}''", sql);
                st = co.getConn().prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    errors.add("Failed to add '" + resp + "' responsibility:" + e.getMessage());
                } else {
                    final String msg = "Can not execute the sql " + sql;
                    log.error(e, msg);
                    throw new ConnectorException(msg, e);
                }
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.ok(method);
    }

    /**
     * This method is shared by addUserResponsibility and updateUserResponsibility to build their PL/SQL statements.
     */
    private StringBuffer buildUserRespStatement(String user, String secGroup, String respName, String respAppName,
            String fromDate, String toDate, String description, boolean doInsert) {

        StringBuffer b = new StringBuffer();
        b.append("DECLARE user varchar2(300); security_group varchar2(300); ");
        b.append("responsibility_long_name varchar2(300); ");
        if (respAppName != null) {
            b.append("responsibility_app_name varchar2(300); ");
        }
        b.append("sec_group_id Number; user_id_num Number; resp_id varchar2(300); app_id Number; sec_id Number; ");
        b.append("description varchar2(300); resp_sec_g_key varchar2(300); ");
        b.append("BEGIN user := ");
        addQuoted(b, user.toUpperCase());
        b.append("; security_group := ");
        addQuoted(b, secGroup.toUpperCase());
        b.append("; responsibility_long_name := ");
        addQuoted(b, respName);
        if (respAppName != null) {
            b.append("; responsibility_app_name := ");
            addQuoted(b, respAppName);
        }
        b.append("; ");
        b.append("SELECT responsibility_id, application_id INTO resp_id, app_id ");
        b.append("FROM " + co.app() + "fnd_responsibility_vl ");
        b.append("WHERE responsibility_name = responsibility_long_name");
        if (respAppName != null) {
            b.append(" AND application_id = ");
            b.append("(SELECT application_id FROM " + co.app() + "fnd_application_vl ");
            b.append("WHERE application_name = responsibility_app_name)");
        }
        b.append("; ");
        b.append("SELECT user_id INTO user_id_num ");
        b.append("FROM " + co.app() + "fnd_user ");
        b.append("WHERE USER_NAME = user; ");
        b.append("SELECT security_group_id INTO sec_group_id ");
        b.append("FROM " + co.app() + "fnd_security_groups_vl ");
        b.append("WHERE SECURITY_GROUP_KEY = security_group; ");

        b.append(co.app());
        if (doInsert) {
            b.append("fnd_user_resp_groups_api.Insert_Assignment (user_id_num, resp_id, app_id, sec_group_id, ");
        } else {
            b.append("fnd_user_resp_groups_api.Update_Assignment (user_id_num, resp_id, app_id, sec_group_id, ");
        }
        b.append(fromDate);
        b.append(", ");
        b.append(toDate);
        b.append(", ");
        b.append(description);
        b.append("); COMMIT; END;");

        return b;
    }

    private void deleteUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "deleteUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        if ((tok != null) && (tok.countTokens() == 3)) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        StringBuffer b = new StringBuffer();

        b.append("DECLARE user_id varchar2(300); security_group varchar2(300); ");
        b.append("responsibility_long_name varchar2(300); responsibility_app_name ");
        b.append("varchar2(300); resp_app varchar2(300); resp_key varchar2(300); ");
        b.append("description varchar2(300); resp_sec_g_key varchar2(300); ");

        b.append("BEGIN user_id := ");
        addQuoted(b, identity.toUpperCase());
        b.append("; security_group := ");
        addQuoted(b, securityGroup);
        b.append("; responsibility_long_name := ");
        addQuoted(b, respName);
        b.append("; responsibility_app_name := ");
        addQuoted(b, respAppName);
        b.append("; SELECT  fndsecg.security_group_key INTO resp_sec_g_key ");
        b.append("FROM " + co.app() + "fnd_security_groups fndsecg, " + co.app()
                + "fnd_security_groups_vl fndsecgvl ");
        b.append("WHERE fndsecg.security_group_id = fndsecgvl.security_group_id ");
        b.append("AND fndsecgvl.security_group_name = security_group; ");
        b.append("SELECT fndapp.application_short_name, fndresp.responsibility_key, ");
        b.append("fndrespvl.description INTO resp_app, resp_key, description ");
        b
                .append("FROM " + co.app() + "fnd_responsibility_vl fndrespvl, " + co.app()
                        + "fnd_responsibility fndresp, ");
        b.append(co.app() + "fnd_application_vl fndappvl, " + co.app() + "fnd_application fndapp ");
        b.append("WHERE fndappvl.application_id = fndrespvl.application_id ");
        b.append("AND fndappvl.APPLICATION_ID = fndapp.APPLICATION_ID ");
        b.append("AND fndappvl.APPLICATION_NAME = responsibility_app_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_NAME = responsibility_long_name ");
        b.append("AND fndrespvl.RESPONSIBILITY_ID = fndresp.RESPONSIBILITY_ID ");
        b.append("AND fndrespvl.APPLICATION_ID = fndresp.APPLICATION_ID; ");
        b.append(co.app() + "fnd_user_pkg.DelResp (user_id, resp_app, resp_key, resp_sec_g_key); ");
        b.append("COMMIT; END;");

        final String sql = b.toString();
        try {
            log.info("execute statement ''{0}''", sql);
            st = co.getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() == ORA_01403) {
                // bug#16656: delay error handling for missing responsibilities
                errors.add("Failed to delete '" + resp + "' responsibility:" + e.getMessage());
            } else {
                final String msg = "Can not execute the sql " + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }
        log.ok(method);
    }

    /**
     * @return
     */
    private boolean getDescriptionExiests() {
        final String sql = "select user_id, description from " + co.app()
                + "fnd_user_resp_groups_direct where USER_ID = '9999999999'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = co.getConn().prepareStatement(sql);
            res = ps.executeQuery();
            log.ok("description exists");
            return true;
        } catch (SQLException e) {
            //log.error(e, sql);
            log.ok("description does not exists");
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        return false;
    }

    private void updateUserResponsibility(String identity, String resp, List<String> errors) {
        final String method = "updateUserResponsibility";
        log.info(method);
        PreparedStatement st = null;
        String securityGroup = null;
        String respName = null;
        String respAppName = null;
        String description = null;
        String fromDate = null;
        String toDate = null;

        // * What if one of values is null in resp, will strTok still count it??
        StringTokenizer tok = new StringTokenizer(resp, "||", false);
        int count = tok.countTokens();
        if ((tok != null) && (count > 4)) {
            respName = tok.nextToken();
            respAppName = tok.nextToken();
            securityGroup = tok.nextToken();
            // descr optionable in 11.5.10 - check if sent
            if (count > 5) {
                description = tok.nextToken();
            }
            fromDate = tok.nextToken();
            toDate = tok.nextToken();
        } else {
            final String msg = "Invalid Responsibility: " + resp;
            log.error(msg);
            throw new ConnectorException(msg);
        }

        // descr null conversion
        if (description != null && !description.equalsIgnoreCase("null")) {
            description = "'" + description + "'";
        } else {
            description = null;
        }
        // date field convert - start_date cannot be null, set to sysdate
        if ((fromDate == null) || fromDate.equalsIgnoreCase("null")) {
            fromDate = "sysdate";
        } else if (fromDate.length() == 10) {
            fromDate = "to_date('" + fromDate + "', 'yyyy-mm-dd')";
        } else if (fromDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            fromDate = "to_date('" + fromDate.substring(0, fromDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        if ((toDate == null) || toDate.equalsIgnoreCase("null")) {
            toDate = null;
        } else if (toDate.equalsIgnoreCase(SYSDATE)) {
            toDate = "sysdate";
        } else if (toDate.length() == 10) {
            toDate = "to_date('" + toDate + "', 'yyyy-mm-dd')";
        } else if (toDate.length() > 10) {
            // try YYYY-MM-DD HH:MM:SS.n
            toDate = "to_date('" + toDate.substring(0, toDate.length() - 2) + "', 'yyyy-mm-dd hh24:mi:ss')";
        }

        StringBuffer b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                respAppName, fromDate, toDate, description, false /* not doing an insert, doing an update */);

        boolean doRetryWithoutAppname = false;
        String sql = b.toString();
        try {
            log.info("sql select {0}", sql);
            st = co.getConn().prepareStatement(sql);
            st.execute();
        } catch (SQLException e) {
            //
            // 19057: check whether this is a "no data found" error;
            // if so, then perhaps the responsibility we seek doesn't
            // have a valid app name.  We'll retry the query without
            // specifying the app name.
            //
            if (e.getErrorCode() == ORA_01403) {
                doRetryWithoutAppname = true;
            } else {
                final String msg = "Error in sql :" + sql;
                log.error(e, msg);
                throw new ConnectorException(msg, e);
            }
        } finally {
            SQLUtil.closeQuietly(st);
            st = null;
        }

        if (doRetryWithoutAppname) {
            //
            // 19057: without the responsibility's application name, must
            // fall back to using just the responsibility name to identify
            // the desired responsibility
            //
            b = buildUserRespStatement(identity.toUpperCase(), securityGroup.toUpperCase(), respName,
                    null /* respAppName is not valid */, fromDate, toDate, description, false);

            sql = b.toString();
            try {
                log.info("sql select {0}", sql);
                st = co.getConn().prepareStatement(sql);
                st.execute();
            } catch (SQLException e) {
                if (e.getErrorCode() == ORA_01403) {
                    // bug#16656: delay error handling for missing responsibilities
                    errors.add("Failed to update '" + resp + "' responsibility:" + e.getMessage());
                } else {
                    final String msg = "Can not execute the sql " + sql;
                    log.error(e, msg);
                    throw new ConnectorException(msg, e);
                }
            } finally {
                SQLUtil.closeQuietly(st);
                st = null;
            }
        }
        log.ok(method);
    }    
}
