package us.holsopple.jsonschema2immutable.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.*;
import org.immutables.value.Value;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

import javax.annotation.Nullable;

public class PropertyRule implements Rule<JDefinedClass, JDefinedClass> {
    private final RuleFactory ruleFactory;

    public PropertyRule(RuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
    }

    @Override
    public JDefinedClass apply(String nodeName, JsonNode originalNode, JDefinedClass cls, Schema schema) {
        JType propertyType = ruleFactory.getSchemaRule().apply(nodeName, originalNode, cls, schema);
        JsonNode node = resolveRefs(originalNode, schema);
        boolean isRequired = isRequired(nodeName, node, originalNode, schema);

        if (!isRequired && propertyType.isPrimitive()) {
            propertyType = propertyType.boxify();
        }

        JExpression defaultExpression = getDefaultExpr(originalNode, node, cls, propertyType);
        JMethod getter = addGetter(cls, propertyType, nodeName, originalNode, node, isRequired, defaultExpression);
        ruleFactory.getAnnotator().propertyGetter(getter, cls, nodeName);
        propertyAnnotations(nodeName, node, originalNode, schema, getter);

        if (defaultExpression != null) {
            getter.annotate(Value.Default.class);
            getter.body()._return(defaultExpression);
        }

        /*
        ruleFactory.getMinimumMaximumRule().apply(nodeName, node, null, schema);

        ruleFactory.getMinItemsMaxItemsRule().apply(nodeName, node, null, schema);

        ruleFactory.getMinLengthMaxLengthRule().apply(nodeName, node, null, schema);

        if (isObject(node) || isArray(node)) {
            ruleFactory.getValidRule().apply(nodeName, node, null, schema);
        }
        */

        return cls;
    }

    private JsonNode getOriginalOrRefProperty(JsonNode originalNode, JsonNode node, String propertyName) {
        if (originalNode.has(propertyName)) {
            return originalNode.get(propertyName);
        } else if (node.has(propertyName)) {
            return node.get(propertyName);
        }
        return null;
    }

    private void propertyAnnotations(String nodeName, JsonNode node, JsonNode originalNode, Schema schema, JMethod getter) {
        JsonNode title = getOriginalOrRefProperty(originalNode, node, "title");
        if (title != null) {
            ruleFactory.getTitleRule().apply(nodeName, title, getter, schema);
        }

        JsonNode javaName = getOriginalOrRefProperty(originalNode, node, "javaName");
        if (javaName != null) {
            ruleFactory.getJavaNameRule().apply(nodeName, javaName, getter, schema);
        }

        JsonNode description = getOriginalOrRefProperty(originalNode, node, "description");
        if (description != null) {
            ruleFactory.getDescriptionRule().apply(nodeName, description, getter, schema);
        }
    }

    private boolean isRequired(String nodeName, JsonNode node, JsonNode originalNode, Schema schema) {
        JsonNode requiredNode = getOriginalOrRefProperty(originalNode, node, "required");
        if (requiredNode != null) {
            return requiredNode.asBoolean();
        }

        JsonNode requiredArray = schema.getContent().get("required");

        if (requiredArray != null) {
            for (JsonNode required : requiredArray) {
                if (nodeName.equals(required.asText()))
                    return true;
            }
        }

        return false;
    }

    private JMethod addGetter(JDefinedClass c, JType type, String jsonPropertyName, JsonNode originalNode,
                              JsonNode node, boolean isRequired, JExpression defaultValue) {
        int mods = defaultValue == null ? (JMod.PUBLIC | JMod.ABSTRACT) : JMod.PUBLIC;
        JMethod getter = c.method(mods, type, getGetterName(jsonPropertyName, type, node));
        if (!isRequired && defaultValue == null && !"array".equals(node.get("type").asText())) {
            getter.annotate(Nullable.class);
        }
        return getter;
    }

    private String getGetterName(String propertyName, JType type, JsonNode node) {
        return ruleFactory.getNameHelper().getGetterName(propertyName, type, node);
    }

    private JsonNode resolveRefs(JsonNode node, Schema parent) {
        if (node.has("$ref")) {
            Schema refSchema = ruleFactory.getSchemaStore()
                    .create(parent, node.get("$ref").asText(), ruleFactory.getGenerationConfig().getRefFragmentPathDelimiters());
            JsonNode refNode = refSchema.getContent();
            return resolveRefs(refNode, parent);
        } else {
            return node;
        }
    }

    private JExpression getDefaultExpr(JsonNode originalNode, JsonNode node, JClass cls, JType type) {
        JsonNode defaultNode = getOriginalOrRefProperty(originalNode, node, "default");

        JCodeModel codeModel = cls.owner();
        if (defaultNode != null) {
            if (type.unboxify() == codeModel.BOOLEAN) {
                return JExpr.lit(defaultNode.asBoolean());
            } else if (type.unboxify() == codeModel.INT) {
                return JExpr.lit(defaultNode.asInt());
            } else if (type.unboxify() == codeModel.LONG) {
                return JExpr.lit(defaultNode.asLong());
            } else if (type.unboxify() == codeModel.DOUBLE || type.unboxify() == codeModel.FLOAT) {
                return JExpr.lit(defaultNode.asDouble());
            } else {
                if (type.boxify().isAssignableFrom(codeModel.ref(String.class))) {
                    return JExpr.lit(defaultNode.asText());
                } else if (defaultNode.isIntegralNumber()) {
                    return type.boxify().staticInvoke("fromValue").arg(JExpr.lit(defaultNode.asLong()));
                } else if (defaultNode.isNumber()) {
                    return type.boxify().staticInvoke("fromValue").arg(JExpr.lit(defaultNode.asDouble()));
                } else if (type instanceof JDefinedClass && ((JDefinedClass) type).getClassType() == ClassType.ENUM) {
                    return type.boxify().staticInvoke("fromValue").arg(JExpr.lit(defaultNode.asText()));
                } else {
                    return type.boxify().staticInvoke("fromString").arg(JExpr.lit(defaultNode.asText()));
                }
            }
        }
        return null;
    }
}
