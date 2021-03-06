/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.Assert;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Test;

/**
 * @author kitko
 *
 */
public class OracleCreateOrAlterStBuilderTest {

    /**
     * Test method for {@link org.identityconnectors.oracle.OracleCreateOrAlterStBuilder#buildCreateUserSt(java.lang.StringBuilder, org.identityconnectors.oracle.OracleUserAttributes)}.
     */
    @Test
    public void testCreateLocalUserSt() {
        try{
            createDefaultBuilder().buildCreateUserSt(new OracleUserAttributes.Builder().build());
            fail("Not enough info, create must fail");
        }catch(Exception e){}
        OracleUserAttributes.Builder local = new OracleUserAttributes.Builder();
        local.setUserName("user1");
        local.setAuth(OracleAuthentication.LOCAL);        
        assertEquals("create user \"user1\" identified by \"user1\"", createDefaultBuilder().buildCreateUserSt(local.build()).toString());
        local.setPassword(new GuardedString("password".toCharArray()));
        assertEquals("create user \"user1\" identified by \"password\"", createDefaultBuilder().buildCreateUserSt(local.build()).toString());
        
        local.setGlobalName("myGlobalName");
        try{
            createDefaultBuilder().buildCreateUserSt(local.build());
            fail("Must fail for extra globa name");
        }catch(Exception e){}
        
    }
    
    
    /** Test create table space */
    @Test
    public void testCreateTableSpace(){
        OracleUserAttributes.Builder attributes = new OracleUserAttributes.Builder();
        attributes.setUserName("user1");
        attributes.setDefaultTableSpace("users");
        attributes.setTempTableSpace("temp");
        attributes.setAuth(OracleAuthentication.LOCAL);
        attributes.setPassword(new GuardedString("password".toCharArray()));
        assertEquals("create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\"", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setProfile("MyProfile");
        assertEquals("create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" profile \"MyProfile\"", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
    }
    
    /** Test quotas */
    @Test
    public void testCreateQuota(){
        OracleUserAttributes.Builder attributes = new OracleUserAttributes.Builder();
        attributes.setAuth(OracleAuthentication.LOCAL);
        attributes.setUserName("user1");
        attributes.setDefaultTableSpace("users");
        attributes.setTempTableSpace("temp");
        attributes.setPassword(new GuardedString("password".toCharArray()));
        attributes.setDefaultTSQuota("-1");
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota unlimited on \"users\"",
                createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setDefaultTSQuota("32K");
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota 32K on \"users\"",
                createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setDefaultTSQuota(null);
        attributes.setTempTSQuota("-1");
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota unlimited on \"temp\"",
                createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setTempTSQuota("32M");
        assertEquals(
                "create user \"user1\" identified by \"password\" default tablespace \"users\" temporary tablespace \"temp\" quota 32M on \"temp\"",
                createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        
    }

    private OracleCreateOrAlterStBuilder createDefaultBuilder() {
        return new OracleCreateOrAlterStBuilder(
				new OracleCaseSensitivityBuilder(TestHelpers
						.createDummyMessages()).build(), TestHelpers
						.createDummyMessages(), new ExtraAttributesPolicySetupBuilder(TestHelpers.createDummyMessages()).build());
    }
    
    private OracleCreateOrAlterStBuilder createIgnorePasswordBuilder() {
        return new OracleCreateOrAlterStBuilder(
				new OracleCaseSensitivityBuilder(TestHelpers
						.createDummyMessages()).build(), TestHelpers
						.createDummyMessages(),
				new ExtraAttributesPolicySetupBuilder(TestHelpers
						.createDummyMessages()).definePolicy(OracleUserAttribute.PASSWORD, CreateOp.class, ExtraAttributesPolicy.IGNORE).build());
    }
    
    
    /** Test create external */
    @Test
    public void testCreateExternallUserSt() {
        OracleUserAttributes.Builder external = new OracleUserAttributes.Builder();
        external.setUserName("user1");
        external.setAuth(OracleAuthentication.EXTERNAL);
        assertEquals("create user \"user1\" identified externally", createDefaultBuilder().buildCreateUserSt(external.build()).toString());
        
        external.setPassword(new GuardedString("myPassword".toCharArray()));
        try{
        	createDefaultBuilder().buildCreateUserSt(external.build());
        	fail("Build for external authentication must fail when password is sent");
        }catch(Exception e){}
        //When ignoring password, we should not fail
        createIgnorePasswordBuilder().buildCreateUserSt(external.build());
    }
    
    /** Test create global */
    @Test
    public void testCreateGlobalUserSt() {
        OracleUserAttributes.Builder global = new OracleUserAttributes.Builder();
        global.setUserName("user1");
        global.setAuth(OracleAuthentication.GLOBAL);
        try{
            createDefaultBuilder().buildCreateUserSt(global.build());
            fail("Must fail for missing global name");
        }catch(Exception e){}
        global.setGlobalName("global");
        assertEquals("create user \"user1\" identified globally as 'global'", createDefaultBuilder().buildCreateUserSt(global.build()).toString());
        
        global.setPassword(new GuardedString("myPassword".toCharArray()));
        try{
        	createDefaultBuilder().buildCreateUserSt(global.build());
        	fail("Build for global authentication must fail when password is sent");
        }catch(Exception e){}
        //When ignoring password, we should not fail
        createIgnorePasswordBuilder().buildCreateUserSt(global.build());
        
    }
    
    /** Test expire password  */
    @Test
    public void testCreateExpirePassword() {
        OracleUserAttributes.Builder attributes = new OracleUserAttributes.Builder();
        attributes.setAuth(OracleAuthentication.LOCAL);
        attributes.setUserName("user1");
        attributes.setExpirePassword(true);
        assertEquals("create user \"user1\" identified by \"user1\" password expire", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setExpirePassword(false);
        assertEquals("create user \"user1\" identified by \"user1\"", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setExpirePassword(null);
        assertEquals("create user \"user1\" identified by \"user1\"", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
    }
    
    /** Test  lock/unlock */
    @Test
    public void testCreateLock() {
        OracleUserAttributes.Builder attributes = new OracleUserAttributes.Builder();
        attributes.setAuth(OracleAuthentication.LOCAL);
        attributes.setUserName("user1");
        attributes.setEnable(true);
        assertEquals("create user \"user1\" identified by \"user1\" account unlock", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
        attributes.setEnable(false);
        assertEquals("create user \"user1\" identified by \"user1\" account lock", createDefaultBuilder().buildCreateUserSt(attributes.build()).toString());
    }

    
    /** Test alter user */
    @Test
    public void testAlterUser() {
        OracleUserAttributes.Builder attributes = new OracleUserAttributes.Builder();
        attributes.setAuth(null);
        attributes.setUserName("user1");
        attributes.setExpirePassword(true);
        attributes.setEnable(true);
        attributes.setDefaultTSQuota("-1");
        UserRecord record = new UserRecord.Builder().setDefaultTableSpace("users").build();
        assertEquals("alter user \"user1\" quota unlimited on \"users\" password expire account unlock", createDefaultBuilder().buildAlterUserSt(attributes.build(), record).toString());
        
        attributes.setEnable(null);
        attributes.setExpirePassword(null);
        attributes.setTempTableSpace("tempTS");
        record = new UserRecord.Builder().setDefaultTableSpace("defTS").build();
        assertEquals("alter user \"user1\" temporary tablespace \"tempTS\" quota unlimited on \"defTS\"", createDefaultBuilder().buildAlterUserSt(attributes.build(), record).toString());
        
        attributes = new OracleUserAttributes.Builder();
        attributes.setUserName("user1");
        attributes.setExpirePassword(true);
        assertEquals("alter user \"user1\" password expire", createDefaultBuilder().buildAlterUserSt(attributes.build(), record).toString());
        
        
        attributes = new OracleUserAttributes.Builder();
        attributes.setUserName("user1");
       	Assert.assertNull(createDefaultBuilder().buildAlterUserSt(attributes.build(), record));
       	
       	//try to unexpire password. That means just set any new password
        attributes = new OracleUserAttributes.Builder();
        attributes.setUserName("user1");
        attributes.setExpirePassword(false);
        attributes.setPassword(new GuardedString("newPassword".toCharArray()));
        assertEquals("alter user \"user1\" identified by \"newPassword\"", createDefaultBuilder().buildAlterUserSt(attributes.build(), record).toString());
        attributes.setPassword(null);
        
        record = new UserRecord.Builder().setDefaultTableSpace("users").setStatus("EXPIRED").build();
        try{
        	createDefaultBuilder().buildAlterUserSt(attributes.build(), record);
        	fail("Must require password for unexpire");
        }catch(RuntimeException e){}
        record = new UserRecord.Builder().setDefaultTableSpace("users").setStatus("OPEN").build();
        //When already unexpired, ignore unexpire attribute
       	createDefaultBuilder().buildAlterUserSt(attributes.build(), record);
    }
    
    /** Test that builder properly formats tokens */
    @Test
    public void testCaseSensitivity(){
    	ConnectorMessages cm = TestHelpers.createDummyMessages();
		OracleCaseSensitivitySetup ocs = new OracleCaseSensitivityBuilder(cm)
				.defineFormattersAndNormalizers(
						new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(OracleUserAttribute.USER).setQuatesChar("").build(),
						new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(OracleUserAttribute.PROFILE).setQuatesChar("'").build()
						).build();
    	OracleCreateOrAlterStBuilder builder = new OracleCreateOrAlterStBuilder(
				ocs, TestHelpers.createDummyMessages(),
				new ExtraAttributesPolicySetupBuilder(TestHelpers
						.createDummyMessages()).build());
        OracleUserAttributes.Builder attributes = new OracleUserAttributes.Builder();
        attributes.setAuth(null);
        attributes.setUserName("user1");
        attributes.setProfile("profile");
        Assert.assertEquals("create user user1 identified by \"user1\" profile 'profile'", builder.buildCreateUserSt(attributes.build()));
        UserRecord record = new UserRecord.Builder().build();
        Assert.assertEquals("alter user user1 profile 'profile'", builder.buildAlterUserSt(attributes.build(), record));

    }

}
