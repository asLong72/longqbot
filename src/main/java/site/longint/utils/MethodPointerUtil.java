package site.longint.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodPointerUtil {
    public static Method getMethodwithNoParam(Object theObject, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // getDeclaredMethod()无视函数访问权限
        // getMethod
        return theObject.getClass().getDeclaredMethod(methodName);
    }
    public static Method getMethodwithTwoParams(Object theObject, String methodName, Class p1c, Class p2c) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // getDeclaredMethod()无视函数访问权限
        // getMethod
        return theObject.getClass().getDeclaredMethod(methodName, p1c, p2c);
    }
    public static Object methodCaller(Object theObject, String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return theObject.getClass().getMethod(methodName).invoke(theObject);
        // Catch the exceptions
    }
}
