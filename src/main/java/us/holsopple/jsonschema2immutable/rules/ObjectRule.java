package us.holsopple.jsonschema2immutable.rules;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.*;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.immutables.value.Value;
import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.exception.ClassAlreadyExistsException;
import org.jsonschema2pojo.exception.GenerationException;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;
import org.jsonschema2pojo.util.MakeUniqueClassName;

import java.lang.reflect.Modifier;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.jsonschema2pojo.rules.PrimitiveTypes.isPrimitive;
import static org.jsonschema2pojo.rules.PrimitiveTypes.primitiveType;
import static org.jsonschema2pojo.util.TypeUtil.resolveType;

public class ObjectRule implements Rule<JPackage, JType> {

    private final RuleFactory ruleFactory;

    ObjectRule(RuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
    }

    /**
     * Applies this schema rule to take the required code generation steps.
     * <p>
     * When this rule is applied for schemas of type object, the properties of
     * the schema are used to generate a new Java class and determine its
     * characteristics. See other implementers of {@link Rule} for details.
     */
    @Override
    public JType apply(String nodeName, JsonNode node, JPackage _package, Schema schema) {

        JType superType = getSuperType(nodeName, node, _package, schema);

        if (superType != null) {
            if (superType.isPrimitive() || isFinal(superType)) {
                return superType;
            }

            if (superType.boxify() != null && !superType.boxify().isInterface()) {
                throw new RuntimeException("cannot extend a class, only an interface");
            }
        }

        JDefinedClass jclass;
        try {
            jclass = createClass(nodeName, node, _package);
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }

        if (superType != null) {
            jclass._implements((JClass) superType);
        }

        schema.setJavaTypeIfEmpty(jclass);

        if (node.has("deserializationClassProperty")) {
            addJsonTypeInfoAnnotation(jclass, node);
        }

        if (node.has("title")) {
            ruleFactory.getTitleRule().apply(nodeName, node.get("title"), jclass, schema);
        }

        if (node.has("description")) {
            ruleFactory.getDescriptionRule().apply(nodeName, node.get("description"), jclass, schema);
        }

        ruleFactory.getPropertiesRule().apply(nodeName, node.get("properties"), jclass, schema);

        if (node.has("javaInterfaces")) {
            addInterfaces(jclass, node.get("javaInterfaces"));
        }

        ruleFactory.getDynamicPropertiesRule().apply(nodeName, node.get("properties"), jclass, schema);

        if (node.has("required")) {
            ruleFactory.getRequiredArrayRule().apply(nodeName, node.get("required"), jclass, schema);
        }

        return jclass;

    }

    /**
     * Creates a new Java class that will be generated.
     *
     * @param nodeName
     *            the node name which may be used to dictate the new class name
     * @param node
     *            the node representing the schema that caused the need for a
     *            new class. This node may include a 'javaType' property which
     *            if present will override the fully qualified name of the newly
     *            generated class.
     * @param _package
     *            the package which may contain a new class after this method
     *            call
     * @return a reference to a newly created class
     * @throws ClassAlreadyExistsException
     *             if the given arguments cause an attempt to create a class
     *             that already exists, either on the classpath or in the
     *             current map of classes to be generated.
     */
    private JDefinedClass createClass(String nodeName, JsonNode node, JPackage _package) throws ClassAlreadyExistsException {

        JDefinedClass newType;

        try {
            if (node.has("existingJavaType")) {
                String fqn = substringBefore(node.get("existingJavaType").asText(), "<");

                if (isPrimitive(fqn, _package.owner())) {
                    throw new ClassAlreadyExistsException(primitiveType(fqn, _package.owner()));
                }

                JClass existingClass = resolveType(_package, fqn + (node.get("existingJavaType").asText().contains("<") ? "<" + substringAfter(node.get("existingJavaType").asText(), "<") : ""));
                throw new ClassAlreadyExistsException(existingClass);
            }

            if (node.has("javaType")) {
                String fqn = node.path("javaType").asText();

                if (isPrimitive(fqn, _package.owner())) {
                    throw new GenerationException("javaType cannot refer to a primitive type (" + fqn + "), did you mean to use existingJavaType?");
                }

                if (fqn.contains("<")) {
                    throw new GenerationException("javaType does not support generic args (" + fqn + "), did you mean to use existingJavaType?");
                }

                int index = fqn.lastIndexOf(".") + 1;
                if (index >= 0 && index < fqn.length()) {
                    fqn = fqn.substring(0, index) + ruleFactory.getGenerationConfig().getClassNamePrefix() + fqn.substring(index) + ruleFactory.getGenerationConfig().getClassNameSuffix();
                }

                newType = _package.owner()._class(JMod.PUBLIC | JMod.ABSTRACT, fqn, ClassType.CLASS);
            } else {
                newType = _package._class(JMod.PUBLIC | JMod.ABSTRACT, getClassName(nodeName, node, _package), ClassType.CLASS);
            }
        } catch (JClassAlreadyExistsException e) {
            throw new ClassAlreadyExistsException(e.getExistingClass());
        }

        ruleFactory.getAnnotator().propertyInclusion(newType, node);

        addImmutableAnnotations(newType);
        return newType;

    }

    private void addImmutableAnnotations(JDefinedClass iface) {
        iface.annotate(Value.Immutable.class);
        iface.annotate(Value.Modifiable.class);
        iface.annotate(JsonDeserialize.class).param("as",
                iface.owner().ref(iface._package().name() + ".Immutable" + iface.name()));
    }

    private boolean isFinal(JType superType) {
        try {
            Class<?> javaClass = Class.forName(superType.fullName());
            return Modifier.isFinal(javaClass.getModifiers());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private JType getSuperType(String nodeName, JsonNode node, JPackage jPackage, Schema schema) {
        if (node.has("extendsJavaClass")) {
            throw new IllegalStateException("'extendsJavaClass' not supported (immutables can't extend other classes)");
        }

        Schema superTypeSchema = getSuperSchema(node, schema);
        if (superTypeSchema == null) {
            return null;
        }
        return ruleFactory.getSchemaRule()
                    .apply(nodeName + "Parent", node.get("extends"), jPackage, superTypeSchema);
    }

    private Schema getSuperSchema(JsonNode node, Schema schema) {
        if (!node.has("extends")) {
            return null;
        }
        String path = schema.getId().getFragment() == null ?
                "#extends" :
                "#" + schema.getId().getFragment() + "/extends";

        return ruleFactory.getSchemaStore()
                .create(schema, path, ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters());
    }

    private void addJsonTypeInfoAnnotation(JDefinedClass jclass, JsonNode node) {
        if (ruleFactory.getGenerationConfig().getAnnotationStyle() == AnnotationStyle.JACKSON2) {
            String annotationName = node.get("deserializationClassProperty").asText();
            JAnnotationUse jsonTypeInfo = jclass.annotate(JsonTypeInfo.class);
            jsonTypeInfo.param("use", JsonTypeInfo.Id.CLASS);
            jsonTypeInfo.param("include", JsonTypeInfo.As.PROPERTY);
            jsonTypeInfo.param("property", annotationName);
        }
    }

    private static JDefinedClass definedClassOrNullFromType(JType type)
    {
        if (type == null || type.isPrimitive())
        {
            return null;
        }
        JClass fieldClass = type.boxify();
        JPackage jPackage = fieldClass._package();
        return jPackage._getClass(fieldClass.name());
    }

    /**
     * This is recursive with searchClassAndSuperClassesForField
     */
    private JFieldVar searchSuperClassesForField(String property, JDefinedClass jclass) {
        JClass superClass = jclass._extends();
        JDefinedClass definedSuperClass = definedClassOrNullFromType(superClass);
        if (definedSuperClass == null) {
            return null;
        }
        return searchClassAndSuperClassesForField(property, definedSuperClass);
    }

    private JFieldVar searchClassAndSuperClassesForField(String property, JDefinedClass jclass) {
        Map<String, JFieldVar> fields = jclass.fields();
        JFieldVar field = fields.get(property);
        if (field == null) {
            return searchSuperClassesForField(property, jclass);
        }
        return field;
    }

    private void addInterfaces(JDefinedClass jclass, JsonNode javaInterfaces) {
        for (JsonNode i : javaInterfaces) {
            jclass._implements(resolveType(jclass._package(), i.asText()));
        }
    }

    private String getClassName(String nodeName, JsonNode node, JPackage _package) {
        String prefix = ruleFactory.getGenerationConfig().getClassNamePrefix();
        String suffix = ruleFactory.getGenerationConfig().getClassNameSuffix();
        String fieldName = ruleFactory.getNameHelper().getFieldName(nodeName, node);
        String capitalizedFieldName = StringUtils.capitalize(fieldName);
        String fullFieldName = createFullFieldName(capitalizedFieldName, prefix, suffix);

        String className = ruleFactory.getNameHelper().replaceIllegalCharacters(fullFieldName);
        String normalizedName = ruleFactory.getNameHelper().normalizeName(className);
        return makeUnique(normalizedName, _package);
    }

    private String createFullFieldName(String nodeName, String prefix, String suffix) {
        String returnString = nodeName;
        if (prefix != null) {
            returnString = prefix + returnString;
        }

        if (suffix != null) {
            returnString = returnString + suffix;
        }

        return returnString;
    }

    private String makeUnique(String className, JPackage _package) {
        try {
            JDefinedClass _class = _package._class(className);
            _package.remove(_class);
            return className;
        } catch (JClassAlreadyExistsException e) {
            return makeUnique(MakeUniqueClassName.makeUnique(className), _package);
        }
    }

    private boolean usesPolymorphicDeserialization(JsonNode node) {
        if (ruleFactory.getGenerationConfig().getAnnotationStyle() == AnnotationStyle.JACKSON2) {
            return node.has("deserializationClassProperty");
        }
        return false;
    }

}
