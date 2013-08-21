package com.example.TransmitWifi;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import android.util.Log;

/**
 * http 服务抽象类
 * @author haihui.li
 * @version 1.0.0
 */
public abstract class HTTPServerDaemon {
    private static final String TAG = "HTTPServerDaemon";
    /**
     * mime type plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";
    /**
     * mime type  html
     */
    public static final String MIME_HTML = "text/html";
    /**
     * mime type  binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    private final InetAddress mINetAddress;
    private final int mPort;
    private ServerSocket mServerSocket;
    private Thread mListenThread;
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
    /**
     *构造函数
     *@param ia Host internet address
     *@param port Host port to listen
     */
    public HTTPServerDaemon(InetAddress ia, int port) {
        this.mINetAddress = ia;
        this.mPort = port;
    }


    private void doWithClientConnect(final InputStream inputStream, final Socket finalAccept) {
        Thread task = new Thread() {
            @Override
            public void run() {
                OutputStream outputStream = null;
                try {
                    outputStream = finalAccept.getOutputStream();
                    HTTPSession session = new HTTPSession(inputStream, outputStream);
                    Log.i(TAG, "new a session for the http server");
                    while (!finalAccept.isClosed()) {
                        session.execute();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    NetWorkUtils.safeClose(outputStream);
                    NetWorkUtils.safeClose(inputStream);
                    NetWorkUtils.safeClose(finalAccept);
                }
            }
        };
        task.setName("HTTPServerDaemon Request Processor");
        task.start();
    }

    /**
     * 启动http server
     * @throws IOException if the socket is in use.
     */
    public void start() throws IOException {
        mServerSocket = new ServerSocket();
        mServerSocket.bind(new InetSocketAddress(mINetAddress, mPort));

        mListenThread = new Thread() {
            @Override
            public void run() {
                do {
                    try {
                        Log.i(TAG, "wait for the connection accept ");
                        final Socket finalAccept = mServerSocket.accept();
                        Log.i(TAG, "get a connection ");
                        final InputStream inputStream = finalAccept.getInputStream();

                        if (inputStream == null) {
                            NetWorkUtils.safeClose(finalAccept);
                        } else {
                            doWithClientConnect(inputStream, finalAccept);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } while (!mServerSocket.isClosed());
            }
        };
        mListenThread.setName("NanoHttpd Main Listener");
        mListenThread.start();
    }

    /**
     * 停止http server
     */
    public void stop() {
        try {
            NetWorkUtils.safeClose(mServerSocket);
            mListenThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 定制相关功能的重载函数
     * @param uri    Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method "GET", "POST" etc.
     * @param parms  Parsed, percent decoded parameters from URI and, in case of POST, data.
     * @param headers Header entries, percent decoded
     * @param files put upload file list
     * @return HTTP response, see class Response for details
     */
    public abstract Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms,
                                   Map<String, String> files);

    protected Response serve(HTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> parms = session.getParms();
        Map<String, String> headers = session.getHeaders();
        return serve(uri, method, headers, parms, null);
    }

    protected String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
            Log.i(TAG, "decode percent failed");
        }
        return decoded;
    }

    /**
     * 解析URL里面的参数
     * @param parms original string
     * @return a map of  (parameter name)
     */
    protected Map<String, List<String>> decodeParameters(Map<String, String> parms) {
        return this.decodeParameters(parms.get(QUERY_STRING_PARAMETER));
    }

    /**
     * 解析URL里面的参数
     * @param queryString a query string pulled from the URL.
     * @return a map of (parameter name) 
     */
    protected Map<String, List<String>> decodeParameters(String queryString) {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = (sep >= 0) ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName)) {
                    parms.put(propertyName, new ArrayList<String>());
                }
                String propertyValue = (sep >= 0) ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null) {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }

    /**
     *http 请求的方法 
     */
    public enum Method {
        /**
         * Get method
         */
        GET,
        /**
         * Put method
         */
        PUT, POST, DELETE, HEAD;

        static Method lookup(String method) {
            for (Method m : Method.values()) {
                if (m.toString().equalsIgnoreCase(method)) {
                    return m;
                }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------------- //

    /**
     * HTTP response. 
     */
    public static class Response {
        /**
         * HTTP status code after processing, e.g. "200 OK", HTTP_OK
         */
        private Status mStatus;
        /**
         * MIME type of content, e.g. "text/html"
         */
        private String mMimeType;
        /**
         * Data of the response, may be null.
         */
        private InputStream mData;
        /**
         * Headers for the HTTP response. Use addHeader() to add lines.
         */
        private Map<String, String> mHeader = new HashMap<String, String>();
        /**
         * The request method that spawned this response.
         */
        private Method mRequestMethod;
        /**
         * 构造函数: response = HTTP_OK, mime = MIME_HTML and your supplied message
         * @param msg The message will return to the client
         */
        public Response(String msg) {
            this(Status.OK, MIME_HTML, msg);
        }

        /**
         * 构造函数
         * @param status Status of response
         * @param mimeType the type of input stream
         * @param data the content input
         */
        public Response(Status status, String mimeType, InputStream data) {
            this.mStatus = status;
            this.mMimeType = mimeType;
            this.mData = data;
        }

        /**
         * 构造函数 
         * @param status The status of response
         * @param mimeType the type of content
         * @param txt the content
         */
        public Response(Status status, String mimeType, String txt) {
            this.mStatus = status;
            this.mMimeType = mimeType;
            try {
                this.mData = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
            } catch (java.io.UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }
        }

        /**
         * 回复中增加参数
         * @param name name of parameter
         * @param value value of parameter
         */
        public void addHeader(String name, String value) {
            mHeader.put(name, value);
        }

        private void send(OutputStream outputStream) {
            String mime = mMimeType;
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                if (mStatus == null) {
                    throw new Error("sendResponse(): Status can't be null.");
                }
                PrintWriter pw = new PrintWriter(outputStream);
                pw.print("HTTP/1.1 " + mStatus.getDescription() + " \r\n");

                if (mime != null) {
                    pw.print("Content-Type: " + mime + "\r\n");
                }

                if (mHeader == null || mHeader.get("Date") == null) {
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                }

                if (mHeader != null) {
                    for (String key : mHeader.keySet()) {
                        String value = mHeader.get(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                int pending = mData != null ? mData.available() : -1; 
                // This is to support partial sends, see serveFile()
                Log.i(TAG, "pending :" + pending);
                if (pending > 0) {
                    pw.print("Connection: keep-alive\r\n");
                    pw.print("Content-Length: " + pending + "\r\n");
                }

                pw.print("\r\n");
                pw.flush();

                if (mRequestMethod != Method.HEAD && mData != null) {
                    final int bufferSize = 16384; //16K
                    byte[] buff = new byte[bufferSize];
                    while (pending > 0) {
                        Log.i(TAG, "read begin");
                        int read = mData.read(buff, 0, ((pending > bufferSize) ? bufferSize : pending));
                        Log.i(TAG, "read end");
                        Log.i(TAG, "read =" + read);
                        if (read <= 0) {
                            break;
                        }
                        outputStream.write(buff, 0, read);

                        pending -= read;
                    }
                }
                outputStream.flush();
                NetWorkUtils.safeClose(mData);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                // Couldn't write? No can do.
            }
        }

        /**
         * 获得返回状态 
         * @return the status of response
         */
        public Status getStatus() {
            return mStatus;
        }

        /**
         * 设置返回状态
         * @param status status of response
         */
        public void setStatus(Status status) {
            this.mStatus = status;
        }

        /**
         * 获得mime type of content
         * @return mine type
         */

        public String getMimeType() {
            return mMimeType;
        }

        /**
         *设置mine type of content
         * @param mimeType mine type as string
         */
        public void setMimeType(String mimeType) {
            this.mMimeType = mimeType;
        }

        /**
         * content
         * @return input stream of content
         */
        public InputStream getData() {
            return mData;
        }

        /**
         * set content for response
         * @param data input stream of content
         */
        public void setData(InputStream data) {
            this.mData = data;
        }

        /**
         * 获得请求的方法
         * @return method of request
         */
        public Method getRequestMethod() {
            return mRequestMethod;
        }

        /**
         * 设置请求的方法
         * @param requestMethod method of request
         */
        public void setRequestMethod(Method requestMethod) {
            this.mRequestMethod = requestMethod;
        }

        /**
         *http 回复的状态 
         */
        public enum Status {
            /**
             *normal status
             */
            OK(200, "OK"),
            /**
             *created status of http
             */
            CREATED(201, "Created"),
            /**
             *accepted status of http
             */
            ACCEPTED(202, "Accepted"),
            /**
             * status of http
             */
            NO_CONTENT(204, "No Content"),
            /**
             * status of http
             */
            PARTIAL_CONTENT(206, "Partial Content"),
            /**
             * status of http
             */
            REDIRECT(301, "Moved Permanently"),
            /**
             * status of http
             */
            NOT_MODIFIED(304, "Not Modified"),
            /**
             * status of http
             */
            BAD_REQUEST(400, "Bad Request"),
            /**
             * status of http
             */
            UNAUTHORIZED(401, "Unauthorized"),
            /**
             * status of http
             */
            FORBIDDEN(403, "Forbidden"),
            /**
             * status of http
             */
            NOT_FOUND(404, "Not Found"),
            /**
             * status of http
             */
            RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
            /**
             * status of http
             */
            INTERNAL_ERROR(500, "Internal Server Error");
            private final int mRequestStatus;
            private final String mDescription;

            Status(int requestStatus, String description) {
                this.mRequestStatus = requestStatus;
                this.mDescription = description;
            }

            /**
             * request status
             * @return request status
             */
            public int getRequestStatus() {
                return this.mRequestStatus;
            }

            /**
             * Description for the status
             * @return Description
             */
            public String getDescription() {
                return "" + this.mRequestStatus + " " + mDescription;
            }
        }
    }

    /**
     * 处理http相关请求 i.e. parses the HTTP request and returns the response.
     */
    protected class HTTPSession {
        private static final int BUFSIZE = 8192;

        private InputStream mInputStream;
        private final OutputStream mOutputStream;

        private int mSplitByte;
        private int mReadLength;

        private String mUri;
        private Method mMethod;
        private Map<String, String> mParameters;
        private Map<String, String> mHeaders;

        public HTTPSession(InputStream inputStream, OutputStream outputStream) {
            this.mInputStream = inputStream;
            this.mOutputStream = outputStream;
        }

        public void execute() throws IOException {
            try {
                // Read the first 8192 bytes.
                // The full header should fit in here.
                // Apache's default header limit is 8KB.
                // Do NOT assume that a single read will get the entire header at once!
                byte[] buf = new byte[BUFSIZE];
                mSplitByte = 0;
                mReadLength = 0;
                int read = mInputStream.read(buf, 0, BUFSIZE);

                if (read == -1) {
                    // socket was been closed
                    throw new SocketException();
                }

                while (read > 0) {
                    mReadLength += read;
                    mSplitByte = findHeaderEnd(buf, mReadLength);
                    if (mSplitByte >  0) {
                        break;
                    }
                    read = mInputStream.read(buf, mReadLength, BUFSIZE - mReadLength);
                }

                if (mSplitByte < mReadLength) {
                    ByteArrayInputStream splitInputStream = new ByteArrayInputStream(buf, mSplitByte, mReadLength - mSplitByte);
                    SequenceInputStream sequenceInputStream = new SequenceInputStream(splitInputStream, mInputStream);
                    mInputStream = sequenceInputStream;
                }
                mParameters = new HashMap<String, String>();
                mHeaders = new HashMap<String, String>();

                BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, mReadLength)));

                Map<String, String> pre = new HashMap<String, String>();
                decodeHeader(hin, pre, mParameters, mHeaders);
                /**
                 *Get client request method
                 */
                mMethod = Method.lookup(pre.get("method"));

                if (mMethod == null) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error.");
                }
                /**
                 *Get client request file
                 */
                mUri = pre.get("uri");
                // Ok, now do the serve()
                Response r = serve(this);

                if (r == null) {
                    throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    r.setRequestMethod(mMethod);
                    r.send(mOutputStream);
                }
            } catch (SocketException e) {
                // throw it out to close socket object (finalAccept)
                throw e;
            } catch (IOException ioe) {
                Response r = new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                r.send(mOutputStream);
                NetWorkUtils.safeClose(mOutputStream);
            } catch (ResponseException re) {
                Response r = new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                r.send(mOutputStream);
                NetWorkUtils.safeClose(mOutputStream);
            } finally {
                Log.i(TAG, "##################finally##############");
            }
        }

        private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers)
                throws ResponseException {
            try {
                // Read the request line
                String inLine = in.readLine();
                if (inLine == null) {
                    return;
                }

                StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }

                pre.put("method", st.nextToken());

                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                }

                String uri = st.nextToken();

                // Decode parameters from the URI
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParams(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                } else {
                    uri = decodePercent(uri);
                }

                // If there's another token, it's protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                // NOTE: this now forces header names lowercase since they are
                // case insensitive and vary by client.
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while (line != null && line.trim().length() > 0) {
                        int p = line.indexOf(':');
                        if (p >= 0) {
                            headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                        }
                        line = in.readLine();
                    }
                }

                pre.put("uri", uri);
            } catch (IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }

        private int findHeaderEnd(final byte[] buf, int rlen) {
            int splitbyte = 0;
            while (splitbyte + 3 < rlen) {
                if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                    return splitbyte + 4;
                }
                splitbyte++;
            }
            return 0;
        }

        private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
            int matchcount = 0;
            int matchbyte = -1;
            List<Integer> matchbytes = new ArrayList<Integer>();
            for (int i = 0; i < b.limit(); i++) {
                if (b.get(i) == boundary[matchcount]) {
                    if (matchcount == 0) {
                        matchbyte = i;
                    }
                    matchcount++;
                    if (matchcount == boundary.length) {
                        matchbytes.add(matchbyte);
                        matchcount = 0;
                        matchbyte = -1;
                    }
                } else {
                    i -= matchcount;
                    matchcount = 0;
                    matchbyte = -1;
                }
            }
            int[] ret = new int[matchbytes.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = matchbytes.get(i);
            }
            return ret;
        }

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
         * adds them to given Map. NOTE: this doesn't support multiple identical keys due to the simplicity of Map.
         */
        private void decodeParams(String param, Map<String, String> p) {
            if (param == null) {
                p.put(QUERY_STRING_PARAMETER, "");
                return;
            }

            p.put(QUERY_STRING_PARAMETER, param);
            StringTokenizer st = new StringTokenizer(param, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(decodePercent(e.substring(0, sep)).trim(),
                            decodePercent(e.substring(sep + 1)));
                } else {
                    p.put(decodePercent(e).trim(), "");
                }
            }
        }

        public final Map<String, String> getParms() {
            return mParameters;
        }

        public final Map<String, String> getHeaders() {
            return mHeaders;
        }

        public final String getUri() {
            return mUri;
        }

        public final Method getMethod() {
            return mMethod;
        }

        public final InputStream getInputStream() {
            return mInputStream;
        }
    }

    private static final class ResponseException extends Exception {

        private final Response.Status mStatus;

        public ResponseException(Response.Status status, String message) {
            super(message);
            this.mStatus = status;
        }

        public ResponseException(Response.Status status, String message, Exception e) {
            super(message, e);
            this.mStatus = status;
        }

        public Response.Status getStatus() {
            return mStatus;
        }
    }

    /**
     * 获得监听端口 
     * @return the port of listen
     */
    public final int getListeningPort() {
        return mServerSocket == null ? -1 : mServerSocket.getLocalPort();
    }

    /**
     * 获得http服务是否开启
     * @return true is started and reverse
     */

    public final boolean wasStarted() {
        return mServerSocket != null && mListenThread != null;
    }

    /**
     * 检查服务是否正常
     * @return the same as above
     */
    public final boolean isAlive() {
        return wasStarted() && !mServerSocket.isClosed() && mListenThread.isAlive();
    }
}
