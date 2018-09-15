package us.holsopple.jsonschema2immutable.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

import java.util.Iterator;

public class PropertiesRule implements Rule<JDefinedClass, JDefinedClass> {
    private final RuleFactory ruleFactory;

    public PropertiesRule(RuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
    }

    @Override
    public JDefinedClass apply(String nodeName, JsonNode node, JDefinedClass cls, Schema schema) {
        if (node != null) {
            for (Iterator<String> properties = node.fieldNames(); properties.hasNext(); ) {
                String property = properties.next();

                ruleFactory.getPropertyRule().apply(property, node.get(property), cls, schema);
            }

            ruleFactory.getAnnotator().propertyOrder(cls, node);
        }

        return cls;
    }
}
