import com.sun.codemodel.JCodeModel;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jsonschema2pojo.*;
import us.holsopple.jsonschema2immutable.rules.ImmutableRuleFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class JsonSchema2ImmutableApplication {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Json Schema to Immutable").build()
                .defaultHelp(true)
                .description("Produce java immutable definitions from json schema");
        parser.addArgument("-o", "--output").setDefault("target/generated-sources");
        parser.addArgument("-p", "--package").required(true);
        parser.addArgument("sourceDir").nargs("*");
        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return;
        }
        File output = new File(ns.getString("output"));

        try {
            System.out.println("writing output to " + output.getCanonicalPath());
        } catch (IOException ex) {
            System.err.println("error opening output dir: " + ex.getLocalizedMessage());
        }

        String outputPkg = ns.getString("package");
        List<String> sourceDirs = ns.getList("sourceDir");

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

        for (String sourceDir : sourceDirs) {
            try (Stream<Path> paths = Files.walk(Paths.get(sourceDir))) {
                paths.filter(Files::isRegularFile).forEach(sourceFile -> {
                    try {
                        mapper.generate(codeModel, "ClassName", outputPkg, sourceFile.toUri().toURL());
                    } catch (MalformedURLException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            } catch (IOException ex) {
                System.err.println("can't open source in " + sourceDir + ": " + ex.getLocalizedMessage());
            }
        }

        try {
            codeModel.build(output);
        } catch (IOException ex) {
            System.err.println("error writing code model: " + ex.getLocalizedMessage());
        }
    }
}
