package anticope.rejects.utils.portscanner;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PScanRunner {
    public volatile boolean running = true;
    public volatile int portsScanned = 0;
    private final Object scanLock = new Object();
    final ExecutorService es;
    final List<Future<PortScannerManager.ScanResult>> futures = new ArrayList<>();
    final Thread runner;

    public PScanRunner(InetAddress address, int threads, int threadDelay, int timeoutMS, Collection<Integer> ports,
                       Consumer<List<PortScannerManager.ScanResult>> callback) {
        es = Executors.newFixedThreadPool(Math.max(1, threads));
        runner = new Thread(() -> {
            try {
                for (Integer port : ports) {
                    if (!running) break;
                    futures.add(isPortOpen(es, address.getHostAddress(), port, timeoutMS, threadDelay));
                }
                es.shutdown();
                try {
                    es.awaitTermination(200L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                List<PortScannerManager.ScanResult> results = new ArrayList<>();
                for (Future<PortScannerManager.ScanResult> fsc : futures) {
                    if (!running) break;
                    try {
                        results.add(fsc.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (CancellationException e) {
                        if (!running) break;
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                if (running && callback != null) {
                    callback.accept(results);
                }
            } finally {
                running = false;
                es.shutdownNow();
            }
        });
        runner.start();
    }

    public void cancel() {
        running = false;
        if (es != null) es.shutdownNow();
        if (runner != null) runner.interrupt();
    }

    private Future<PortScannerManager.ScanResult> isPortOpen(ExecutorService es, String ip, int port, int timeout,
                                                             int delay) {
        return es.submit(() -> {
            if (!running)
                return new PortScannerManager.ScanResult(port, false);
            Thread.sleep(delay);
            synchronized (scanLock) {
                portsScanned++;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), timeout);
                return new PortScannerManager.ScanResult(port, true);
            } catch (Exception exc) {
                return new PortScannerManager.ScanResult(port, false);
            }
        });
    }
}
