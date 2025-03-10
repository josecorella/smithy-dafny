// Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package software.amazon.polymorph.smithyjava.nameresolver;

import com.squareup.javapoet.ClassName;

import java.util.Set;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

public class Constants {
    public static final ShapeId SMITHY_API_UNIT = ShapeId.fromParts("smithy.api", "Unit");
    public static final ClassName DAFNY_RESULT_CLASS_NAME = ClassName.get("Wrappers_Compile", "Result");
    public static final ClassName DAFNY_TUPLE0_CLASS_NAME = ClassName.get("dafny", "Tuple0");
    public static final ClassName DAFNY_TYPE_DESCRIPTOR_CLASS_NAME = ClassName.get("dafny", "TypeDescriptor");
    public static final ClassName DAFNY_SEQUENCE_CLASS_NAME = ClassName.get("dafny", "DafnySequence");
    public static final ClassName DAFNY_SET_CLASS_NAME = ClassName.get("dafny", "DafnySet");
    public static final ClassName DAFNY_MAP_CLASS_NAME = ClassName.get("dafny", "DafnyMap");
    public static final Set<ShapeType> SHAPE_TYPES_LIST_SET = Set.of(ShapeType.LIST, ShapeType.SET);
    public static final Set<ShapeType> SHAPE_TYPES_LIST_SET_MAP = Set.of(ShapeType.LIST, ShapeType.SET, ShapeType.MAP);
}
