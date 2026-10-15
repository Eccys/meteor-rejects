package anticope.rejects.utils.portscanner;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PortScannerManager {
    public static List<PScanRunner> scans = new CopyOnWriteArrayList<>();

    public static void killAllScans() {
        for (PScanRunner runner : scans) {
            if (runner != null)
                runner.cancel();
        }
        scans.clear();
    }

    public static class ScanResult {
        private int port;

        private boolean isOpen;

        public ScanResult(int port, boolean isOpen) {
            super();
            this.port = port;
            this.isOpen = isOpen;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public void setOpen(boolean isOpen) {
            this.isOpen = isOpen;
        }

    }
}
