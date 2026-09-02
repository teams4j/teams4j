package io.github.teams4j.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import javax.lang.model.element.Modifier;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import org.junit.jupiter.api.Test;

/**
 * Pins the JavaPoet capabilities the generator depends on.
 *
 * <p>The generated model requires sealed interfaces with {@code permits}, records, and Jackson's polymorphic
 * annotations. If any could not be emitted the whole approach would need rethinking, so a library
 * upgrade that breaks one should fail here first.
 */
class JavaPoetCapabilityTest {

    private static final String PKG = "io.github.teams4j.cards";
    private static final ClassName JSON_TYPE_NAME = ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeName");
    private static final ClassName JSON_PROPERTY = ClassName.get("com.fasterxml.jackson.annotation", "JsonProperty");

    /**
     * Union points become sealed interfaces. Because each generated type lands in its own file, the
     * {@code permits} clause has to be explicit: it may only be omitted within one compilation unit.
     */
    @Test
    void emitsSealedInterfaceWithExplicitPermitsClause() {
        TypeSpec cardElement = TypeSpec.interfaceBuilder("CardElement")
                .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
                .addPermittedSubclass(ClassName.get(PKG + ".element", "TextBlock"))
                .addPermittedSubclass(ClassName.get(PKG + ".element", "Image"))
                .build();

        String out = JavaFile.builder(PKG, cardElement).build().toString();

        assertThat(out).contains("public sealed interface CardElement");
        assertThat(out).contains("permits TextBlock, Image");
    }

    /** Concrete types become records, with the discriminator carried by {@code @JsonTypeName}. */
    @Test
    void emitsRecordImplementingSealedRoot() {
        MethodSpec components = MethodSpec.compactConstructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(String.class, "text")
                        .addAnnotation(AnnotationSpec.builder(JSON_PROPERTY)
                                .addMember("value", "$S", "text")
                                .build())
                        .build())
                .addParameter(String.class, "weight")
                .build();

        TypeSpec textBlock = TypeSpec.recordBuilder("TextBlock")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(PKG, "CardElement"))
                .addAnnotation(AnnotationSpec.builder(JSON_TYPE_NAME)
                        .addMember("value", "$S", "TextBlock")
                        .build())
                .recordConstructor(components)
                .build();

        String out = JavaFile.builder(PKG + ".element", textBlock).build().toString();

        assertThat(out).contains("@JsonTypeName(\"TextBlock\")");
        assertThat(out).contains("public record TextBlock(");
        assertThat(out).contains("implements CardElement");
        assertThat(out).contains("@JsonProperty(\"text\")");
    }
}
