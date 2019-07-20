package us.holsopple.jsonschema2immutable.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.RuleFactory;

public class EnumRule extends org.jsonschema2pojo.rules.EnumRule {
    protected EnumRule(RuleFactory ruleFactory) {
        super(ruleFactory);
    }

    @Override
    public JType apply(String nodeName, JsonNode node, JClassContainer container, Schema schema) {
        return super.apply(nodeName, node, container.getPackage(), schema);
    }
}
