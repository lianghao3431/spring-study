package com.lh.spring.mvc.servlet;

import com.lh.spring.mvc.annotation.*;
import com.sun.org.glassfish.gmbal.Description;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName LhDispatchServlet
 * @Description
 * @Date 2019/7/20 15:38
 * @Aurhor liang.hao
 */
public class LhDispatchServlet extends HttpServlet {

    /**配置读取*/
    private Properties properties = new Properties();

    /**配置包下面的className集合*/
    private List<String> clasNameList = new ArrayList<>();

    /**IOC 容器，用来存储实例化的对象*/
    private Map<String, Object> ioc = new HashMap<>();

    /**controller中 被requeMapping注解的方法，以及访问url的集合*/
    private List<HandlerMapping> handlerMappings = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            /**6、最后的调用*/
            doDispatcher(req, resp);
        } catch (Exception e) {
            resp.getWriter().write(e.getMessage());
        }

    }

    /**
    * @Author liang.hao
    * @Description doPost的调用实现逻辑
    * @Date 20:42 2019/7/21
    * @Param
    * @return
    **/
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        /**根据调用的信息，获取相关的handlerMapping */
        HandlerMapping handlerMapping = getHandlerMapping(req);
        /**判断是否获取到相关的handlerMapping，如果没有则代表url不存在，返回404*/
        if (null == handlerMapping) {
            resp.getWriter().write("404 Not Found");
            return;
        }
        /**获取url对应的controller方法*/
        Method method = handlerMapping.method;
        /**获取调用入参的map，http默认是字符串协议，所以所有参数会以字符串数组的方式传入*/
        Map<String, String[]> map = req.getParameterMap();
        /**根据方法，获取该方法的形参类型*/
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] objectArr = new Object[parameterTypes.length];
        /**循环入参map，已填写方法入参数组*/
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String paramName = entry.getKey();
            /**通过handlerMapping的参数map ， 来获取相应的参数值，以及参数的位置*/
            if (handlerMapping.paramerIndexMap.containsKey(paramName)) {
                int i = handlerMapping.paramerIndexMap.get(paramName);
                /**将参数中的[] 替换掉*/
                String value = Arrays.toString(map.get(paramName)).replaceAll("\\[|\\]", "");
                /**将String类型的参数，转换成形成的类型，放入入参数组中*/
                objectArr[i] = covert(parameterTypes[i], value);
            }
        }
        /**判断是否将httpServletRequest ,HttpServletResponse 入参，如果有则加到参数数组中*/
        if(handlerMapping.paramerIndexMap.containsKey(HttpServletRequest.class.getName())){
            int reqIndex = handlerMapping.paramerIndexMap.get(HttpServletRequest.class.getName());
            objectArr[reqIndex] = req;
        }
        if(handlerMapping.paramerIndexMap.containsKey(HttpServletResponse.class.getName())){
            int respIndex = handlerMapping.paramerIndexMap.get(HttpServletResponse.class.getName());
            objectArr[respIndex] = resp;
        }
        /**通过反射来调用方法，并获取返回值*/
        Object returnValue = method.invoke(handlerMapping.controller, objectArr);
        if(null == returnValue || returnValue instanceof Void){
            return;
        }
        resp.getWriter().write(returnValue.toString());
    }

    /**
    * @Author liang.hao
    * @Description 强制转换，只做了Integer ，这里可以用策略模式
    * @Date 20:51 2019/7/21
    * @Param [parameterType, value]
    * @return java.lang.Object
    **/
    private Object covert(Class<?> parameterType, String value) {
        if (parameterType == Integer.class) {
            return Integer.valueOf(value);
        }
        return value;
    }

    /**
    * @Author liang.hao
    * @Description 遍历handlerMappingList ，获取url对应的handlerMapping
    * @Date 20:52 2019/7/21
    * @Param [request]
    * @return com.lh.spring.mvc.servlet.LhDispatchServlet.HandlerMapping
    **/
    private HandlerMapping getHandlerMapping(HttpServletRequest request) {
        if (handlerMappings.isEmpty()) {
            return null;
        }
        /**获取相对路径*/
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        /**拼接完整的url决定路径，*/
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        for (HandlerMapping handlerMapping : handlerMappings) {
            /**正则表达式匹配，匹配到返回*/
            Matcher matcher = handlerMapping.url.matcher(url);
            if (matcher.matches()) {
                return handlerMapping;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        /**1、加载配置文件*/
        doConfigLoad(config.getInitParameter("contextConfigLocation"));
        /**2、扫描相关的类*/
        doScanner(properties.getProperty("scannerPath"));
        /**3、初始化类，放到IOC容器中*/
        doInstance();

        /**4、注入完成*/
        doAutowired();
        /**5、初始化handlerMapp*/
        initHandlerMapping();

    }

    /**
    * @Author liang.hao
    * @Description 注入相关的类的属性
    * @Date 20:55 2019/7/21
    * @Param []
    * @return void
    **/
    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        /**遍历IOC容器*/
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            /**获取类里面的所有属性，包括私有的*/
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            /**遍历属性*/
            for (Field field : fields) {
                if (!field.isAnnotationPresent(LhAutowired.class)) {
                    continue;
                }
                /**属性被LhAutowired注解，则获取LhAutowired对象*/
                LhAutowired lhAutowired = field.getAnnotation(LhAutowired.class);
                /**获取该属性是否是别名注入(及自定义了名称)，*/
                String beanName = lhAutowired.value().trim();
                if ("".equals(beanName)) {
                    /**不是，则通过类型注入，直接获取类型名称*/
                    beanName = field.getType().getName();
                }
                /**反射强吻，直接设置Accessible的值为true ， 就可以对私有属性设置值*/
                field.setAccessible(true);
                try {
                    /**通过beanName从IOC容器中获取实例化的对象，并给他注入属性*/
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
    * @Author liang.hao
    * @Description 初始化handlerMappingList
    * @Date 20:55 2019/7/21
    * @Param []
    * @return void
    **/
    private void initHandlerMapping() {

        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Object object = entry.getValue();
            /**判断类是否被LhController注解了，没有直接跳过*/
            if (!object.getClass().isAnnotationPresent(LhController.class)) {
                continue;
            }
            String baseUrl = "";
            /**判断类是否被LhRequestMapping注解了，如果是，则获取类型的url*/
            if (object.getClass().isAnnotationPresent(LhRequestMapping.class)) {
                LhRequestMapping lhRequestMapping = object.getClass().getAnnotation(LhRequestMapping.class);
                baseUrl = lhRequestMapping.value();

            }
            /**循环类里面的方法，不包括私有方法*/
            for (Method method : entry.getValue().getClass().getMethods()) {
                /**判断方法是否被LhRequestMapping 注解了，如果是，则获取配置的url*/
                if (method.isAnnotationPresent(LhRequestMapping.class)) {
                    LhRequestMapping lhRequestMapping = method.getAnnotation(LhRequestMapping.class);
                    String url = ("/" + baseUrl + "/" + lhRequestMapping.value()).replaceAll("/+", "/");
                    /**配置一个正则表达式，后面用来匹配url*/
                    Pattern pattern = Pattern.compile(url);
                    /**new 一个handlerMapping 存储方法，方法所属的类，以及url的正则*/
                    HandlerMapping handlerMapping = new HandlerMapping(pattern, method, object);
                    handlerMappings.add(handlerMapping);
                    System.out.println("Mapping:" + url + "," + method);
                }
            }
        }
    }

    /**
    * @Author liang.hao
    * @Description 首字母小写
    * @Date 21:05 2019/7/21
    * @Param [className]
    * @return java.lang.String
    **/
    private String toLowerFirstCase(String className) {
        char[] chars = className.toCharArray();
        /**在Java ，直接char+32 可以直接将大写字母转换成小写字母*/
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
    * @Author liang.hao
    * @Description 类的实例化填充IOC容器
    * @Date 21:07 2019/7/21
    * @Param []
    * @return void
    **/
    private void doInstance() {

        if (clasNameList.isEmpty()) {
            return;
        }
        for (String className : clasNameList) {

            try {
                /**通过类路径，放射获取类*/
                Class<?> clazz = Class.forName(className);
                /**如果是controller 直接放进IOC容易，通过反射获取实例*/
                if (clazz.isAnnotationPresent(LhController.class)) {
                    ioc.put(toLowerFirstCase(clazz.getSimpleName()), clazz.newInstance());
                } else if (clazz.isAnnotationPresent(LhService.class)) {
                    /**Service一般是接口，无法实例化，只能实例化实现类，所以需要多次判断*/
                    /**1、自定义beanName*/
                    LhService lhService = clazz.getAnnotation(LhService.class);
                    String beanName = lhService.value();
                    /**2、如果没有自定义name*/
                    if ("".equals(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object object = clazz.newInstance();
                    ioc.put(beanName, object);
                    /**3、根据类型自动注入，投机取巧的方式*/
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("这个名称" + i.getName() + "已经存在");
                        }
                        ioc.put(i.getName(), object);
                    }
                } else {
                    continue;
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
    * @Author liang.hao
    * @Description 扫面配置报下面的所有类名称
    * @Date 21:10 2019/7/21
    * @Param [scannerPath]
    * @return void
    **/
    private void doScanner(String scannerPath) {
        /**根据类路径获取对应的URL*/
        URL url = this.getClass().getClassLoader().getResource("/" + scannerPath.replaceAll("\\.", "/"));
        File classPath = new File(url.getPath());
        /**遍历路径下所有文件*/
        for (File file : classPath.listFiles()) {
            /**如果是文件夹则，递归调用*/
            if (file.isDirectory()) {
                doScanner(scannerPath + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                } else {
                    /**文件以.class结尾，则放进List中*/
                    clasNameList.add(scannerPath + "." + file.getName().replace(".class", ""));
                }
            }
        }


    }
/**
* @Author liang.hao
* @Description 读取配置的包路径
* @Date 21:12 2019/7/21
* @Param [contextConfigLocation]
* @return void
**/
    private void doConfigLoad(String contextConfigLocation) {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class HandlerMapping {
        private Pattern url;
        private Method method;
        private Object controller;

        private Map<String, Integer> paramerIndexMap;

        public HandlerMapping(Pattern url, Method method, Object controller) {
            this.url = url;
            this.method = method;
            this.controller = controller;
            this.paramerIndexMap = new HashMap<>();
            putParamerIndexMap(method);
        }

        /**
        * @Author liang.hao
        * @Description 获取方法中，参数名称，以及相应位置
        * @Date 21:13 2019/7/21
        * @Param [method]
        * @return void
        **/
        private void putParamerIndexMap(Method method) {
            /**获取方法中所有参数的注解，是个二维数组，因为一个方法有多个参数，一个参数又可以有多个注解*/
            Annotation[][] pa = method.getParameterAnnotations();
            for (int j = 0; j < pa.length; j++) {
                for (Annotation annotation : pa[j]) {
                    /**判断参数中是否被LhRequestParam注解了，如果是，获取配置的名称，以及对应的位置，放到map中，后面留着用*/
                    if (annotation instanceof LhRequestParam) {
                        String paramName = ((LhRequestParam) annotation).value();
                        this.paramerIndexMap.put(paramName, j);
                    }
                }
            }
            /**获取形参类型数组*/
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                Class clazz = paramTypes[i];
                /**判断参数中是否有HttpServletResponse、HttpServletRequest ，如果有，也将他们的位置记录备用*/
                if (clazz == HttpServletResponse.class || clazz == HttpServletRequest.class) {
                    this.paramerIndexMap.put(clazz.getName(), i);
                }
            }
        }
    }
}
