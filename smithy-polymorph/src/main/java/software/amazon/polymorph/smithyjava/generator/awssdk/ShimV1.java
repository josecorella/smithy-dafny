package software.amazon.polymorph.smithyjava.generator.awssdk;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import software.amazon.polymorph.smithyjava.generator.Generator;
import software.amazon.polymorph.smithyjava.nameresolver.Dafny;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.StringUtils;

import static software.amazon.polymorph.smithyjava.nameresolver.Constants.DAFNY_RESULT_CLASS_NAME;
import static software.amazon.polymorph.smithyjava.nameresolver.Constants.DAFNY_TUPLE0_CLASS_NAME;
import static software.amazon.polymorph.smithyjava.nameresolver.Constants.SMITHY_API_UNIT;


/**
 * Generates an AWS SDK Shim for the AWS SKD for Java V1
 * exposing an AWS Service's operations to Dafny Generated Java.
 */
public class ShimV1 extends Generator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShimV1.class);
    public ShimV1(JavaAwsSdkV1 awsSdk) {
        super(awsSdk);
    }

    @Override
    public Set<JavaFile> javaFiles() {
        JavaFile.Builder builder = JavaFile
                .builder(subject.dafnyNameResolver.packageName(), shim());
        return Collections.singleton(builder.build());
    }

    TypeSpec shim() {
        return TypeSpec
                .classBuilder(
                        ClassName.get(subject.dafnyNameResolver.packageName(), "Shim"))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(subject.dafnyNameResolver.typeForShape(subject.serviceShape.getId()))
                .addField(
                        subject.nativeNameResolver.typeForService(subject.serviceShape),
                        "_impl", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(constructor())
                .addMethods(
                        subject.serviceShape.getAllOperations()
                                .stream()
                                .map(this::operation)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList()))
                .build();
    }

    MethodSpec constructor() {
        return MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(
                        subject.nativeNameResolver.typeForService(subject.serviceShape),
                        "impl")
                .addStatement("_impl = impl")
                .build();
    }

    Optional<MethodSpec> operation(final ShapeId operationShapeId) {
        final OperationShape operationShape = subject.model.expectShape(operationShapeId, OperationShape.class);
        ShapeId inputShapeId = operationShape.getInputShape();
        ShapeId outputShapeId = operationShape.getOutputShape();
        TypeName dafnyOutput = subject.dafnyNameResolver.typeForShape(outputShapeId);
        String operationName = operationShape.toShapeId().getName();
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(StringUtils.capitalize(operationName))
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(
                        Dafny.asDafnyResult(
                                dafnyOutput,
                                subject.dafnyNameResolver.abstractClassForError()
                        ))
                .addParameter(subject.dafnyNameResolver.typeForShape(inputShapeId), "input")
                .addStatement("$T converted = ToNative.$L(input)",
                        subject.nativeNameResolver.typeForShape(inputShapeId),
                        StringUtils.capitalize(inputShapeId.getName()))
                .beginControlFlow("try");
        if (outputShapeId.equals(SMITHY_API_UNIT)) {
            builder.addStatement("_impl.$L(converted)",
                            StringUtils.uncapitalize(operationName))
                    .addStatement("return $T.create_Success($T.create())",
                            DAFNY_RESULT_CLASS_NAME, DAFNY_TUPLE0_CLASS_NAME);
        } else {
            builder.addStatement("$T result = _impl.$L(converted)",
                            subject.nativeNameResolver.typeForOperationOutput(outputShapeId),
                            StringUtils.uncapitalize(operationName))
                    .addStatement("$T dafnyResponse = ToDafny.$L(result)",
                            dafnyOutput,
                            StringUtils.capitalize(outputShapeId.getName()))
                    .addStatement("return $T.create_Success(dafnyResponse)",
                            DAFNY_RESULT_CLASS_NAME);
        }

        operationShape.getErrors().forEach(shapeId ->
                builder
                        .nextControlFlow("catch ($T ex)", subject.nativeNameResolver.typeForShape(shapeId))
                        .addStatement("return $T.create_Failure(ToDafny.Error(ex))",
                                DAFNY_RESULT_CLASS_NAME)
        );
        return Optional.of(builder
                .nextControlFlow("catch ($T ex)", subject.nativeNameResolver.baseErrorForService())
                .addStatement("return $T.create_Failure(ToDafny.Error(ex))",
                        DAFNY_RESULT_CLASS_NAME)
                .endControlFlow()
                .build());
    }

}
