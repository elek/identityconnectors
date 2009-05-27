/**
 * 
 */
package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.identityconnectors.framework.common.objects.ConnectorMessages;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.*;

/**
 * Tests for creation of OracleCaseSensitivity using builder
 * @author kitko
 *
 */
public class OracleCaseSensitivitySetupTest {
    
    /** Tests manual sensitivity with formatters */
    @Test
    public void testCreateExplicitFormatters(){
    	ConnectorMessages cm = TestHelpers.createDummyMessages();
        OracleCaseSensitivitySetup cs = createBuilder().defineFormattersAndNormalizers(new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(OracleUserAttributeCS.SCHEMA).setQuatesChar("AAA").build()).build();
        Assert.assertEquals("AAA",cs.getAttributeFormatterAndNormalizer(OracleUserAttributeCS.SCHEMA).getQuatesChar());
        CSAttributeFormatterAndNormalizer formatter = new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(OracleUserAttributeCS.PROFILE).setQuatesChar("BBB").build();
        assertEquals(OracleUserAttributeCS.PROFILE, formatter.getAttribute());
        assertEquals("BBB", formatter.getQuatesChar());
    }
    
    /** Tests manual sensitivity with normalizers */
    @Test
    public void testCreateExplicitNormalizers(){
    	ConnectorMessages cm = TestHelpers.createDummyMessages();
        OracleCaseSensitivitySetup cs = createBuilder().defineFormattersAndNormalizers(new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(OracleUserAttributeCS.SCHEMA).setToUpper(true).build()).build();
        Assert.assertTrue(cs.getAttributeFormatterAndNormalizer(OracleUserAttributeCS.SCHEMA).isToUpper());
        CSAttributeFormatterAndNormalizer normalizer = new CSAttributeFormatterAndNormalizer.Builder(cm).setAttribute(OracleUserAttributeCS.DEF_TABLESPACE).setToUpper(false).build();
        assertEquals(OracleUserAttributeCS.DEF_TABLESPACE, normalizer.getAttribute());
        assertEquals(false, normalizer.isToUpper());
        
    }
    
    
    /** Test create all formatters */
    @Test
    public void testCreateAllFormatters(){
        OracleCaseSensitivitySetup cs = createBuilder().defineFormattersAndNormalizers().build();
        for(OracleUserAttributeCS oua : OracleUserAttributeCS.values()){
            Assert.assertNotNull("Formatter for attribute " + oua + " cannot be null", cs.getAttributeFormatterAndNormalizer(oua));
        }
    }
    
    /** Test create all normalizers */
    @Test
    public void testCreateAllNormalizers(){
        OracleCaseSensitivitySetup cs = createBuilder().build();
        for(OracleUserAttributeCS oua : OracleUserAttributeCS.values()){
            Assert.assertNotNull("Normalizer for attribute " + oua + " cannot be null", cs.getAttributeFormatterAndNormalizer(oua));
        }
    }
    
    /** Test create formatter and normalizers from string map */
    @Test
    public void testCreateFromFormat(){
        createBuilder().parseMap("default").build();
        try{
            createBuilder().parseMap("unknown").build();
            fail("Must fail for unknown");
        }
        catch(RuntimeException e){}
        final OracleCaseSensitivitySetup cs = createBuilder().parseMap("ALL={upper=false},USER={quates=AAA},ROLE={upper=true}").build();
        assertEquals("\"",cs.getAttributeFormatterAndNormalizer((OracleUserAttributeCS.DEF_TABLESPACE)).getQuatesChar());
        assertEquals("AAA",cs.getAttributeFormatterAndNormalizer((OracleUserAttributeCS.USER)).getQuatesChar());
        assertEquals(true,cs.getAttributeFormatterAndNormalizer(OracleUserAttributeCS.USER).isToUpper());
        assertEquals(true,cs.getAttributeFormatterAndNormalizer(OracleUserAttributeCS.ROLE).isToUpper());
        assertEquals("\"",cs.getAttributeFormatterAndNormalizer((OracleUserAttributeCS.ROLE)).getQuatesChar());
        
    }
    
    private OracleCaseSensitivityBuilder createBuilder(){
    	return new OracleCaseSensitivityBuilder(TestHelpers.createDummyMessages());
    }
    
    
}
