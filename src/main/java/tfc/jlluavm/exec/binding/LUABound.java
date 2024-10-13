package tfc.jlluavm.exec.binding;

public @interface LUABound {
    String name();

    /**
     * Valid values:
     * name: bound to the name
     * owner: bound to the owner
     * global: becomes a global variable
     * none: the object is not in a scope and cannot be referenced
     * <p>
     * if you have a class with binding name math, and it's scope is set to name, you would reference that with "math"
     * if you then have a function in that class which is bound to name sin, and scoped to owner.name, you would reference it with "math.sin()"
     */
    String scope() default "owner.name";
}
