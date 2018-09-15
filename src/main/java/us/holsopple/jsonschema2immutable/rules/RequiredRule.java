package us.holsopple.jsonschema2immutable.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDocCommentable;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.Rule;

public class RequiredRule implements Rule<JDocCommentable, JDocCommentable> {
    @Override
    public JDocCommentable apply(String nodeName, JsonNode node, JDocCommentable field, Schema currentSchema) {
        return field;
    }
}
