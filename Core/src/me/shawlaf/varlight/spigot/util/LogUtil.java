package me.shawlaf.varlight.spigot.util;

import java.util.function.Supplier;

public class LogUtil {

    private LogUtil() {

    }

    public static Supplier<String> supply(String s) {
        return () -> s;
    }

    public static StackTraceElement currentStackTraceElem() {
        return Thread.currentThread().getStackTrace()[2];
    }

    public static StackTraceElement currentStackTraceElem(int mod) {
        return Thread.currentThread().getStackTrace()[2 + mod];
    }

    public static int currentLineNumber() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }

    public static int currentLineNumber(int mod) {
        return Thread.currentThread().getStackTrace()[2 + mod].getLineNumber();
    }

    public static String currentFileName() {
        return Thread.currentThread().getStackTrace()[2].getFileName();
    }

    public static String currentFileName(int mod) {
        return Thread.currentThread().getStackTrace()[2 + mod].getFileName();
    }
}
