package com.softlayer.api.example;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Alternate entry point for examples that has the class name as the first argument */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("First parameter must be example name");
        }
        Method main = Class.forName(Main.class.getPackage().getName() + "." + args[0]).
            getDeclaredMethod("main", String[].class);
        // Take the class name off the front
        main.invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
    }
}
