package org.identityconnectors.oracle;

import java.util.*;

/** OracleCaseSensitivity is responsible for formatting oracle objects tokens (users,schema...)
 *  Maybe we do not need such granurality, and have just one settings for all objects, but using this scenario
 *  we should cover all corner cases.
 *  For user we will provide some simplification how to define this caseSensitivy setup 
 * */
interface OracleCaseSensitivitySetup {
    public String normalizeToken(OracleUserAttribute attr,String token);
    public String formatToken(OracleUserAttribute attr,String token);
    public char[] formatToken(OracleUserAttribute attr,char[] token);
    public CSTokenFormatter getAttributeFormatter(OracleUserAttribute attribute);
    public CSTokenNormalizer getAttributeNormalizer(OracleUserAttribute attribute);
}

/** Oracle user attribute */
enum OracleUserAttribute {
    USER_NAME,
    PASSWORD,
    SCHEMA,
    ROLE,
    PRIVILEGE(""),
    PROFILE,
    DEF_TABLESPACE,
    TEMP_TABLESPACE,
    GLOBAL_NAME(false,"'");
    OracleUserAttribute(){}
    OracleUserAttribute(String defQuatesChar){
        this.defQuatesChar = defQuatesChar;
    }
    OracleUserAttribute(boolean defToUpper,String defQuatesChar){
        this.defToUpper = true;
        this.defQuatesChar = defQuatesChar;
    }
    private boolean defToUpper = true;
    private String defQuatesChar = "\"";
    boolean isDefToUpper() {
        return defToUpper;
    }
    String getDefQuatesChar() {
        return defQuatesChar;
    }
}

/** Formatter that formats given token. 
 *  It can make it upper case, append quates etc.
 *
 */
final class CSTokenFormatter{
    private OracleUserAttribute attribute;
    private String quatesChar = "";
    OracleUserAttribute getAttribute() {
        return attribute;
    }
    String getQuatesChar() {
        return quatesChar;
    }
    char[] formatToken(char[] token){
        if(token == null){
            return token;
        }
        StringBuilder builder = new StringBuilder(token.length + 2);
        builder.append(quatesChar);
        builder.append(token);
        builder.append(quatesChar);
        char[] dst = new char[builder.length()];
        builder.getChars(0, builder.length() , dst, 0);
        builder.delete(0, builder.length());
        return dst;
    }
    String formatToken(String token){
        if(token == null){
            return token;
        }
        return new String(formatToken(token.toCharArray()));
    }
    private CSTokenFormatter(){}
    
    final static class Builder{
        CSTokenFormatter element = new CSTokenFormatter();
        Builder setAttribute(OracleUserAttribute attribute){
            element.attribute = attribute;
            return this;
        }
        Builder setQuatesChar(String quatesChar){
            element.quatesChar = quatesChar;
            return this;
        }
        Builder setValues(Map<String, Object> aMap){
            Map<String, Object> map = new HashMap<String, Object>(aMap);
            String quates = (String) map.remove("quates");
            if(quates != null){
                element.quatesChar = quates;
            }
            if(!map.isEmpty()){
                throw new RuntimeException("Some elements in aMap not recognized : " + map);
            }
            return this;
        }
        Builder setDefaultValues(){
            //We can have different defaults for each attribute
            if(element.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            element.quatesChar = element.attribute.getDefQuatesChar();
            return this;
        }
        CSTokenFormatter build(){
            if(element.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            return element;
        }
        CSTokenFormatter buildWithDefaultValues(OracleUserAttribute attribute){
            setAttribute(attribute);
            setDefaultValues();
            return build();
        }
    }
}

final class CSTokenNormalizer{
    private OracleUserAttribute attribute;
    private boolean toUpper;
    OracleUserAttribute getAttribute() {
        return attribute;
    }
    boolean isToUpper() {
        return toUpper;
    }
    String normalizeToken(String token){
        if(token == null || token.length() == 0){
            return token;
        }
        return toUpper ? token.toUpperCase() : token;
    }
    
    final static class Builder{
        CSTokenNormalizer element = new CSTokenNormalizer();
        Builder setAttribute(OracleUserAttribute attribute){
            element.attribute = attribute;
            return this;
        }
        Builder setToUpper(boolean toUpper){
            element.toUpper = toUpper;
            return this;
        }
        Builder setValues(Map<String, Object> aMap){
            Map<String, Object> map = new HashMap<String, Object>(aMap);
            String toUpper = (String) map.remove("upper");
            if(toUpper != null){
                element.toUpper = Boolean.valueOf(toUpper);
            }
            if(!map.isEmpty()){
                throw new RuntimeException("Some elements in aMap not recognized : " + map);
            }
            return this;
        }
        Builder setDefaultValues(){
            //We can have different defaults for each attribute
            if(element.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            element.toUpper = element.attribute.isDefToUpper();
            return this;
        }
        CSTokenNormalizer build(){
            if(element.attribute == null){
                throw new IllegalStateException("Attribute not set");
            }
            return element;
        }
        CSTokenNormalizer buildWithDefaultValues(OracleUserAttribute attribute){
            setAttribute(attribute);
            setDefaultValues();
            return build();
        }
    }
}


/** Builder of OracleCaseSensitivity using formatters for user attributes */
final class OracleCaseSensitivityBuilder{
    private Map<OracleUserAttribute,CSTokenFormatter> formatters = new HashMap<OracleUserAttribute, CSTokenFormatter>(6);
    private Map<OracleUserAttribute,CSTokenNormalizer> normalizers = new HashMap<OracleUserAttribute, CSTokenNormalizer>(6);
    
    OracleCaseSensitivityBuilder defineFormatters(CSTokenFormatter... formatters){
        for(CSTokenFormatter element : formatters){
            this.formatters.put(element.getAttribute(),element);
        }
        return this;
    }
    
    
    OracleCaseSensitivityBuilder defineNormalizers(CSTokenNormalizer... normalizers){
        for(CSTokenNormalizer element : normalizers){
            this.normalizers.put(element.getAttribute(),element);
        }
        return this;
    }
    
    
    @SuppressWarnings("unchecked")
    OracleCaseSensitivityBuilder parseMap(String format){
        //If we set string default, all formatters will have its default value
        if("default".equalsIgnoreCase(format)){
            return this;
        }
        final Map<String, Object> map = MapParser.parseMap(format);
        final Map<String, Object> formatters = (Map<String, Object>) map.remove("formatters");
        final Map<String, Object> normalizers = (Map<String, Object>) map.remove("normalizers");
        if(!map.isEmpty()){
            throw new IllegalArgumentException("Unrecognized element in format map " + map.keySet());
        }
        if(formatters != null){
            for(String attributeName : formatters.keySet()){
                Map<String, Object> elementMap = (Map<String, Object>) formatters.get(attributeName);
                if("ALL".equalsIgnoreCase(attributeName)){
                    for(OracleUserAttribute attribute : OracleUserAttribute.values()){
                        this.formatters.put(attribute, new CSTokenFormatter.Builder().setAttribute(attribute).setValues(elementMap).build());
                    }
                    continue;
                }
                OracleUserAttribute attribute = OracleUserAttribute.valueOf(attributeName);
                CSTokenFormatter element = new CSTokenFormatter.Builder().setAttribute(attribute).setValues(elementMap).build(); 
                this.formatters.put(element.getAttribute(),element);    
            }
        }
        if(normalizers != null){
            for(String attributeName : normalizers.keySet()){
                Map<String, Object> elementMap = (Map<String, Object>) normalizers.get(attributeName);
                if("ALL".equalsIgnoreCase(attributeName)){
                    for(OracleUserAttribute attribute : OracleUserAttribute.values()){
                        this.normalizers.put(attribute, new CSTokenNormalizer.Builder().setAttribute(attribute).setValues(elementMap).build());
                    }
                    continue;
                }
                OracleUserAttribute attribute = OracleUserAttribute.valueOf(attributeName);
                CSTokenNormalizer element = new CSTokenNormalizer.Builder().setAttribute(attribute).setValues(elementMap).build(); 
                this.normalizers.put(element.getAttribute(),element);    
            }
        }
        return this;
    }
    
    OracleCaseSensitivitySetup build(){
        Map<OracleUserAttribute,CSTokenFormatter> formatters = new HashMap<OracleUserAttribute, CSTokenFormatter>(this.formatters);
        Map<OracleUserAttribute,CSTokenNormalizer> normalizers = new HashMap<OracleUserAttribute, CSTokenNormalizer>(this.normalizers);
        
        //If any elements is not defined in specified map set default value
        for(OracleUserAttribute attribute : OracleUserAttribute.values()){
            CSTokenFormatter formatter = formatters.get(attribute);
            if(formatter == null){
                formatter = new CSTokenFormatter.Builder().buildWithDefaultValues(attribute);
                formatters.put(attribute,formatter);
            }
            CSTokenNormalizer normalizer = normalizers.get(attribute);
            if(normalizer == null){
                normalizer = new CSTokenNormalizer.Builder().buildWithDefaultValues(attribute);
                normalizers.put(attribute,normalizer);
            }
        }
        return new OracleCaseSensitivityImpl(formatters,normalizers);
    }
    
}

final class OracleCaseSensitivityImpl implements OracleCaseSensitivitySetup{
    private Map<OracleUserAttribute,CSTokenFormatter> formatters;
    private Map<OracleUserAttribute,CSTokenNormalizer> normalizers;    
    
    OracleCaseSensitivityImpl(Map<OracleUserAttribute,CSTokenFormatter> formatters,Map<OracleUserAttribute,CSTokenNormalizer> normalizers){
        this.formatters = new HashMap<OracleUserAttribute, CSTokenFormatter>(formatters);
        this.normalizers = new HashMap<OracleUserAttribute, CSTokenNormalizer>(normalizers);
    }
    public CSTokenFormatter getAttributeFormatter(OracleUserAttribute attribute){
        final CSTokenFormatter formatter = formatters.get(attribute);
        if(formatter == null){
            throw new IllegalArgumentException("No formatter defined for attribute " + attribute);
        }
        return formatter;
    }
    
    public CSTokenNormalizer getAttributeNormalizer(OracleUserAttribute attribute){
        final CSTokenNormalizer normalizer = normalizers.get(attribute);
        if(normalizer == null){
            throw new IllegalArgumentException("No normalizer defined for attribute " + attribute);
        }
        return normalizer;
    }
    
    public String formatToken(OracleUserAttribute attr, String token) {
        return getAttributeFormatter(attr).formatToken(token);
    }
    public String normalizeToken(OracleUserAttribute attr, String token) {
        return getAttributeNormalizer(attr).normalizeToken(token);
    }
    public char[] formatToken(OracleUserAttribute attr, char[] token) {
        return getAttributeFormatter(attr).formatToken(token);
    }
    
}



 
