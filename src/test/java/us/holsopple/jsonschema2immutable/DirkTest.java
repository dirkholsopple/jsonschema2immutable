package us.holsopple.jsonschema2immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.codemodel.*;
import org.jsonschema2pojo.*;
import org.junit.jupiter.api.Test;
import us.holsopple.jsonschema2immutable.rules.ImmutableRuleFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirkTest {
    @Test
    public void testSchemaGeneration() throws Exception {
        URL source = DirkTest.class.getResource("/schema/dirk-schema.json");

        JCodeModel codeModel = new JCodeModel();

        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() { // set config option by overriding method
                return true;
            }
        };

        SchemaMapper mapper = new SchemaMapper(
                new ImmutableRuleFactory(config, new Jackson2Annotator(config), new SchemaStore()),
                new SchemaGenerator());
        mapper.generate(codeModel, "ClassName", "com.example", source);

        //addInterfaces(codeModel);

        File output = new File("target/generated-sources");
        System.out.println("writing output to " + output.getCanonicalPath());
        codeModel.build(output);
    }

    private void addInterfaces(JCodeModel codeModel) {
        Iterator<JPackage> packages = codeModel.packages();
        while (packages.hasNext()) {
            JPackage pkg = packages.next();
            JPackage interfacePkg = codeModel._package(pkg.name() + "2");
            addInterfaces(pkg, interfacePkg);
        }
    }

    private void addInterfaces(JPackage pkg, JPackage interfacePkg) {
        Iterator<JDefinedClass> classes = pkg.classes();
        while (classes.hasNext()) {
            addInterfaces(classes.next(), interfacePkg);
        }
    }

    private void addInterfaces(JDefinedClass cls, JPackage interfacePkg) {
        Iterator<JDefinedClass> innerClasses = cls.classes();
        while (innerClasses.hasNext()) {
            addInterfaces(innerClasses.next(), interfacePkg);
        }
        if (cls.getClassType() == ClassType.ENUM) {
            return;
        }
        JDefinedClass iface;
        try {
            iface = interfacePkg._interface(cls.name());
        } catch (JClassAlreadyExistsException ex) {
            throw new RuntimeException(ex);
        }
        iface.javadoc().add(cls.javadoc());
        for (JMethod method : cls.methods()) {
            JMods mods = method.mods();
            if ((mods.getValue() & JMod.PUBLIC) > 0 && method.name().startsWith("get") &&
                    !method.name().equals("getAdditionalProperties")) {
                Collection<JAnnotationUse> annotations = method.annotations();
                JAnnotationUse jsonProperty = annotations.stream()
                        .filter(a -> a.getAnnotationClass().isAssignableFrom(cls.owner().ref(JsonProperty.class)))
                        .findFirst().orElse(null);
                if (jsonProperty != null) {
                    JType type = method.type();
                    JMethod ifaceMethod = iface.method(JMod.PUBLIC, type, method.name());
                    ifaceMethod.javadoc().add(method.javadoc());
                    if (needsJsonPropertyAnnotation(method.name(), jsonPropertyName(jsonProperty))) {
                        copyParams(jsonProperty, ifaceMethod.annotate(JsonProperty.class));
                    }
                    if (isNullable(method)) {
                        ifaceMethod.annotate(Nullable.class);
                    } else {
                        ifaceMethod.type(type.unboxify());
                    }
                }
            }
        }
        cls._implements(iface);
    }

    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("^\"(.*)\"$");

    private void copyParams(JAnnotationUse source, JAnnotationUse destination) {
        for (Map.Entry<String, JAnnotationValue> param : source.getAnnotationMembers().entrySet()) {
            String name = param.getKey();
            List<String> values = annotationStringValues(param.getValue());
            if (values.size() > 1) {
                JAnnotationArrayMember array = destination.paramArray(name);
                for (String value : values) {
                    array.param(value);
                }
            } else {
                destination.param(name, values.get(0));
            }
        }
    }

    private boolean needsJsonPropertyAnnotation(String methodName, String jsonPropertyName) {
        return !(methodName.equals(jsonPropertyName) ||
                ("get" + jsonPropertyName.substring(0, 1).toUpperCase() + jsonPropertyName.substring(1))
                        .equals(methodName));
    }

    private String jsonPropertyName(JAnnotationUse jsonPropertyAnnotation) {
        List<String> values = annotationStringValues(jsonPropertyAnnotation.getAnnotationMembers().get("value"));
        if (values.size() != 1) {
            throw new RuntimeException("JsonProperty annotation without exactly one value: " +
                    String.join(", ", values));
        }
        return values.get(0);
    }

    private List<String> annotationStringValues(JAnnotationValue value) {
        StringWriter writer = new StringWriter();
        JFormatter formatter = new JFormatter(writer);
        value.generate(formatter);
        String str = writer.toString();
        String[] values = str.split(",");

        return Stream.of(values).map(v -> {
            Matcher matcher = STRING_LITERAL_PATTERN.matcher(v);
            if (!matcher.matches()) {
                throw new RuntimeException("couldn't parse value as a string literal: " + v);
            }
            return matcher.group(1);
        }).collect(Collectors.toList());
    }

    private boolean isNullable(JMethod method) {
        StringWriter writer = new StringWriter();
        JFormatter formatter = new JFormatter(writer);
        method.javadoc().generate(formatter);
        String javadocText = writer.toString();

        return !javadocText.contains("(Required)");
    }
}
