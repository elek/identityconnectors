/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.operations.SPIOperation;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.junit.Test;


/**
 * Tests for OracleAttributeNormalizer
 * @author kitko
 *
 */
public class OracleInputNormalizerTest {

	@Test
	public void testNormalizeAttribute(){
		OracleInputNormalizer normalizer = new OracleInputNormalizer(OracleConfigurationTest.createSystemConfiguration().getCSSetup());
		ObjectClass objectClass = ObjectClass.ACCOUNT;
		
		Class<? extends SPIOperation> op = UpdateOp.class;
		
		assertNull(normalizer.normalizeAttribute(objectClass, op, null));
		
		Attribute attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build("dummy","dummyValue"));
		assertNotNull(attr);
		assertEquals("dummyValue", AttributeUtil.getSingleValue(attr));
		
		//User is by default case sensitive/insensitive, depends on OracleUserAttributeCS
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Name.NAME,"myName"));
		assertNotNull(attr);
		assertEquals(OracleUserAttribute.USER.getFormatting().isToUpper() ?  "myName".toUpperCase() : "myName", AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Name.NAME.toLowerCase(),"myName"));
		assertNotNull(attr);
		assertEquals(OracleUserAttribute.USER.getFormatting().isToUpper() ?  "myName".toUpperCase() : "myName", AttributeUtil.getSingleValue(attr));
		
		//We do not normalize by UID
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Uid.NAME,"myUid"));
		assertNotNull(attr);
		assertEquals("myUid", AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(Uid.NAME.toLowerCase(),"myUid"));
		assertNotNull(attr);
		assertEquals("myUid", AttributeUtil.getSingleValue(attr));
		
		// By default we do not uppercase globalname
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_GLOBAL_ATTR_NAME,"myGlobalName"));
		assertNotNull(attr);
		assertEquals("myGlobalName", AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME,"myProfile"));
		assertNotNull(attr);
		assertEquals("myProfile".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PROFILE_ATTR_NAME.toLowerCase(),"myProfile"));
		assertNotNull(attr);
		assertEquals("myProfile".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass,  op, AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME,"myDefTs"));
		assertNotNull(attr);
		assertEquals("myDefTs".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_DEF_TS_ATTR_NAME.toLowerCase(),"myDefTs"));
		assertNotNull(attr);
		assertEquals("myDefTs".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME,"myTempTs"));
		assertNotNull(attr);
		assertEquals("myTempTs".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_TEMP_TS_ATTR_NAME.toLowerCase(),"myTempTs"));
		assertNotNull(attr);
		assertEquals("myTempTs".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME,"myPriv"));
		assertNotNull(attr);
		assertEquals("myPriv".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_PRIVS_ATTR_NAME.toLowerCase(),"myPriv"));
		assertNotNull(attr);
		assertEquals("myPriv".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME,"myRole"));
		assertNotNull(attr);
		assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME.toLowerCase(),"myRole"));
		assertNotNull(attr);
		assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));

		attr = normalizer.normalizeAttribute(objectClass, op, AttributeBuilder.build(OracleConstants.ORACLE_ROLES_ATTR_NAME.toUpperCase(),"myRole"));
		assertNotNull(attr);
		assertEquals("myRole".toUpperCase(), AttributeUtil.getSingleValue(attr));
		
	}
}
