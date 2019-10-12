package me.shawlaf.varlight.nms;

import java.lang.reflect.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class ReflectionHelper {

    private ReflectionHelper() {

    }

    public static Field getField(Class declaredIn, String name) {
        try {
            return declaredIn.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Method getMethod(Class declaredIn, String name, Class... argTypes) {
        try {
            return declaredIn.getDeclaredMethod(name, argTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void fieldAccess(Field field, FieldAccess fieldAccess) {
        if (!fieldAccess.hasAccess(field)) {
            fieldAccess.apply(field);
        }
    }

    private static Object getValue(Field field, Object instance) {
        try {
            return Safe.getValue(field, instance);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T getStatic(Class declaredIn, String name) {
        return getStatic(getField(declaredIn, name));
    }

    public static <T> T getStatic(Field field) {
        return (T) getValue(field, null);
    }

    public static <T> T get(Object instance, String name) {
        return get(getField(instance.getClass(), name), instance);
    }

    public static <T> T get(Field field, Object instance) {
        return (T) getValue(field, instance);
    }

    private static void setValue(Object instance, Field field, Object value) {
        try {
            Safe.setValue(instance, field, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setStatic(Class declaredIn, String name, Object value) {
        try {
            setStatic(declaredIn.getDeclaredField(name), value);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void setStatic(Field field, Object value) {
        setValue(null, field, value);
    }

    public static <I> void set(I instance, String name, Object value) {
        try {
            set(instance, instance.getClass().getDeclaredField(name), value);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static <I> void set(I instance, Field field, Object value) {
        setValue(instance, field, value);
    }

    private static Object invokeMethod(Object instance, Method method, Object... args) {
        try {
            return Safe.invokeMethod(instance, method, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T invokeStatic(Method method, Object... args) {
        return (T) invokeMethod(null, method, args);
    }

    public static <T> T invoke(Object instance, Method method, Object... args) {
        return (T) invokeMethod(instance, method, args);
    }

    private enum FieldAccess {
        READ(AccessibleObject::isAccessible, field -> field.setAccessible(true)),
        READ_WRITE(
                field -> field.isAccessible() && (field.getModifiers() & Modifier.FINAL) == 0,
                field -> {
                    field.setAccessible(true);

                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, modifiersField.getInt(field) & ~Modifier.FINAL);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
        );

        private final Predicate<Field> hasAccess;
        private final Consumer<Field> apply;

        FieldAccess(Predicate<Field> hasAccess, Consumer<Field> apply) {
            this.hasAccess = hasAccess;
            this.apply = apply;
        }

        public void apply(Field field) {
            apply.accept(field);
        }

        public boolean hasAccess(Field field) {
            return hasAccess.test(field);
        }
    }

    public static class Safe {
        private Safe() {

        }

        public static Field getField(Class declaredIn, String name) throws NoSuchFieldException {
            return declaredIn.getDeclaredField(name);
        }

        public static Method getMethod(Class declaredIn, String name, Class... argTypes) throws NoSuchMethodException {
            return declaredIn.getDeclaredMethod(name, argTypes);
        }

        private static Object getValue(Field field, Object instance) throws IllegalAccessException {
            fieldAccess(field, FieldAccess.READ);
            return field.get(instance);
        }


        public static <T> T getStatic(Class declaredIn, String name) throws NoSuchFieldException, IllegalAccessException {
            return getStatic(getField(declaredIn, name));
        }

        public static <T> T getStatic(Field field) throws IllegalAccessException {
            return (T) getValue(field, null);
        }

        public static <T> T get(Object instance, String name) throws NoSuchFieldException, IllegalAccessException {
            return get(getField(instance.getClass(), name), instance);
        }

        public static <T> T get(Field field, Object instance) throws IllegalAccessException {
            return (T) getValue(field, instance);
        }

        private static void setValue(Object instance, Field field, Object value) throws IllegalAccessException {
            fieldAccess(field, FieldAccess.READ_WRITE);
            field.set(instance, value);
        }

        public static void setStatic(Class declaredIn, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
            setStatic(declaredIn.getDeclaredField(name), value);
        }

        public static void setStatic(Field field, Object value) throws IllegalAccessException {
            setValue(null, field, value);
        }

        public static <I> void set(I instance, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
            set(instance, instance.getClass().getDeclaredField(name), value);
        }

        public static <I> void set(I instance, Field field, Object value) throws IllegalAccessException {
            setValue(instance, field, value);
        }

        private static Object invokeMethod(Object instance, Method method, Object... args) throws InvocationTargetException, IllegalAccessException {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }

            return method.invoke(instance, args);
        }

        public static Object invokeStatic(Method method, Object... args) throws InvocationTargetException, IllegalAccessException {
            return invokeMethod(null, method, args);
        }

        public static Object invoke(Object instance, Method method, Object... args) throws InvocationTargetException, IllegalAccessException {
            return invokeMethod(instance, method, args);
        }


    }


}
