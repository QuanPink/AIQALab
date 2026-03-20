package com.framework.data.constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class ProtocolTestData {
    private static final Properties props = new Properties();
    static {
        try (InputStream is = ProtocolTestData.class.getClassLoader()
                .getResourceAsStream("testdata/protocols.properties")) {
            if (is != null) props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load protocol test data", e);
        }
    }

    private ProtocolTestData() {}

    public static List<String> getSubsetProtocols() {
        return Arrays.asList(props.getProperty("subset.protocols", "").split(","));
    }
    public static List<String> getBase58ProtocolFilter() {
        return Arrays.asList(props.getProperty("base58.protocol.filter", "").split(","));
    }
    public static List<String> getBech32ProtocolFilter() {
        return Arrays.asList(props.getProperty("bech32.protocol.filter", "").split(","));
    }
    public static String getBech32Prefix() {
        return props.getProperty("bech32.account.prefix", "dydx1");
    }
    public static List<String> getAllProtocols() {
        return Arrays.asList(props.getProperty("all.protocols", "").split(","));
    }
}
