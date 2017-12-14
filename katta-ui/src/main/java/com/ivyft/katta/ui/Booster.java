package com.ivyft.katta.ui;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.ivyft.katta.ui.annaotion.Action;
import com.ivyft.katta.ui.annaotion.Path;
import com.ivyft.katta.ui.handle.DynamicRequestServlet;
import com.ivyft.katta.ui.utils.ClasspathPackageScanner;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * <pre>
 *
 * Created by zhenqin.
 * User: zhenqin
 * Date: 17/12/13
 * Time: 14:47
 * Vendor: yiidata.com
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class Booster {



    Server srv = null;


    public String getName() {
        return "katta-ui";
    }


    public void start() {
        Preconditions.checkState(srv == null,
                "Running HTTP Server found in source: " + getName()
                        + " before I started one."
                        + "Will not attempt to start.");
        srv = new Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setReuseAddress(true);

        try {
            // 静态文件
            ResourceHandler resourceHandler = new ResourceHandler();  //静态资源处理的handler
            resourceHandler.setWelcomeFiles(new String[]{"index.html"});
            resourceHandler.setResourceBase("src/main/resources/static");


            LOG.info("add rule: /static");
            ContextHandler contextHandler = new ContextHandler();
            contextHandler.setContextPath("/static");
            contextHandler.setHandler(resourceHandler);
            srv.addHandler(contextHandler);


            ServletHandler handler = new ServletHandler();
            LOG.info("add rule: /");
            ClasspathPackageScanner scanner = new ClasspathPackageScanner(this.getClass().getPackage().getName());
            List<String> nameList = scanner.getFullyQualifiedClassNameList();
            for (String s : nameList) {
                Class<?> aClass = Class.forName(s);
                Action annotation = aClass.getAnnotation(Action.class);
                if(annotation == null) {
                    continue;
                }

                // 是 Action 类，需要增加
                Object instance = aClass.newInstance();
                Method[] methods = aClass.getDeclaredMethods();
                for (Method method : methods) {
                    Path annotation1 = method.getAnnotation(Path.class);
                    if(annotation1 == null) {
                        continue;
                    }

                    // 有 Path 注解
                    String[] values = annotation1.value();
                    for (String value : values) {
                        handler.addServletWithMapping(new ServletHolder(new DynamicRequestServlet(instance, method)), value);
                        LOG.info("add path: " + value);
                    }
                }
            }


            // 上传压缩包
            ContextHandler uploadHandler = new ContextHandler();
            uploadHandler.setContextPath("/");
            uploadHandler.setHandler(handler);
            srv.addHandler(uploadHandler);

            connector.setHost("0.0.0.0");
            connector.setPort(8080);
            srv.addConnector(connector);

            srv.start();
        } catch (Exception ex) {
            LOG.error("Error while starting HTTPSource. Exception follows.", ex);
            Throwables.propagate(ex);
        }
        Preconditions.checkArgument(srv.isRunning());
    }

    public void stop() {
        try {
            srv.stop();
            srv.join();
            srv = null;
        } catch (Exception ex) {
            LOG.error("Error while stopping HTTPSource. Exception follows.", ex);
        }
        LOG.info("Http source {} stopped. ", getName());
    }


    static Logger LOG = LoggerFactory.getLogger(Booster.class);

    public static void main(String[] args) {
        Booster booster = new Booster();
        booster.start();
        try {
            booster.serve();
        } catch (InterruptedException e) {
            e.printStackTrace();

            booster.stop();
        }
    }

    private void serve() throws InterruptedException {
        srv.join();
    }
}