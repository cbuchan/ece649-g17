package simulator.framework;

import jSimPack.SimTime;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.util.*;
import java.lang.reflect.*;

/**
 * A facade that provides a simple interface for instantiating objects, finding
 * methods, and invoking methods.
 * 
 * @author Justin Ray
 */
public class ReflectionFactory {

    boolean verbose;

    public ReflectionFactory() {
        this(false);
    }

    public ReflectionFactory(boolean verbose) {
        this.verbose = verbose;
    }


    /**
     * Convenience overload to create an object with no parameters .
     *
     * @param className
     * @param packagePath
     * @return
     * @throws ClassNotFoundException
     */

    public Object createObject(String className, List<String> packagePath) throws ClassNotFoundException {
        return createObjectFromMixed(className, packagePath, new Object[0], new String[0]);
    }


    /**
     * Convenience overload to create an object just from parameters
     * This method will cause an error if any of the parameters are null
     *
     * @param className
     * @param packagePath
     * @param parameters
     * @return
     * @throws ClassNotFoundException
     */

    public Object createObjectFromParameters(String className, List<String> packagePath, Object[] parameters) throws ClassNotFoundException {
        return createObjectFromMixed(className, packagePath, parameters, new String[parameters.length]);
    }

    /**
     * Convenience overload to create an object just from string arguments.
     * This method will cause an error if any of the strings are null
     *
     * @param className
     * @param packagePath
     * @param strings
     * @return
     * @throws ClassNotFoundException
     */
    public Object createObjectFromStrings(String className, List<String> packagePath, String[] strings) throws ClassNotFoundException {
        return createObjectFromMixed(className, packagePath, new Object[strings.length], strings);
    }

    /**
     * This class can be used to create objects from a mixture of pre-existing
     * objects and String parameters
     *
     * The parameters and strings arrays must be the same length, and they
     * must be mutually exclusive, i.e. for a given index value, one of the
     * arrays must have a null value, and the other must be non-null.
     *
     * @param className
     * @param packagePath
     * @param parameters array of objects to pass as parameters
     * @param strings array of string arguments
     * @return the object constructed using the given parameters.
     * @throws ClassNotFoundException if the class does not exist.
     * @throws IllegalArgumentException if the parameters don't fit with the class
     * or the parameters and strings arrays are inconsistent
     */
    public Object createObjectFromMixed(String className, List<String> packagePath, Object[] parameters, String[] strings) throws ClassNotFoundException {
        if (parameters.length != strings.length) {
            throw new IllegalArgumentException("parameters and strings arrays must be the same length");
        }

        for (int i = 0; i < parameters.length; i++) {
            if ((parameters[i] == null && strings[i] == null)
                    || (parameters[i] != null && strings[i] != null)) {
                throw new IllegalArgumentException("strings and parameters arrays are not mutually exclusive in element " + i);
            }
        }

        Class<?> c = findClass(className, packagePath);

        log("found a class: " + c.getName());

        Constructor<?>[] cons = c.getConstructors();
        if (cons.length == 0) {
            throw new IllegalArgumentException(className
                    + " has no public constructor");
        }

        Constructor<?> theConstructor = null;
        Class<?>[] paramTypes = null;
        for (Constructor<?> con : cons) {
            paramTypes = con.getParameterTypes();
            log("found constructor with " + paramTypes.length + " parameters.");

            if (paramTypes.length != parameters.length) {
                continue;
            }

            // Convert the string representation of each argument into an
            // Object to pass to the constructor.
            boolean match = true;
            for (int i = 0; i < paramTypes.length; ++i) {
                if (parameters[i] != null) {
                    //check the type of the parameter against the type in the constructor
                    try {
                        //the isInstance() check below will fail on primitive types
                        //so use the box() function to convert to the object wrapper
                        //classes.
                        if (paramTypes[i].isPrimitive()) {
                            //System.out.println("type is primitive");
                            paramTypes[i] = box(paramTypes[i]);
                        }

                        
                        //this section will fail to correctly compare primitive types
                        if (!paramTypes[i].isInstance(parameters[i])) {
                            match = false;
                            break;
                        }
                    } catch (ClassCastException ex) {
                        match = false;
                        break;
                    }
                } else {
                    //try to coerce the string to the given type
                    try {
                        Object o = parseString(paramTypes[i], strings[i]);
                        parameters[i] = o;
                    } catch (IllegalArgumentException ex) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                theConstructor = con;
                break;
            }
        }

        if (null == theConstructor) {
            if (parameters.length > 1) {
                throw new IllegalArgumentException("While trying to construct " + className + ", couldn't find a constructor that accepts the arguments " + commaConcatenate(parameters) + " and/or strings values " + commaConcatenate(strings) + " in packages " + commaConcatenate(packagePath));
            } else {
                throw new IllegalArgumentException("While trying to construct " + className + ", couldn't find a constructor with no arguments in packages " + commaConcatenate(packagePath));
            }
        }

        try {
            return theConstructor.newInstance(parameters);
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            ByteArrayOutputStream stackStream = new ByteArrayOutputStream();
            cause.printStackTrace(new PrintStream(stackStream));
            throw new IllegalArgumentException("Error while instantiating " + className + ". This probably means an exception was thrown in the controller constructor.  More information on the cause of this error is given below:\n" + stackStream.toString());
        }
    }

    /**
     * Converts a primitive class type to its equivalent java wrapper class.
     * I.e. int -> Integer, char -> Character, etc.
     * 
     * @param c Class object.  If a non-primitive class is passed, it will be
     * returned.
     * @return the appropriate non-primitive class
     */
    private Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        else if (c == Byte.TYPE) return Byte.class;
        else if (c == Short.TYPE) return Short.class;
        else if (c == Integer.TYPE) return Integer.class;
        else if (c == Long.TYPE) return Long.class;
        else if (c == Float.TYPE) return Float.class;
        else if (c == Double.TYPE) return Double.class;
        else if (c == Boolean.TYPE) return Boolean.class;
        else if (c == Character.TYPE) return Character.class;
        else throw new RuntimeException("If this error occurs, a primitive type is missing from the box() function");
    }

    /**
     * Utility class that tries to create a Class<?> object from a name and the given
     * package names.  
     * @param className
     * @param packagePath  paths should end in ., as in "simulator.elevatorcontrol."
     * @return class object if one exists
     * @throws ClassNotFoundException if no class with that name was found.
     */
    private Class<?> findClass(String className, List<String> packagePath) throws ClassNotFoundException {
        Class<?> c = null;
        if (packagePath == null || packagePath.size() == 0) {
            try {
                c = Class.forName(className);
            } catch (ClassNotFoundException e) {
            }
        } else {
            for (String pack : packagePath) {
                try {
                    //System.out.println(pack + words.get(0));
                    c = Class.forName(pack + className);
                } catch (ClassNotFoundException e) {
                }
            }
        }
        if (c == null) {
            throw new ClassNotFoundException("could not find class named \""
                    + className + "\" in paths " + commaConcatenate(packagePath));
        }
        return c;
    }

    /**
     * Overload for when we have an object instead of a class type
     * @param theClass
     * @param methodName
     * @return
     * @throws java.lang.NoSuchMethodException
     */
    public Method getMethod(Object targetObject, String methodName) throws NoSuchMethodException {
        return getMethod(targetObject.getClass(), methodName);
    }

    /**
     * Use this getMethod call for methods with no arguments
     * @param theClass
     * @param methodName
     * @return
     * @throws java.lang.NoSuchMethodException
     */
    public Method getMethod(Class<?> theClass, String methodName)
            throws NoSuchMethodException {
        log("getMethod(" + theClass + ", " + methodName + ")");
        for (Method m : theClass.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] argTypes = m.getParameterTypes();
            if (argTypes.length != 0) //throw new IllegalArgumentException("Method " + methodName + " exists but has " + argTypes.length + " arguments (expected none)");
            //can't error on the argument length, because there might be two methods with the same name.
            {
                continue;
            }
            return m;
        }
        throw new NoSuchMethodException("Class " + theClass.getSimpleName() + " contains no method named " + methodName + " with 0 arguments.");
    }

    /**
     * Get a method from the given class that matches the argument list.  The
     * matching is done by trying to parse the strings into the types of each
     * argument in the method.
     * 
     * @param theClass target class to search for methods
     * @param methodName name of method
     * @param args list of string arguments
     * @return
     * @throws NoSuchMethodException
     */
    public Method getMethod(Class<?> theClass, String methodName, List<String> args)
            throws NoSuchMethodException {
        log("getMethod(" + theClass + ", " + methodName + ", " + args + ")");
        for (Method m : theClass.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] argTypes = m.getParameterTypes();
            if (argTypes.length != args.size()) {
                continue;
            }
            try {
                for (int i = 0; i < argTypes.length; ++i) {
                    parseString(argTypes[i], args.get(i));
                }
            } catch (Exception e) {
                /*throw new IllegalArgumentException(m + " expects a "
                + argTypes[i].getName() + " for parameter " + i
                + ", but received " + args.get(i));            */
                continue;
            }
            return m;
        }
        throw new NoSuchMethodException("Class " + theClass.getSimpleName() + " contains no method named " + methodName + " which matches the arguments " + commaConcatenate(args));
    }

    /**
     * Overload for getMethod if what we have is a source object and not a class
     * @param sourceObject
     * @param methodName
     * @param args
     * @return
     * @throws NoSuchMethodException
     */
    public Method getMethod(Object sourceObject, String methodName, List<String> args)
            throws NoSuchMethodException {
        return getMethod(sourceObject.getClass(), methodName, args);
    }

    public Method getStaticMethod(Class<?> theClass, String methodName, List<String> args)
            throws NoSuchMethodException {
        log("getMethod(" + theClass + ", " + methodName + ", " + args + ")");
        for (Method m : theClass.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] argTypes = m.getParameterTypes();
            if (argTypes.length != args.size()) {
                continue;
            }
            try {
                for (int i = 0; i < argTypes.length; ++i) {
                    parseString(argTypes[i], args.get(i));
                }
            } catch (Exception e) {
                /*throw new IllegalArgumentException(m + " expects a "
                + argTypes[i].getName() + " for parameter " + i
                + ", but received " + args.get(i));            */
                continue;
            }
            return m;
        }
        throw new NoSuchMethodException("Class " + theClass.getSimpleName() + " contains no STATIC method named " + methodName + " which matches the arguments " + commaConcatenate(args));
    }

    /**
     * Use this method to create an object from a static factory method.  
     * E.g. HallCallButtonPayload.getWriteablePayload()
     * 
     * @param className name of the class to search for the method on
     * @param packagePaths paths to search when creating the class
     * @param methodName the method name
     * @param args arguments to pass to the method
     * @return an object that is returned by the static method
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     */
    public Object createFromFactoryMethod(String className, List<String> packagePaths, String methodName, List<String> args) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        Class<?> c = findClass(className, packagePaths);
        Method staticMethod = getStaticMethod(c, methodName, args);
        return invoke(staticMethod, null, args);
    }

    /**
     * Overload for when we have an object instead of a class
     * @param theClass
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     */
    public Field getField(Object target, String fieldName) throws NoSuchFieldException {
        return getField(target.getClass(), fieldName);
    }

    /**
     * Get a field by name from a given class
     * @param theClass
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     */
    public Field getField(Class<?> theClass, String fieldName) throws NoSuchFieldException {
        return theClass.getDeclaredField(fieldName);
    }

    /**
     * Get a value from a field of a given name in a class.
     * @param sourceClass
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public Object getStaticFieldValue(Class<?> sourceClass, String fieldName) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field idField = getField(sourceClass, fieldName);
        return idField.get(null);
    }

    /**
     * Tries to parse the value string into an object of the type returned by the method
     */
    public Object createReturnType(Method theMethod, String value) {
        return parseString(theMethod.getReturnType(), value);
    }

    /**
     * Tries to parse the value string into an object that matches the type of the field.
     * @param field
     * @param value
     * @return
     */
    public Object createFieldType(Field field, String value) {
        return parseString(field.getType(), value);
    }

    /**
     * Call the given method on the given object using the arguments in the args
     * list.  This call assumes that the argument list was verfied when
     * creating the method, so an exception will be thrown if the args are 
     * inapprprpriate.
     * 
     * @param theMethod
     * @param obj object to call the method on -- can be null if it is a static
     * method.
     * @param args
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public Object invoke(Method theMethod, Object obj, List<String> args)
            throws IllegalAccessException, InvocationTargetException {
        log("invoking method: " + theMethod);
        log("            obj: " + obj);
        log("           args: " + args);

        Class<?>[] argTypes = theMethod.getParameterTypes();
        log("   looking for types: " + Arrays.toString(argTypes));
        if (argTypes.length != args.size()) {
            throw new IllegalArgumentException(theMethod.getName()
                    + " does not accept this many arguments.  Expected " + argTypes.length + " arguments.");
        }

        Object[] objArgs = new Object[args.size()];

        for (int i = 0; i < argTypes.length; ++i) {
            try {
                objArgs[i] = parseString(argTypes[i], args.get(i));
            } catch (Exception e) {
                throw new IllegalArgumentException(theMethod.getName()
                        + " expects a " + argTypes[i].getName()
                        + " for parameter " + i + ", but received "
                        + args.get(i));
            }
        }

        log("  passing these args: " + Arrays.toString(objArgs));

        return theMethod.invoke(obj, objArgs);
    }

    public void log(String s) {
        if (verbose) {
            Harness.log("ReflectionFactory", s);
        }
    }

    /**
     * Attempt to convert the string value to an object of the given type.
     */
    @SuppressWarnings(value = "unchecked")
    private static Object parseString(Class type, String value)
            throws IllegalArgumentException {
        if (value == null) {
            return null;
        }
        if (Boolean.TYPE == type) {
            /* Boolean.parseBoolean(String) returns FALSE for anything other
             * than "true" (case insensitive). We want something a little more
             * robust than that.
             */
            if (value.equalsIgnoreCase("true")) {
                return Boolean.TRUE;
            }
            if (value.equalsIgnoreCase("false")) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("value \"" + value
                    + "\" could not" + " be parsed as a boolean value");
        }
        if (Byte.TYPE == type) {
            return Byte.parseByte(value);
        }
        if (Double.TYPE == type) {
            return Double.parseDouble(value);
        }
        if (Float.TYPE == type) {
            return Float.parseFloat(value);
        }
        if (Integer.TYPE == type) {
            return Integer.parseInt(value);
        }
        if (Long.TYPE == type) {
            return Long.parseLong(value);
        }
        if (Short.TYPE == type) {
            return Short.parseShort(value);
        }
        if (String.class == type) {
            return value;
        }
        if (SimTime.class.isAssignableFrom(type)) {
            return new SimTime(value);
        }
        if (type.isEnum()) /*
         * The next line will generate a compiler warning. I don't know how
         * to fix it.
         */ {
            return Enum.valueOf(type, value);
        }
        throw new IllegalArgumentException("value \"" + value + "\" could not"
                + " be interpreted as " + type);
    }

    public final static String commaConcatenate(List<String> strs) {
        return commaConcatenate(strs, 0);
    }

    public final static String commaConcatenate(List<String> strs, int startingIndex) {
        if (startingIndex >= strs.size()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startingIndex; i < strs.size(); i++) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(strs.get(i));
        }
        return sb.toString();
    }

    public final static String commaConcatenate(Object[] objects) {
        return commaConcatenate(objects, 0);
    }

    public final static String commaConcatenate(Object[] objects, int startingIndex) {
        if (startingIndex >= objects.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startingIndex; i < objects.length; i++) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            if (objects[i] == null) {
                sb.append("null");
            } else {
                sb.append(objects[i].toString());
            }
        }
        return sb.toString();
    }
}
