package com.example.tomcat.config;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Envuelve un caso de uso en un proxy que ejecuta cada método dentro de una transacción.
 * Así los servicios de la capa application no dependen de Spring y, aun así, operaciones
 * como "registrar pago + generar factura" se confirman juntas o se revierten juntas.
 */
public final class TransaccionalProxy {

    private TransaccionalProxy() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T envolver(Class<T> puerto, T destino, PlatformTransactionManager transactionManager,
                                 boolean soloLectura) {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        transaccion.setReadOnly(soloLectura);

        return (T) Proxy.newProxyInstance(
                puerto.getClassLoader(),
                new Class<?>[]{puerto},
                (proxy, metodo, args) -> {
                    if (metodo.getDeclaringClass() == Object.class) {
                        return invocar(destino, metodo, args);
                    }
                    return transaccion.execute(estado -> invocar(destino, metodo, args));
                }
        );
    }

    private static Object invocar(Object destino, Method metodo, Object[] args) {
        try {
            return metodo.invoke(destino, args);
        } catch (InvocationTargetException e) {
            // Se relanza la excepción original para que GlobalExceptionHandler la traduzca a su código HTTP.
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (causa instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(causa);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
