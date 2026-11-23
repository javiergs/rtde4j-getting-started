package rover;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class RtdeClient {

    // ---------------- PROTOCOL ----------------
    static final int REQUEST_PROTOCOL_VERSION = 'V';
    static final int GET_URCONTROL_VERSION = 'v';
    static final int TEXT_MESSAGE = 'M';
    static final int DATA_PACKAGE = 'U';
    static final int CONTROL_PACKAGE_SETUP_OUTPUTS = 'O';
    static final int CONTROL_PACKAGE_START = 'S';
    static final int PROTOCOL_VERSION = 2;
    static final int TIMEOUT_MS = 1000;
    static final double FREQUENCY = 125;
    static final int INFO_MESSAGE = 3;

    private final String host;
    private final int port;
    private Socket sock;
    private DataInputStream in;
    private DataOutputStream out;
    private List<String> outputNames = new ArrayList<>();
    private List<String> outputTypes = new ArrayList<>();

    public RtdeClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ---------------- CONFIG FILE ----------------
    //reads one recipe from rtde_config.xml as [names, types], same file format as the python rtde_config
    public static List<List<String>> readRecipe(String path, String key) throws Exception {
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path)).getDocumentElement();
        NodeList recipes = root.getElementsByTagName("recipe");
        for (int i = 0; i < recipes.getLength(); i++) {
            Element recipe = (Element) recipes.item(i);
            if (!recipe.getAttribute("key").equals(key)) continue;
            List<String> names = new ArrayList<>();
            List<String> types = new ArrayList<>();
            NodeList fields = recipe.getElementsByTagName("field");
            for (int j = 0; j < fields.getLength(); j++) {
                Element field = (Element) fields.item(j);
                names.add(field.getAttribute("name"));
                types.add(field.getAttribute("type"));
            }
            return List.of(names, types);
        }
        throw new IllegalArgumentException("recipe '" + key + "' not found in " + path);
    }

    // ---------------- CONNECTION ----------------
    public void connect() throws IOException {
        sock = new Socket();
        sock.setTcpNoDelay(true);
        sock.setSoTimeout(TIMEOUT_MS);
        sock.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
        in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
        out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));

        byte[] reply = sendAndReceive(REQUEST_PROTOCOL_VERSION, ByteBuffer.allocate(2).putShort((short) PROTOCOL_VERSION).array());
        if (reply[0] == 0) throw new IOException("Unable to negotiate protocol version");
    }

    public void disconnect() {
        try {
            if (sock != null) sock.close();
        } catch (IOException ignored) {
        }
        sock = null;
    }

    public void getControllerVersion() throws IOException {
        sendAndReceive(GET_URCONTROL_VERSION, new byte[0]);
    }

    public void sendOutputSetup(List<String> names, List<String> types) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(ByteBuffer.allocate(8).putDouble(FREQUENCY).array());
        payload.write(String.join(",", names).getBytes(StandardCharsets.UTF_8));
        byte[] reply = sendAndReceive(CONTROL_PACKAGE_SETUP_OUTPUTS, payload.toByteArray());

        //reply is [recipe id][comma separated types]; the robot answers NOT_FOUND for unknown fields
        List<String> robotTypes = List.of(new String(reply, 1, reply.length - 1, StandardCharsets.UTF_8).split(","));
        if (!robotTypes.equals(types)) {
            throw new IOException("Data type inconsistency for output setup: " + types + " - " + robotTypes);
        }
        outputNames = names;
        outputTypes = types;
    }

    public void sendStart() throws IOException {
        byte[] reply = sendAndReceive(CONTROL_PACKAGE_START, new byte[0]);
        if (reply[0] == 0) throw new IOException("RTDE synchronization failed to start");
    }

    // ---------------- RECEIVE ----------------
    //returns the newest data package as name -> values (scalars are length 1), or null if no data arrived within the timeout
    public Map<String, double[]> receive() throws IOException {
        byte[] latest = null;
        try {
            do {
                int[] header = new int[1];
                byte[] payload = readPacket(header);
                if (header[0] == DATA_PACKAGE) latest = payload;
                //keeps reading while more packages are buffered so the loop always acts on the newest state
            } while (latest == null || in.available() > 0);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (EOFException e) {
            throw new IOException("received 0 bytes from Controller");
        }
        return unpack(latest);
    }

    private Map<String, double[]> unpack(byte[] payload) {
        ByteBuffer buf = ByteBuffer.wrap(payload, 1, payload.length - 1);
        Map<String, double[]> state = new HashMap<>();
        for (int i = 0; i < outputNames.size(); i++) {
            String type = outputTypes.get(i);
            int size = type.startsWith("VECTOR6") ? 6 : type.startsWith("VECTOR3") ? 3 : 1;
            double[] values = new double[size];
            for (int k = 0; k < size; k++) {
                switch (type) {
                    case "DOUBLE", "VECTOR3D", "VECTOR6D" -> values[k] = buf.getDouble();
                    case "UINT64" -> values[k] = buf.getLong();
                    case "UINT32", "VECTOR6UINT32" -> values[k] = buf.getInt() & 0xFFFFFFFFL;
                    case "INT32", "VECTOR6INT32" -> values[k] = buf.getInt();
                    case "UINT8", "BOOL" -> values[k] = buf.get() & 0xFF;
                    default -> throw new IllegalArgumentException("unpack_field: unknown data type: " + type);
                }
            }
            state.put(outputNames.get(i), values);
        }
        return state;
    }

    // ---------------- PACKETS ----------------
    //every packet is [uint16 size incl. header][uint8 command][payload], big endian
    private byte[] readPacket(int[] command) throws IOException {
        int size = in.readUnsignedShort();
        command[0] = in.readUnsignedByte();
        byte[] payload = new byte[size - 3];
        in.readFully(payload);
        if (command[0] == TEXT_MESSAGE) printTextMessage(payload);
        return payload;
    }

    private byte[] sendAndReceive(int command, byte[] payload) throws IOException {
        out.writeShort(payload.length + 3);
        out.writeByte(command);
        out.write(payload);
        out.flush();

        int[] received = new int[1];
        while (true) {
            byte[] reply = readPacket(received);
            if (received[0] == command) return reply;
        }
    }

    private void printTextMessage(byte[] payload) {
        int msgLength = payload[0] & 0xFF;
        String message = new String(payload, 1, msgLength, StandardCharsets.UTF_8);
        int srcLength = payload[1 + msgLength] & 0xFF;
        String source = new String(payload, 2 + msgLength, srcLength, StandardCharsets.UTF_8);
        int level = payload[2 + msgLength + srcLength] & 0xFF;
        if (level < INFO_MESSAGE) System.out.println(source + ": " + message);
    }
}
