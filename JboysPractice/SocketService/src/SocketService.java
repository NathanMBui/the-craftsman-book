import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class SocketService {
    private ServerSocket serverSocket = null;
    private Thread serverThread = null;
    private boolean running = false;
    private SocketServer itsServer;
    private List<Thread> serverThreads = Collections.synchronizedList(new LinkedList<>());

    public void serve(int port, SocketServer server) throws  Exception {
        itsServer = server;
        serverSocket = new ServerSocket(port);
        serverThread = makeServerThread();
        serverThread.start();
    }

    public static PrintStream getPrintStream(Socket socket) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        return new PrintStream(outputStream);
    }

    public static BufferedReader getBufferedReader(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        return new BufferedReader(inputStreamReader);
    }

    private Thread makeServerThread() {
        return new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        running = true;
                        while (running) {
                            acceptAndServeConnection();
                        }
                    }
                }
        );
    }

    private void acceptAndServeConnection() {
        try {
            Socket socket = serverSocket.accept();
            Thread serverThread = new Thread(new ServiceRunnable(socket));
            serverThreads.add(serverThread);
            serverThread.start();
        } catch (IOException ex) {
        }
    }

    public void close() throws Exception {
        if (running) {
            running = false;
            serverSocket.close();

            serverThread.join();
            while (serverThreads.size() > 0) {
                Thread thread = serverThreads.get(0);
                serverThreads.remove(thread);
                thread.join();
            }
        } else {
            serverSocket.close();
        }
    }

    private class ServiceRunnable implements Runnable {
        private Socket itsSocket;

        public ServiceRunnable(Socket socket) {
            this.itsSocket = socket;
        }

        @Override
        public void run() {
            try {
                itsServer.serve(itsSocket);
                serverThreads.remove(Thread.currentThread());
                itsSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
