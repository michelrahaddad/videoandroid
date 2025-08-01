package com.generalplus.GoPlusDrone;
import com.generalplus.GoPlusDrone.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Created by willywen on 2017/7/25.
 */
public class Utils {

    public static byte[] int2byte(int[]src) {
        int srcLength = src.length;
        byte[]dst = new byte[srcLength << 2];

        for (int i=0; i<srcLength; i++) {
            int x = src[i];
            int j = i << 2;
            dst[j++] = (byte) ((x >>> 0) & 0xff);
            dst[j++] = (byte) ((x >>> 8) & 0xff);
            dst[j++] = (byte) ((x >>> 16) & 0xff);
            dst[j++] = (byte) ((x >>> 24) & 0xff);
        }
        return dst;
    }
        /**
         * Convert byte array to hex string
         * @param bytes
         * @return
         */
        public static String bytesToHex(byte[] bytes) {
            StringBuilder sbuf = new StringBuilder();
            for(int idx=0; idx < bytes.length; idx++) {
                int intVal = bytes[idx] & 0xff;
                if (intVal < 0x10) sbuf.append("0");
                sbuf.append(Integer.toHexString(intVal).toUpperCase());
            }
            return sbuf.toString();
        }

        /**
         * Get utf8 byte array.
         * @param str
         * @return  array of NULL if error was found
         */
        public static byte[] getUTF8Bytes(String str) {
            try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
        }

        /**
         * Load UTF8withBOM or any ansi text file.
         * @param filename
         * @return
         * @throws IOException
         */
        public static String loadFileAsString(String filename) throws IOException {
            final int BUFLEN=1024;
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
                byte[] bytes = new byte[BUFLEN];
                boolean isUTF8=false;
                int read,count=0;
                while((read=is.read(bytes)) != -1) {
                    if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
                        isUTF8=true;
                        baos.write(bytes, 3, read-3); // drop UTF8 bom marker
                    } else {
                        baos.write(bytes, 0, read);
                    }
                    count+=read;
                }
                return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
            } finally {
                try{ is.close(); } catch(Exception ex){}
            }
        }

        /**
         * Returns MAC address of the given interface name.
         * @param interfaceName eth0, wlan0 or NULL=use first interface
         * @return  mac address or empty string
         */
        public static String getMACAddress(String interfaceName) {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    if (interfaceName != null) {
                        if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                    }
                    byte[] mac = intf.getHardwareAddress();
                    if (mac==null) return "";
                    StringBuilder buf = new StringBuilder();
                    for (int idx=0; idx<mac.length; idx++)
                        buf.append(String.format("%02X:", mac[idx]));
                    if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                    return buf.toString();
                }
            } catch (Exception ex) { } // for now eat exceptions
            return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
        }

        /**
         * Get IP address from first non-localhost interface
         * @param ipv4  true=return ipv4, false=return ipv6
         * @return  address or empty string
         */
        public static String getIPAddress(boolean useIPv4) {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress()) {
                            String sAddr = addr.getHostAddress();
                            //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                            boolean isIPv4 = sAddr.indexOf(':')<0;

                            if (useIPv4) {
                                if (isIPv4)
                                    return sAddr;
                            } else {
                                if (!isIPv4) {
                                    int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                    return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) { } // for now eat exceptions
            return "";
        }
    public static int  parseAddressToHex(String address) {

        int result = 0;
        String[] str = address.split("\\.");
        for (int i = 0; i < str.length; i++) {
            int j = Integer.parseInt(str[i]);
            result = result << 8 | (j & 0xFF);
        }
        return result;
    }
}
