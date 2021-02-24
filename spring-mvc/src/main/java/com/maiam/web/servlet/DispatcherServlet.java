package com.maiam.web.servlet;

import com.maiam.web.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DispatcherServlet extends HttpServlet {

    /**
     * 上下文配置
     */
    private static Properties contextConfig = new Properties();

    /**
     * 扫描的类全名
     */
    private static ArrayList<String> classNames = new ArrayList<String>();

    /**
     * IOC容器
     */
    private static Map<String, Object> ioc = new ConcurrentHashMap<String, Object>();

    /**
     * 处理映射
     */
    private static Map<String, Method> handlerMapping = new ConcurrentHashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 6.派遣
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 INNER SERVER ERROR." + Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public void init(ServletConfig config) {
        try {

            // 1.加载配置文件
            doLoadConfig(config.getInitParameter("contextConfigLocation"));

            // 2.扫描类
            doScanClass(contextConfig.get("basePackage").toString());

            // 3.实例化类并保存到IOC容器中
            doInitInstance();

            // 4.完成依赖注入
            doAutowired();

            // 5.初始化HandlerMapping
            doInitHandlerMapping();

            System.out.println("DispatcherServlet 初始化完成...");
        } catch (Exception ex) {
            System.out.println("DispatcherServlet 初始化异常...");
            ex.printStackTrace();
        }
    }

    /**
     * 1.加载配置文件
     *
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = null;
        try {
            // 使用类加载器加载配置文件并生成对应的输入流
            is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            // 使用properties对象加载输入流
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 2.扫描类
     *
     * @param basePackage
     */
    private void doScanClass(String basePackage) {
        // 将包名转换为路径
        String path = "/" + basePackage.replaceAll("\\.", "/");
        URL url = this.getClass().getClassLoader().getResource(path);
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                // 递归遍历目录
                doScanClass(basePackage + "." + file.getName());
            } else {
                // 过滤非class文件
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                // 取出全类名
                classNames.add(basePackage + "." + file.getName().replace(".class", ""));
            }
        }
    }

    /**
     * 3.实例化类并保存到IOC容器中
     * 根据扫描出来的类全名实例化类，并放入IOC容器中。
     * ioc.put(beanName,instance);
     * beanName 有三种取值：
     * 1、类名首字母小写
     * 2、@Controller、@Service、@Repository的value值
     * 3、@Service、@Repository注解的类所实现接口的全名
     * 该方法后续需要进一步优化
     */
    private void doInitInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Controller annotation = clazz.getAnnotation(Controller.class);
                    String beanName = annotation.value();
                    if (isBlank(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    Service annotation = clazz.getAnnotation(Service.class);
                    String beanName = annotation.value();
                    if (isBlank(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    ioc.put(clazz.getName(), instance);
                    // 根据类型注入
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The beanName is existing. Please change the name of " + i.getName());
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else if (clazz.isAnnotationPresent(Repository.class)) {
                    Repository annotation = clazz.getAnnotation(Repository.class);
                    String beanName = annotation.value();
                    if (isBlank(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    ioc.put(clazz.getName(), instance);
                    // 根据类型注入
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The beanName is existing. Please change the name of " + i.getName());
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 4.完成依赖注入
     */
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                Autowired annotation = field.getAnnotation(Autowired.class);
                String beanName = annotation.value();
                if (isBlank(beanName)) {
                    beanName = field.getType().getName();
                }
                // 强制暴力访问 private、protected 字段、属性
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 5.初始化HandlerMapping
     */
    private void doInitHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                baseUrl = clazz.getAnnotation(RequestMapping.class).value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                String url = ("/" + baseUrl + "/" + annotation.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapping:" + url + "," + method);
            }
        }
    }

    /**
     * 6.派遣
     *
     * @param req
     * @param resp
     * @throws Exception
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (!handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND.");
            return;
        }
        Method method = handlerMapping.get(url);
        // 获取请求参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        // 获取方法参数类型
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 设置方法参数
        Object[] params = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            String parameterTypeName = parameterTypes[i].getSimpleName();
            if ("HttpServletRequest".equals(parameterTypeName)) {
                params[i] = req;
                continue;
            }
            if ("HttpServletResponse".equals(parameterTypeName)) {
                params[i] = resp;
                continue;
            }
            // 待完善：方法参数类型不定...
            if ("String".equals(parameterTypeName)) {
                for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
                    String value = Arrays.toString(stringEntry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    params[i] = value;
                }
                continue;
            }
        }
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), params);
    }

    private String toLowerFirstCase(String s) {
        char[] chars = s.toCharArray();
        chars[0] += 32;
        return new String(chars);
    }

    private boolean isBlank(String s) {
        if (s == null) {
            return true;
        }
        return "".equals(s.trim());
    }
}