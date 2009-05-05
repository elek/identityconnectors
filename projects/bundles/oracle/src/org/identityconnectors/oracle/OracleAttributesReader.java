package org.identityconnectors.oracle;

import static org.identityconnectors.oracle.OracleConnector.ORACLE_AUTHENTICATION_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_DEF_TS_QUOTA_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_GLOBAL_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_PROFILE_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_TEMP_TS_ATTR_NAME;
import static org.identityconnectors.oracle.OracleConnector.ORACLE_TEMP_TS_QUOTA_ATTR_NAME;

import java.util.Map;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

/** Transforms attributes from Set<Attribute> attrs to {@link OracleUserAttributes}.
 *  It checks just nullability of attributes and makes some possible value checks.
 *  It does not do any additional logical checks, it is mainly reader of attributes to the helper structure.
 * */
final class OracleAttributesReader {
     final ConnectorMessages messages;
     
     OracleAttributesReader(ConnectorMessages messages){
         this.messages = OracleConnectorHelper.assertNotNull(messages, "messages");
     }
    
     void readCreateAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes){
    	 readAuthAttributes(map, caAttributes);
    	 readRestAttributes(map, caAttributes);
     }
     
     void readAlterAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes){
    	 readAuthAttributes(map, caAttributes);
    	 readRestAttributes(map, caAttributes);
     }
     
     
     
     private void readRestAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes) {
        caAttributes.setExpirePassword(OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.PASSWORD_EXPIRED_NAME));
        caAttributes.setDefaultTableSpace(OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_DEF_TS_ATTR_NAME));
        caAttributes.setTempTableSpace(OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_TEMP_TS_ATTR_NAME));
        caAttributes.setEnable(OracleConnectorHelper.getNotNullAttributeBooleanValue(map, OperationalAttributes.ENABLE_NAME));
        caAttributes.setProfile(OracleConnectorHelper.getNotNullAttributeNotEmptyStringValue(map, ORACLE_PROFILE_ATTR_NAME));
        
        Attribute defaultTSQuota = map.get(ORACLE_DEF_TS_QUOTA_ATTR_NAME);
        if(defaultTSQuota != null){
        	String val = AttributeUtil.getStringValue(defaultTSQuota);
        	if(StringUtil.isBlank(val)){
        		//when updating to null, actuall we want to drop quouta information and this will
        		//be done altering to 0
        		caAttributes.setDefaultTSQuota("0");
        	}
        	else{
        		caAttributes.setDefaultTSQuota(val);
        	}
        }
        
        Attribute tempTSQuota = map.get(ORACLE_TEMP_TS_QUOTA_ATTR_NAME);
        if(tempTSQuota != null){
        	String val = AttributeUtil.getStringValue(tempTSQuota);
        	if(StringUtil.isBlank(val)){
        		//when updating to null, actuall we want to drop quouta information and this will
        		//be done altering to 0
        		caAttributes.setTempTSQuota("0");
        	}
        	else{
        		caAttributes.setTempTSQuota(val);
        	}
        }
    }

    private void readAuthAttributes(Map<String, Attribute> map, OracleUserAttributes.Builder caAttributes) {
        String authentication =  OracleConnectorHelper.getStringValue(map, ORACLE_AUTHENTICATION_ATTR_NAME);
        Attribute passwordAttribute = map.get(OperationalAttributes.PASSWORD_NAME);
        //Set globalname to not silently skip it
        caAttributes.setGlobalName(OracleConnectorHelper.getStringValue(map, ORACLE_GLOBAL_ATTR_NAME));
        caAttributes.setPassword(passwordAttribute != null ? AttributeUtil.getGuardedStringValue(passwordAttribute) : null);
        if(authentication != null){
	        try{
	        	caAttributes.setAuth(OracleAuthentication.valueOf(authentication));
	        }
	        catch(IllegalArgumentException e){
	        	throw new IllegalArgumentException(messages.format(OracleMessages.INVALID_AUTH, OracleMessages.INVALID_AUTH, authentication));
	        }
	        switch(caAttributes.getAuth()){
		        case LOCAL :
		        	//We will set default password in sql builder
		            break;
		        case EXTERNAL : break;
		        case GLOBAL : 
		        	//Now globalname is required
		        	caAttributes.setGlobalName(OracleConnectorHelper.getNotEmptyStringValue(map, ORACLE_GLOBAL_ATTR_NAME));
		        	break;
	        }
        }
    }

}
