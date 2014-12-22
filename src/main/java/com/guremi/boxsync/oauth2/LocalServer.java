package com.guremi.boxsync.oauth2;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalServer {
    private static final Logger LOG = LoggerFactory.getLogger(LocalServer.class);

    private Server server;
    private String code;
    private String error;
    private final Lock lock = new ReentrantLock();
    private final Condition authoriseResponse = lock.newCondition();
    private static final String CALLBACK_PATH = "/callback";

    private int port;
    private String host;

    public LocalServer() {
        this("localhost", -1);
    }

    public LocalServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getRedirectUri() throws Exception {
        if (port == -1) {
            port = getUnusedPort();
        }
        server = new Server();
        ServerConnector sc = new ServerConnector(server);
        sc.setHost(host);
        sc.setPort(port);
        server.addConnector(sc);
        server.setHandler(new CallbackHander());

        server.start();
        return "http://" + host + ":" + port + CALLBACK_PATH;
    }

    public String waitForCode() throws IOException {
        lock.lock();
        try {
            while(code == null && error == null) {
                LOG.info("wait..");
                authoriseResponse.awaitUninterruptibly();
            }
            if (error != null) {
                throw new IOException("User authorization failed.(" + error + ")");
            }
            return code;
        } finally {
            lock.unlock();
        }
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private int getUnusedPort() throws IOException {
        Socket s = new Socket();
        s.bind(null);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }

    public int getPort() {
        return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
    }

    class CallbackHander extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (!CALLBACK_PATH.equals(target)) {
                return;
            }
            writeResponseHtml(response);
            response.flushBuffer();
            ((Request)request).setHandled(true);
            lock.lock();
            try {
                error = request.getParameter("error");
                code = request.getParameter("code");
                LOG.info("code:{}, error:{}", code, error);
                authoriseResponse.signal();
            } finally {
                lock.unlock();
            }
        }

        private void writeResponseHtml(HttpServletResponse response) throws IOException {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/html");

            PrintWriter doc = response.getWriter();
            doc.println("<html>");
            doc.println("<head><title>OAuth 2.0 Authentication Token Recieved</title></head>");
            doc.println("<body>");
            doc.println("Received verification code. Closing...");
            doc.println("<script type='text/javascript'>");
            doc.println("window.setTimeout(function() {");
            doc.println("    window.open('', '_self', ''); window.close(); }, 1000);");
            doc.println("if (window.opener) { window.opener.checkToken(); }");
            doc.println("</script>");
            doc.println("</body>");
            doc.println("</HTML>");
            doc.flush();
        }
    }
}
