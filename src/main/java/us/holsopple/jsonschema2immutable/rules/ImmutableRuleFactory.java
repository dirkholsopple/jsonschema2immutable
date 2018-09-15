package us.holsopple.jsonschema2immutable.rules;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocCommentable;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.Iterator;

@SuppressWarnings("WeakerAccess")
public class ImmutableRuleFactory extends RuleFactory {

    public ImmutableRuleFactory(GenerationConfig generationConfig, Annotator annotator, SchemaStore schemaStore) {
        super(new GenerationConfig () {
            @Override
            public boolean isGenerateBuilders() {
                return false;
            }

            @Override
            public boolean isUsePrimitives() {
                return true;
            }

            @Override
            public Iterator<URL> getSource() {
                return null;
            }

            @Override
            public File getTargetDirectory() {
                return generationConfig.getTargetDirectory();
            }

            @Override
            public String getTargetPackage() {
                return generationConfig.getTargetPackage();
            }

            @Override
            public char[] getPropertyWordDelimiters() {
                return generationConfig.getPropertyWordDelimiters();
            }

            @Override
            public boolean isUseLongIntegers() {
                return generationConfig.isUseLongIntegers();
            }

            @Override
            public boolean isUseBigIntegers() {
                return generationConfig.isUseBigIntegers();
            }

            @Override
            public boolean isUseDoubleNumbers() {
                return generationConfig.isUseDoubleNumbers();
            }

            @Override
            public boolean isUseBigDecimals() {
                return generationConfig.isUseBigDecimals();
            }

            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false;
            }

            @Override
            public boolean isIncludeToString() {
                return false;
            }

            @Override
            public String[] getToStringExcludes() {
                return new String[0];
            }

            @Override
            public AnnotationStyle getAnnotationStyle() {
                return generationConfig.getAnnotationStyle();
            }

            @Override
            public InclusionLevel getInclusionLevel() {
                return generationConfig.getInclusionLevel();
            }

            @Override
            public Class<? extends Annotator> getCustomAnnotator() {
                return generationConfig.getCustomAnnotator();
            }

            @Override
            public Class<? extends RuleFactory> getCustomRuleFactory() {
                return generationConfig.getCustomRuleFactory();
            }

            @Override
            public boolean isIncludeJsr303Annotations() {
                return generationConfig.isIncludeJsr303Annotations();
            }

            @Override
            public boolean isIncludeJsr305Annotations() {
                return true;
            }

            @Override
            public boolean isUseOptionalForGetters() {
                return false;
            }

            @Override
            public SourceType getSourceType() {
                return generationConfig.getSourceType();
            }

            @Override
            public boolean isRemoveOldOutput() {
                return generationConfig.isRemoveOldOutput();
            }

            @Override
            public String getOutputEncoding() {
                return generationConfig.getOutputEncoding();
            }

            @Override
            public boolean isUseJodaDates() {
                return generationConfig.isUseJodaDates();
            }

            @Override
            public boolean isUseJodaLocalDates() {
                return generationConfig.isUseJodaLocalDates();
            }

            @Override
            public boolean isUseJodaLocalTimes() {
                return generationConfig.isUseJodaLocalTimes();
            }

            @Override
            public boolean isParcelable() {
                return false;
            }

            @Override
            public boolean isSerializable() {
                return false;
            }

            @Override
            public FileFilter getFileFilter() {
                return generationConfig.getFileFilter();
            }

            @Override
            public boolean isInitializeCollections() {
                return true;
            }

            @Override
            public String getClassNamePrefix() {
                return generationConfig.getClassNamePrefix();
            }

            @Override
            public String getClassNameSuffix() {
                return generationConfig.getClassNameSuffix();
            }

            @Override
            public String[] getFileExtensions() {
                return generationConfig.getFileExtensions();
            }

            @Override
            public boolean isIncludeConstructors() {
                return false;
            }

            @Override
            public boolean isConstructorsRequiredPropertiesOnly() {
                return false;
            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return false;
            }

            @Override
            public boolean isIncludeAccessors() {
                return false;
            }

            @Override
            public boolean isIncludeGetters() {
                return false;
            }

            @Override
            public boolean isIncludeSetters() {
                return false;
            }

            @Override
            public String getTargetVersion() {
                return generationConfig.getTargetVersion();
            }

            @Override
            public boolean isIncludeDynamicAccessors() {
                return false;
            }

            @Override
            public boolean isIncludeDynamicGetters() {
                return false;
            }

            @Override
            public boolean isIncludeDynamicSetters() {
                return false;
            }

            @Override
            public boolean isIncludeDynamicBuilders() {
                return false;
            }

            @Override
            public String getDateTimeType() {
                return generationConfig.getDateTimeType();
            }

            @Override
            public String getDateType() {
                return generationConfig.getDateType();
            }

            @Override
            public String getTimeType() {
                return generationConfig.getTimeType();
            }

            @Override
            public boolean isFormatDates() {
                return generationConfig.isFormatDates();
            }

            @Override
            public boolean isFormatTimes() {
                return generationConfig.isFormatTimes();
            }

            @Override
            public boolean isFormatDateTimes() {
                return generationConfig.isFormatDateTimes();
            }

            @Override
            public String getCustomDatePattern() {
                return generationConfig.getCustomDatePattern();
            }

            @Override
            public String getCustomTimePattern() {
                return generationConfig.getCustomTimePattern();
            }

            @Override
            public String getCustomDateTimePattern() {
                return generationConfig.getCustomDateTimePattern();
            }

            @Override
            public String getRefFragmentPathDelimiters() {
                return generationConfig.getRefFragmentPathDelimiters();
            }

            @Override
            public SourceSortOrder getSourceSortOrder() {
                return generationConfig.getSourceSortOrder();
            }

            @Override
            public Language getTargetLanguage() {
                return generationConfig.getTargetLanguage();
            }
        }, annotator, schemaStore);
    }

    @Override
    public Rule<JPackage, JType> getObjectRule() {
        return new ObjectRule(this);
    }

    @Override
    public Rule<JDefinedClass, JDefinedClass> getPropertiesRule() {
        return new PropertiesRule(this);
    }

    @Override
    public Rule<JDefinedClass, JDefinedClass> getPropertyRule() {
        return new PropertyRule(this);
    }

    @Override
    public Rule<JDocCommentable, JDocCommentable> getRequiredRule() {
        return new RequiredRule();
    }
}
