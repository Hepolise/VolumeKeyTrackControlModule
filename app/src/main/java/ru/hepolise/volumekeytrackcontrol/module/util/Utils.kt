package ru.hepolise.volumekeytrackcontrol.module.util

import android.content.Context
import android.os.Handler
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.Method

fun XposedInterface.Chain.getContext() = getObjectField("mContext") as Context

fun XposedInterface.Chain.getHandler() = getObjectField("mHandler") as Handler

fun XposedInterface.Chain.getObjectField(fieldName: String): Any? {
    return thisObject.getPrivateField(fieldName)
}

fun Any.getPrivateField(fieldName: String): Any? {
    var currentClass: Class<*>? = javaClass
    while (currentClass != null && currentClass != Any::class.java) {
        try {
            val field = currentClass.getDeclaredField(fieldName)
            field.isAccessible = true
            return field.get(this)
        } catch (e: NoSuchFieldException) {
            currentClass = currentClass.superclass
        }
    }
    throw NoSuchFieldException("Field '$fieldName' not found in class hierarchy of ${javaClass.name}")
}

fun Class<*>.getDeclaredMethodInHierarchy(
    methodName: String,
    vararg parameterTypes: Class<*>
): Method {
    var currentClass: Class<*>? = this
    while (currentClass != null && currentClass != Any::class.java) {
        try {
            val method = currentClass.getDeclaredMethod(methodName, *parameterTypes)
            method.isAccessible = true
            return method
        } catch (e: NoSuchMethodException) {
            currentClass = currentClass.superclass
        }
    }
    throw NoSuchMethodException(
        "Method '$methodName' with params ${parameterTypes.map { it.name }} " +
            "not found in class hierarchy of ${this.name}"
    )
}

fun Class<*>.getDeclaredFieldInHierarchy(fieldName: String): Field {
    var currentClass: Class<*>? = this
    while (currentClass != null && currentClass != Any::class.java) {
        try {
            val field = currentClass.getDeclaredField(fieldName)
            field.isAccessible = true
            return field
        } catch (e: NoSuchFieldException) {
            currentClass = currentClass.superclass
        }
    }
    throw NoSuchFieldException("Field '$fieldName' not found in class hierarchy of ${this.name}")
}

fun String.toClass(classLoader: ClassLoader) = Class.forName(this, true, classLoader)
